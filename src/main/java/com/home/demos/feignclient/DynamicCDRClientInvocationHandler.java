package com.home.demos.feignclient;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.LinkedList;
import java.util.List;

public class DynamicCDRClientInvocationHandler implements InvocationHandler {

    private static final String FIND_ONE_METHOD = "findOne";
    private static final String FIND_ALL_METHOD = "findAll";
    private static final String SAVE_METHOD = "save";
    private static final String UPDATE_METHOD = "update";
    private static final String BY = "By";
    private static final String AND = "And";

    private IGenericClient client;

    public DynamicCDRClientInvocationHandler(IGenericClient client) {
        this.client = client;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {

        Class<?> returnType = null;

        if (List.class.isAssignableFrom(method.getReturnType())) {
            returnType = (Class<?>) ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        } else {
            returnType = method.getReturnType();
        }

        try {
            return executeMethodByName(args, returnType, method.getName(), method);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Object executeMethodByName(Object[] args, Class<?> returnType, String methodName, Method method) throws IllegalAccessException {

        if (methodName.startsWith(FIND_ONE_METHOD)) {
            return findOne(args, returnType, methodName);
        } else if (methodName.startsWith(FIND_ALL_METHOD)) {
            return findAll(args, returnType, methodName);
        } else if (SAVE_METHOD.equals(methodName)) {
            return save(args);
        } else if (UPDATE_METHOD.equals(methodName)) {
            return update(args);
        }

        throw new IllegalArgumentException(String.format("method %s is not supported", methodName));
    }

    private Object save(Object[] args) {

        if (args.length != 1) {
            throw new IllegalArgumentException("save method should have only one parameter");
        }

        IBaseResource objectToSave = (IBaseResource) args[0];

        MethodOutcome response = client.create()
                .resource(objectToSave)
                .execute();

        return response.getResource();
    }

    private Object update(Object[] args) {

        if (args.length != 1) {
            throw new IllegalArgumentException("save method should have only one parameter");
        }

        IBaseResource objectToUpdate = (IBaseResource) args[0];
        String objectID = takeIdFromResponse(objectToUpdate);

        MethodOutcome response = client.update()
                .resource(objectToUpdate)
                .withId(objectID)
                .execute();

        return objectToUpdate;
    }

    private String takeIdFromResponse(IBaseResource response) {
        return response.getIdElement() == null ? null : response.getIdElement().getIdPart();
    }

    private Object findOne(Object[] args, Class<?> returnType, String methodName) throws IllegalAccessException {
        return ((List) findAll(args, returnType, methodName)).stream()
                .findAny()
                .orElse(null);
    }

    private Object findAll(Object[] args, Class returnType, String methodName) throws IllegalAccessException {
        return BundleUtil
                .toListOfResourcesOfType(
                        client.getFhirContext(),
                        fetchResourceByInputParameters(returnType, args, methodName),
                        returnType
                );
    }

    private Bundle fetchResourceByInputParameters(Class returnType, Object[] args, String methodName) throws IllegalAccessException {
        IQuery iQuery = client.search().forResource(returnType);

        IQuery filteredQuery = addFilters(args, returnType, methodName, iQuery);

        return (Bundle) filteredQuery
                .include(IBaseResource.INCLUDE_ALL)
                .returnBundle(Bundle.class)
                .execute()
                ;
    }

    private IQuery addFilters(Object[] args, Class returnType, String methodName, IQuery iQuery) throws IllegalAccessException {

        if (!methodName.contains(BY)) {
            return iQuery;
        }

        String parametersNamesString = methodName.split(BY)[1];
        String[] parametersNames = parametersNamesString.split(AND);

        List<Filter> filters = takePossibleFilters(returnType);
        int parameterIndex = 0;
        boolean firstFilter = true;

        for (String parametersName : parametersNames) {
            Filter currentFilter = filters.stream()
                    .filter(filter -> filter.name.equals(parametersName.toLowerCase()))
                    .findAny()
                    .orElse(null);

            if (currentFilter != null) {

                if (firstFilter) {
                    iQuery
                            .where(currentFilter.filter(args[parameterIndex]));
                } else {
                    iQuery
                            .and(currentFilter.filter(args[parameterIndex]));
                }

                firstFilter = false;
                parameterIndex++;
            }
        }

        return iQuery;
    }

    private List<Filter> takePossibleFilters(Class returnType) throws IllegalAccessException {
        List<Filter> filters = new LinkedList<>();

        for (Field returnTypeFiled : returnType.getFields()) {
            if (IParam.class.isAssignableFrom(returnTypeFiled.getType())) {

                IParam iParam = (IParam) returnTypeFiled.get(null);
                String paramName = iParam.getParamName();

                filters.add(new Filter(paramName, iParam));
            }
        }

        return filters;
    }

    private static class Filter {
        private String name;
        private IParam param;

        private Filter(String name, IParam param) {
            this.name = name
                    .replaceAll("_", "")
                    .replaceAll("-", "");

            this.param = param;
        }

        private ICriterion filter(Object value) {
            if (param instanceof TokenClientParam) {
                return ((TokenClientParam) param).exactly().identifier((String) value);
            }

            if (param instanceof ReferenceClientParam) {
                return ((ReferenceClientParam) param).hasId((String) value);
            }

            return null;
        }
    }
}

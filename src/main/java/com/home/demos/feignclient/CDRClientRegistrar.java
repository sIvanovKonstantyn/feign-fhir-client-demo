package com.home.demos.feignclient;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class CDRClientRegistrar implements BeanFactoryPostProcessor {

    private final String BASE_PACKAGE = "com.home";
    private final String CLASS_TYPE = ".class";

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

        var classLoader = getClass().getClassLoader();

        Arrays.stream(classLoader.getDefinedPackages())
                .map(Package::getName)
                .filter(p -> p.contains(BASE_PACKAGE))
                .flatMap(p -> findAllClassesInPackage(classLoader, p).stream())
                .map(this::takeClassByFullName)
                .filter(Objects::nonNull)
                .filter(Class::isInterface)
                .filter(c -> c.getAnnotation(CDRClient.class) != null)
                .peek(c -> log.debug("CDRCLient class found: {}", c))
                .forEach(
                        c -> beanFactory.registerSingleton(
                                c.getName(),
                                Proxy.newProxyInstance(
                                        classLoader,
                                        new Class[]{c},
                                        new DynamicCDRClientInvocationHandler(
                                                beanFactory.getBean(IGenericClient.class)
                                        ))
                        )
                );
    }

    private Class<?> takeClassByFullName(String className) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
        }

        return clazz;
    }

    private List<String> findAllClassesInPackage(ClassLoader classLoader, String currentPackage) {
        try (var bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        classLoader.getResourceAsStream(currentPackage.replaceAll("\\.", "/"))
                )
        ))) {
            return bufferedReader
                    .lines()
                    .filter(s -> s.contains(CLASS_TYPE))
                    .map(s -> s.replace(CLASS_TYPE, ""))
                    .map(s -> String.format("%s.%s", currentPackage, s))
                    .collect(Collectors.toList())
                    ;

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        return Collections.emptyList();
    }
}

package com.americanwell.demos.feignclient;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.extern.log4j.Log4j2;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Log4j2
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CDRClientRegistrar implements ImportBeanDefinitionRegistrar {

    private static final String CLASS_TYPE = ".class";

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        var classLoader = getClass().getClassLoader();
        var beanFactory = registry instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) registry : null;

        try {

            List<String> packages = List.of(ClassUtils.getPackageName(metadata.getClassName()));
            log.debug("packages found: {}", packages);

            List<? extends Class<?>> interfaces = packages.stream()
                    .flatMap(p -> findAllClassesInPackage(p).stream())
                    .map(this::takeClassByFullName)
                    .filter(Objects::nonNull)
                    .filter(Class::isInterface)
                    .collect(Collectors.toList());
            log.debug("interfaces found: {}", interfaces);

            for (Class clazz : interfaces) {
                if (clazz.getAnnotation(CDRClient.class) == null) {
                    continue;
                }

                log.debug("CDRClient class found: {}", clazz);

                Class<?> targetClass = ClassUtils.resolveClassName(clazz.getName(), null);
                BeanDefinitionBuilder beanDefinition = getBeanDefinitionBuilder(beanFactory, targetClass);
                String[] qualifiers = new String[]{clazz.getName()};
                BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition.getBeanDefinition(), clazz.getName(), qualifiers);
                BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BeanDefinitionBuilder getBeanDefinitionBuilder(ConfigurableBeanFactory beanFactory, Class clazz) {
        BeanDefinitionBuilder beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(
                clazz,
                (Supplier) () ->
                        new CDRClientFactoryBean(
                                Proxy.newProxyInstance(
                                        clazz.getClassLoader(),
                                        new Class[]{clazz},
                                        new DynamicCDRClientInvocationHandler(
                                                beanFactory.getBean(IGenericClient.class)
                                        )),
                                clazz
                        )
        );
        return beanDefinition;
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

    private List<String> findAllClassesInPackage(String currentPackage) {
        return new Reflections(currentPackage, new SubTypesScanner(false)).getSubTypesOf(Object.class).stream()
                .map(Class::getName)
                .collect(Collectors.toList());
    }
}

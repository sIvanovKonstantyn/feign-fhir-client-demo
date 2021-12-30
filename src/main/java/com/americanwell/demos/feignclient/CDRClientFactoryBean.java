package com.americanwell.demos.feignclient;

import org.springframework.beans.factory.FactoryBean;

public class CDRClientFactoryBean implements FactoryBean {

    private final Object createdObject;
    private final Class targetClass;

    public CDRClientFactoryBean(Object createdObject, Class targetClass) {
        this.createdObject = createdObject;
        this.targetClass = targetClass;
    }

    @Override
    public Object getObject() throws Exception {
        return createdObject;
    }

    @Override
    public Class<?> getObjectType() {
        return targetClass;
    }
}

package edu.gdut.MF.testEntity;

import edu.gdut.MF.annotation.Bean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class ServiceEnhancer implements InvocationHandler {
    Service service;

    public ServiceEnhancer(Service service) {
        this.service = service;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("enhanced!");
        return method.invoke(service, args);
    }
}

package edu.gdut.MF.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class WeaveEnv {
    private Method method;
    private Object[] parameters;
    private Object originBean;

    public WeaveEnv(Method method, Object[] parameters, Object originBean) {
        this.method = method;
        this.parameters = parameters;
        this.originBean = originBean;
    }

    public Object invokeMethod(){
        try {
            return method.invoke(originBean,parameters);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Parameter[] parameters) {
        this.parameters = parameters;
    }

    public Object getOriginBean() {
        return originBean;
    }

    public void setOriginBean(Object originBean) {
        this.originBean = originBean;
    }
}

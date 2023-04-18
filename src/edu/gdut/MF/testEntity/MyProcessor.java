package edu.gdut.MF.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;
import edu.gdut.MF.annotation.Order;
import edu.gdut.MF.core.BeanProcessor;

import java.lang.reflect.Proxy;

//@Bean
//@Order(100)
public class MyProcessor implements BeanProcessor {
    @Override
    public Object operateOnBeanAfterInitialization(Object bean, String beanName) {
        if (bean instanceof Service) {
                return Proxy.newProxyInstance(bean.getClass().getClassLoader(),
                        new Class[]{Service.class}, new ServiceEnhancer((Service) bean));
        }
        return null;
    }
}

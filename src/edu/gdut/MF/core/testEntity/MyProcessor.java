package edu.gdut.MF.core.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;
import edu.gdut.MF.annotation.Order;
import edu.gdut.MF.core.BeanProcessor;

@Bean
@Order(100)
public class MyProcessor implements BeanProcessor {
    @Inject
    Wrapper wrapper;
    @Override
    public Object operateOnBeanAfterInitialization(Object bean, String beanName) {
        System.out.println(wrapper.string);
        return null;
    }

    public void setWrapper(Wrapper wrapper) {
        this.wrapper = wrapper;
    }
}

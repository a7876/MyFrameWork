package edu.gdut.MF.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Order;
import edu.gdut.MF.core.BeanProcessor;

@Bean
@Order(100)
public class StringProcessor implements BeanProcessor {

    @Override
    public Object operateOnBeanAfterInitialization(Object bean, String beanName) {
        if (bean instanceof String) {
            return "haha has been modified";
        }
        return null;
    }
}

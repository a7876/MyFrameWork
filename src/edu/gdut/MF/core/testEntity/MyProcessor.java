package edu.gdut.MF.core.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Order;
import edu.gdut.MF.core.AfterInitialProcessor;
import edu.gdut.MF.core.BeanProcessor;

import java.util.ArrayList;
import java.util.List;

@Bean
@Order(100)
public class MyProcessor implements BeanProcessor {
    @Override
    public Object operateOnBeanBeforeInitialization(Object bean, String beanName) {
        return null;
    }
}

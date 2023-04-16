package edu.gdut.MF.core.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;
import edu.gdut.MF.core.BeanProcessor;

@Bean
public class StringProcessor implements BeanProcessor {

    @Override
    public Object operateOnBeanAfterInitialization(Object bean, String beanName) {
        if (bean instanceof String) {
            return "haha has been modified";
        }
        return null;
    }
}

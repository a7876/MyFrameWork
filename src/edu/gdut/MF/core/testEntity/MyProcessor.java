package edu.gdut.MF.core.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Order;
import edu.gdut.MF.core.BeanProcessor;

@Bean
@Order(100)
public class MyProcessor implements BeanProcessor {
    @Override
    public Object operateOnBeanAfterInitialization(Object bean, String beanName) {
        if (bean instanceof Wrapper) {
            Wrapper wrapper = new Wrapper();
            wrapper.string = "haha! has been changed!";
            return wrapper;
        }
        return null;
    }
}

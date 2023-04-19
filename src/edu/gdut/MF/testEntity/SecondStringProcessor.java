package edu.gdut.MF.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Order;
import edu.gdut.MF.core.BeanProcessor;

@Bean
@Order(99) // 测试优先级的第二处理器
public class SecondStringProcessor implements BeanProcessor {
    @Override
    public Object operateOnBeanAfterInitialization(Object bean, String beanName) {
        if (beanName.equals("string")){
            return "string changed!";
        }
        return null;
    }
}

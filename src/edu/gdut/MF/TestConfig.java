package edu.gdut.MF;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.MFConfig;
import edu.gdut.MF.annotation.Order;
import edu.gdut.MF.core.BeanProcessor;
import edu.gdut.MF.core.testEntity.MyProcessor;
import edu.gdut.MF.core.testEntity.Wrapper;

@MFConfig
public class TestConfig {
//    @Bean
//    @Order(100)
    public BeanProcessor beanProcessor(Wrapper wrapper){
        MyProcessor myProcessor = new MyProcessor();
        myProcessor.setWrapper(wrapper);
        return myProcessor;
    }
}

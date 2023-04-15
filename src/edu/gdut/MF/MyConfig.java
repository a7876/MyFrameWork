package edu.gdut.MF;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Import;
import edu.gdut.MF.annotation.MFConfig;
import edu.gdut.MF.annotation.Order;
import edu.gdut.MF.core.BeanProcessor;
import edu.gdut.MF.core.testEntity.MyProcessor;
import edu.gdut.MF.core.testEntity.TestBean;
import edu.gdut.MF.core.testEntity.Wrapper;

@MFConfig
@Import("edu.gdut.MF.TestConfig")
public class MyConfig {
    @Bean
    public String string(){
        return "config string";
    }
}

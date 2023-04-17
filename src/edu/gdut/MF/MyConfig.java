package edu.gdut.MF;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Import;
import edu.gdut.MF.annotation.MFConfig;

@MFConfig
@Import("edu.gdut.MF.TestConfig")
public class MyConfig {
    @Bean
    public String string(){
        return "config string";
    }
}

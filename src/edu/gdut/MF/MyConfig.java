package edu.gdut.MF;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Import;
import edu.gdut.MF.annotation.MFConfig;

@MFConfig
public class MyConfig {
    // 副配置
    @Bean
    public String string(){
        return "config string";
    }
}

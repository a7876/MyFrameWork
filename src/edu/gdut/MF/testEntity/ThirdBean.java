package edu.gdut.MF.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;

@Bean
public class ThirdBean {
    // 测试循环注入
    @Inject
    TestBean testBean;
}

package edu.gdut.MF.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;

@Bean
public class TestBean {
    // 测试循环注入
    @Inject
    TestBean testBean;
    @Inject
    ThirdBean thirdBean;
    public void doing(){
        System.out.println("singleton loop inject result : " + (testBean == this));
        System.out.println(thirdBean);
    }
}

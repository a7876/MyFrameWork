package edu.gdut.MF.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;
@Bean("stringBean")
public class SecondBean {
    // 构造器测试bean
    private final String string;
    @Inject
    public SecondBean(String string) {
        this.string = string;
    }

    public void doing() {
        System.out.println("bean named string value is : " + string);
    }

}

package edu.gdut.MF.core.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;

@Bean
public class TestBean {
    @Inject
    ThirdOne thirdOne;
    public void doing(){
        System.out.println("hahaha!");
    }
}

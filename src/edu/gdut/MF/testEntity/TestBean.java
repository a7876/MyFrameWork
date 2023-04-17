package edu.gdut.MF.testEntity;

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

package edu.gdut.MF.core.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;

@Bean
public class Wrapper {
    public TestBean testBean;
//    @Inject
    public Wrapper wrapper;

    @Inject
    public String string;

    public void doing() {
        System.out.println();
    }

}

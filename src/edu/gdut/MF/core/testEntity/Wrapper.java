package edu.gdut.MF.core.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;

@Bean(priority = true)
public class Wrapper {
    public TestBean testBean;
    @Inject
    public Wrapper wrapper;

    public String string;

    public void doing() {
        System.out.println(string);
    }

}

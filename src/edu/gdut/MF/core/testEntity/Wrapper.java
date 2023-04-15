package edu.gdut.MF.core.testEntity;

import edu.gdut.MF.Enum.InjectionType;
import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;

@Bean(priority = true)
public class Wrapper extends Father {
    @Inject
    public TestBean testBean;
    public Wrapper wrapper;

    public void doing() {
        System.out.println(testBean);
        System.out.println(wrapper);
        System.out.println(wrapper);
    }

    @Inject
    public void setWrapper(Wrapper hr) {
        this.wrapper = hr;
    }
}

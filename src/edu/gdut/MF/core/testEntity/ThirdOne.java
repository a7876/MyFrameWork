package edu.gdut.MF.core.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;

@Bean
public class ThirdOne {
    @Inject
    TestBean testBean;
}

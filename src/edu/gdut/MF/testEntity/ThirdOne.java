package edu.gdut.MF.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;

@Bean
public class ThirdOne {
    @Inject
    TestBean testBean;
}

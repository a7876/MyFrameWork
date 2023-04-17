package edu.gdut.MF.testEntity;

import edu.gdut.MF.annotation.Bean;

@Bean
public class StandardService implements Service{
    @Override
    public void doing() {
        System.out.println("standard service running!");
    }
}

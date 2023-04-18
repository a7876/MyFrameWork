package edu.gdut.MF.testEntity;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;

@Bean
public class StandardService implements Service{
    @Inject
    String string;
    @Override
    public void doing() {
        System.out.println("standard service running!");
    }

    @Override
    public void doing(String string) {
        System.out.println("i have param");
    }

    @Override
    public void normal() {
        System.out.println("normal " + string);
    }
}

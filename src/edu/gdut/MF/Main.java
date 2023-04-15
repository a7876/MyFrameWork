package edu.gdut.MF;

import edu.gdut.MF.annotation.Import;
import edu.gdut.MF.annotation.MFConfig;
import edu.gdut.MF.core.AdvancedBeanFactory;
import edu.gdut.MF.core.testEntity.Wrapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Stream;

@MFConfig
@Import("edu.gdut.MF.MyConfig")
public class Main {
    public static void main(String[] args) {
        AdvancedBeanFactory factory = new AdvancedBeanFactory(Main.class);
        Wrapper wrapper = factory.getBean(Wrapper.class);
        wrapper.doing();
    }
}


package edu.gdut.MF;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Import;
import edu.gdut.MF.annotation.MFConfig;
import edu.gdut.MF.core.AdvancedBeanFactory;
import edu.gdut.MF.core.BeanProcessor;
import edu.gdut.MF.core.testEntity.TestBean;
import edu.gdut.MF.core.testEntity.Wrapper;


@MFConfig
@Import("edu.gdut.MF.MyConfig")
public class Main {
    public static void main(String[] args) {
        AdvancedBeanFactory factory = new AdvancedBeanFactory(Main.class, BeanProcessor.ProcessorOption.ALLOWTOINJECT);
        Wrapper wrapper = (Wrapper) factory.getBean("wrapper");
        System.out.println(wrapper.string);
    }
}


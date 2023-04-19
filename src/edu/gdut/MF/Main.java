package edu.gdut.MF;

import edu.gdut.MF.annotation.Import;
import edu.gdut.MF.annotation.MFConfig;
import edu.gdut.MF.core.AdvancedBeanFactory;
import edu.gdut.MF.core.BeanProcessor;
import edu.gdut.MF.testEntity.SecondBean;
import edu.gdut.MF.testEntity.Service;
import edu.gdut.MF.testEntity.TestBean;


@MFConfig
@Import("edu.gdut.MF.MyConfig")
public class Main {
    // 测试主配置
    public static void main(String[] args) {
        AdvancedBeanFactory factory = new AdvancedBeanFactory(Main.class, BeanProcessor.ProcessorOption.ALLOWTOINJECT);
        Service service = (Service) factory.getBean("standardService");
        SecondBean secondBean = (SecondBean) factory.getBean("stringBean");
        TestBean testBean = factory.getBean(TestBean.class);
        if (secondBean != null)
            secondBean.doing();
        if (testBean != null)
            testBean.doing();
        if (service != null){
            service.doing();
            service.doing("parameter version");
            service.normal();
        }
    }
}


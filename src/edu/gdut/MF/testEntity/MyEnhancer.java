package edu.gdut.MF.testEntity;

import edu.gdut.MF.Enum.EnhanceType;
import edu.gdut.MF.Enum.InjectionType;
import edu.gdut.MF.annotation.EnhanceDefinition;
import edu.gdut.MF.annotation.Enhancer;
import edu.gdut.MF.annotation.Inject;
import edu.gdut.MF.core.WeaveEnv;

@Enhancer
public class MyEnhancer {
    // AOP定义类
    @EnhanceDefinition(type = EnhanceType.BEFORE_METHOD_INVOKE, targetName = "standardService",
            targetMethod = "doing", parameters = {})
    @Inject(InjectionType.INJECT_BY_TYPE) // 支持声明注入类型
    public void before(){
        System.out.println("enhance before");
    }

    @EnhanceDefinition(type = EnhanceType.AFTER_METHOD_INVOKE, targetName = "standardService",
            targetMethod = "doing", parameters = {String.class})
    public void after(){
        System.out.println("enhance after");
    }

    @EnhanceDefinition(type = EnhanceType.AROUND_METHOD_INVOKE, targetName = "standardService",
            targetMethod = "doing", parameters = {String.class})
    public void around(WeaveEnv weaveEnv){
        System.out.println("around before");
        weaveEnv.invokeMethod();
        System.out.println("around after");
    }

}

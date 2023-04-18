package edu.gdut.MF.testEntity;

import edu.gdut.MF.Enum.EnhanceType;
import edu.gdut.MF.Enum.InjectionType;
import edu.gdut.MF.annotation.EnhanceDefinition;
import edu.gdut.MF.annotation.Enhancer;
import edu.gdut.MF.annotation.Inject;
import edu.gdut.MF.core.WeaveEnv;

@Enhancer
public class MyEnhancer {
    @EnhanceDefinition(type = EnhanceType.BEFOREMETHODINVOKE, targetName = "standardService",
            targetMethod = "doing", parameters = {})
    @Inject(InjectionType.INJECTBYTYPE)
    public void before(String s){
        System.out.println("enhance before");
    }

    @EnhanceDefinition(type = EnhanceType.AFTERMETHODINVOKE, targetName = "standardService",
            targetMethod = "doing", parameters = {String.class})
    public void after(){
        System.out.println("enhance after");
    }

}

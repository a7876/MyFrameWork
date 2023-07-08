package edu.gdut.MF.core;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Enhancer;
import edu.gdut.MF.annotation.MFConfig;
import edu.gdut.MF.core.filter.BeanClassAnalyser;
import edu.gdut.MF.core.filter.AnnotationFilter;
import edu.gdut.MF.core.filter.AnnotationMeta;
import edu.gdut.MF.core.filter.TypeFilter;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.Opcodes;

import java.io.IOException;

public class BeanDefinitionFilter implements Opcodes {
    private static final AnnotationFilter annotationFilter = new AnnotationFilter();
    private static final TypeFilter typeFilter = new TypeFilter();

    static {
//      关注的注解
        annotationFilter.register(AnnotationFilter.AnnotationUtils.getObjectFullName(Bean.class.getName()));
        annotationFilter.register(AnnotationFilter.AnnotationUtils.getObjectFullName(Enhancer.class.getName()));
        annotationFilter.register(AnnotationFilter.AnnotationUtils.getObjectFullName(MFConfig.class.getName()));
//      不关注的类型
//      annotation / interface / abstract class ignored
        typeFilter.register(ACC_ANNOTATION);
        typeFilter.register(ACC_ABSTRACT);
    }

    public static boolean isNecessaryToLoad(String name) throws IOException {
        ClassReader classReader = new ClassReader(name);
        AnnotationMeta meta = new AnnotationMeta();
        BeanClassAnalyser analyser = new BeanClassAnalyser(ASM5, null, annotationFilter, typeFilter, meta);
        classReader.accept(analyser, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
        return meta.isBean();
    }

}

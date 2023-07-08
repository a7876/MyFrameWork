package edu.gdut.MF.core.filter;

import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.ClassVisitor;

public class BeanClassAnalyser extends ClassVisitor{
    private final AnnotationFilter annotationFilter;
    private final TypeFilter typeFilter;
    private final AnnotationMeta meta;
    public BeanClassAnalyser(int api, ClassVisitor cv, AnnotationFilter annotationFilter,
                             TypeFilter typeFilter,
                             AnnotationMeta annotationMeta) {
        super(api, cv);
        this.annotationFilter = annotationFilter;
        this.typeFilter = typeFilter;
        meta = annotationMeta;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (annotationFilter.judge(desc))
         meta.setTargetAnnotationMarked(true);
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        meta.setAcceptableType(typeFilter.judge(access));
        super.visit(version, access, name, signature, superName, interfaces);
    }
}

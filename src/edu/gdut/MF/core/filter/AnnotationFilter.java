package edu.gdut.MF.core.filter;

public class AnnotationFilter extends Filter {
    public static class AnnotationUtils {
        public static final String SEPARATOR = "/";
        public static final String DOT = ".";
        public static final String OBJECT_PREFIX = "L";
        public static final String OBJECT_SUFFIX = ";";

        public static String getObjectFullName(String objectFullName) {
            objectFullName = objectFullName.replace(DOT, SEPARATOR);
            return OBJECT_PREFIX + objectFullName + OBJECT_SUFFIX;
        }
    }
}

package edu.gdut.MF.core.filter;


public class TypeFilter extends Filter{
    @Override
    public boolean judge(Object t) {
        Integer access  = (Integer) t;
        TypeHolder typeHolder = new TypeHolder();
        set.forEach(item -> {
            if ((access & (Integer) item) != 0) {
                typeHolder.res = false;
            }
        });
        return typeHolder.res;
    }
    private static class TypeHolder{
        boolean res = true;
    }
}

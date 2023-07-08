package edu.gdut.MF.core.filter;

import java.util.HashSet;
import java.util.Set;

public abstract class Filter {

    Filter() {
        set = new HashSet<>();
    }

    protected Set<Object> set;

    public boolean judge(Object t) {
        return set.contains(t);
    }

    public void register(Object item) {
        set.add(item);
    }

    public void remove(Object item) {
        set.remove(item);
    }

}

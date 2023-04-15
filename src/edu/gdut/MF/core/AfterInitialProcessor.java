package edu.gdut.MF.core;
@FunctionalInterface
public interface AfterInitialProcessor {
    public void operateOnObject(Object o, Class<?> type); // allow to do something after bean has been init
}

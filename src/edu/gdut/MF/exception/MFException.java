package edu.gdut.MF.exception;

public class MFException extends RuntimeException{
    // 自定义运行时异常
    public MFException(String message){
        super(message);
    }

    public MFException(String message, Throwable cause){
        super(message,cause);
    }
}

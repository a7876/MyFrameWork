package edu.gdut.MF.exception;

public class MFException extends RuntimeException{
    public MFException(String message){
        super(message);
    }

    public MFException(String message, Throwable cause){
        super(message,cause);
    }
}

package edu.gdut.MF.core.filter;

public class AnnotationMeta extends MetaInfo{
    private boolean targetAnnotationMarked;
    private boolean acceptableType;

    public boolean isTargetAnnotationMarked() {
        return targetAnnotationMarked;
    }

    public void setTargetAnnotationMarked(boolean targetAnnotationMarked) {
        this.targetAnnotationMarked = targetAnnotationMarked;
    }

    public boolean isAcceptableType() {
        return acceptableType;
    }

    public void setAcceptableType(boolean acceptableType) {
        this.acceptableType = acceptableType;
    }

    public boolean isBean(){
        return targetAnnotationMarked && acceptableType;
    }
}

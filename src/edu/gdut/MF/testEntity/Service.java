package edu.gdut.MF.testEntity;

import edu.gdut.MF.annotation.Bean;

public interface Service {
    // AOP测试接口类
    void doing();
    void doing(String string);
    void normal();
}

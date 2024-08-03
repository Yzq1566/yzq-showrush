package com.yzq.showrush.utils;

public interface ILock {

    boolean tryLock(int timeoutSec);

    void unLock();
}

package com.github.kr328.magic.action;

public interface VoidAction<T extends Throwable> {
    void run() throws T;
}

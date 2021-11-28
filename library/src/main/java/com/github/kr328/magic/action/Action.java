package com.github.kr328.magic.action;

public interface Action<R, T extends Throwable> {
    R run() throws T;
}


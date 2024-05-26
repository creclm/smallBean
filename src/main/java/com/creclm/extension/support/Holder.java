package com.creclm.extension.support;

/**
 *      包装类
 */
public class Holder<T> {

    private volatile T value; // volatile保证线程同步

    public void set(T value){
        this.value = value;
    }

    public T get(){
        return this.value;
    }
}

package com.nikolahitek;

import java.io.Serializable;

public class KeyValuePair implements Serializable {
    public Object key;
    public Object value;

    public KeyValuePair(Object key, Object value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return key + " -> " + value;
    }
}

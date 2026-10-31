package com.appleetcode.backend.src.entity;

public enum Phase {
    INTRODUCAO,
    CONSOLIDACAO,
    LIVRE;

    public String toStateValue() {
        return name().toLowerCase();
    }

    public static Phase fromStateValue(String value) {
        return Phase.valueOf(value.toUpperCase());
    }
}

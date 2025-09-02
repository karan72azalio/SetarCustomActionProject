package com.nokia.nsw.uiv.enums;

public enum ServiceEnum {

    SERVICE_KIND_CBM_SERVICE("CBMDevice");

    private final String value;

    ServiceEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
package com.hackthon.hackathon.enums;

public enum Pa {
    PA_PLUS("PA+"),
    PA_PLUS_PLUS("PA++"),
    PA_PLUS_PLUS_PLUS("PA+++"),
    PA_PLUS_PLUS_PLUS_PLUS("PA++++");
// enum 타입에는 + 문자가 인식이 안돼서 식별자만들고 괄호안에 실제 표시값 넣어놨어용
    private final String value;

    Pa(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
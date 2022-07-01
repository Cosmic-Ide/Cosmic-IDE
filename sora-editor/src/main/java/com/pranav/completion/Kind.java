package com.pranav.completion;

public enum Kind {
    Method("M", 0xffe92e2e),
    Interface("I", 0xffcc7832),
    Field("F", 0xff1c9344),
    Class("C", 0xff1c9344),
    Keyword("K", 0xffcc7832),
    Package("P", 0xffcc7832),
    Lambda("Ł", 0xff36b9da),
    Snippet("S", 0xffcc7832),
    LocalVariable("L", 0xffcc7832);

    private final int color;
    private final String prefix;

    Kind(String prefix, int color) {
        this.prefix = prefix;
        this.color = color;
    }

    public String getValue() {
        return prefix;
    }

    public int getColor() {
        return color;
    }
}
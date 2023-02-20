package com.tyron.javacompletion.parser.classfile;

/** Class accss flags of a class_info structure in .class files. */
public enum ClassAccessFlag {
    PUBLIC(0x0001),
    PRIVATE(0x0002),
    PROTECTED(0x0004),
    STATIC(0x0008),
    FINAL(0x0010),
    SUPER(0x0020),
    INTERFACE(0x0200),
    ABSTRACT(0x0400),
    SYNTHETIC(0x1000),
    ANNOTATION(0x2000),
    ENUM(0x4000),
    ;

    private final int value;

    ClassAccessFlag(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
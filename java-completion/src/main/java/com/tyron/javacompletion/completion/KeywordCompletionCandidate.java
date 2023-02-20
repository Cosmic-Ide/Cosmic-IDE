package com.tyron.javacompletion.completion;

import java.util.Optional;

enum KeywordCompletionCandidate implements CompletionCandidate {
    ABSTRACT,
    ASSERT,
    BOOLEAN,
    BREAK,
    BYTE,
    CASE,
    CATCH,
    CHAR,
    CLASS,
    CONST,
    CONTINUE,
    DEFAULT,
    DO,
    DOUBLE,
    ELSE,
    ENUM,
    EXTENDS,
    FINAL,
    FINALLY,
    FLOAT,
    FOR,
    GOTO,
    IF,
    IMPLEMENTS,
    IMPORT,
    INSTANCEOF,
    INT,
    INTERFACE,
    LONG,
    NATIVE,
    NEW,
    PACKAGE,
    PRIVATE,
    PROTECTED,
    PUBLIC,
    RETURN,
    SHORT,
    STATIC,
    STRICTFP,
    SUPER,
    SWITCH,
    SYNCHRONIZED,
    THIS,
    THROW,
    THROWS,
    TRANSIENT,
    TRY,
    VOID,
    VOLATILE,
    WHILE,
    ;

    @Override
    public String getName() {
        return name().toLowerCase();
    }

    @Override
    public Kind getKind() {
        return Kind.KEYWORD;
    }

    @Override
    public SortCategory getSortCategory() {
        return SortCategory.KEYWORD;
    }

    @Override
    public Optional<String> getDetail() {
        return Optional.ofNullable(null);
    }
}

package org.cosmicide.editor.analyzers.services;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.util.Context;

public class NBCheck extends Check {

    protected NBCheck(Context context) {
        super(context);
    }

    public static void preRegister(Context context) {
        context.put(checkKey, (Context.Factory<Check>) NBCheck::new);
    }

    @Override
    public void clearLocalClassNameIndexes(Symbol.ClassSymbol c) {
        if (c.owner != null && c.owner.enclClass() != null) {
            super.clearLocalClassNameIndexes(c);
        }
    }
}

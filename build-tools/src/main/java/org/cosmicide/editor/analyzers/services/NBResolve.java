/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers.services;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.util.Context;

/**
 * @author lahvac
 */
public class NBResolve extends Resolve {
    private boolean accessibleOverride;

    protected NBResolve(Context ctx) {
        super(ctx);
    }

    public static NBResolve instance(Context context) {
        Resolve instance = context.get(resolveKey);
        if (instance == null) {
            instance = new NBResolve(context);
        }
        return (NBResolve) instance;
    }

    public static void preRegister(Context context) {
        context.put(resolveKey, (Context.Factory<Resolve>) NBResolve::new);
    }

    public static boolean isStatic(Env<AttrContext> env) {
        return Resolve.isStatic(env);
    }

    public void disableAccessibilityChecks() {
        accessibleOverride = true;
    }

    public void restoreAccessbilityChecks() {
        accessibleOverride = false;
    }

    @Override
    public boolean isAccessible(Env<AttrContext> env, Type site, Symbol sym, boolean checkInner) {
        if (accessibleOverride) {
            return true;
        }
        return super.isAccessible(env, site, sym, checkInner);
    }

    @Override
    public boolean isAccessible(Env<AttrContext> env, TypeSymbol c, boolean checkInner) {
        if (accessibleOverride) {
            return true;
        }
        return super.isAccessible(env, c, checkInner);
    }
}
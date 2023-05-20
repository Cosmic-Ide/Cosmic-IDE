/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers.services;

import com.sun.tools.javac.code.Symbol;
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

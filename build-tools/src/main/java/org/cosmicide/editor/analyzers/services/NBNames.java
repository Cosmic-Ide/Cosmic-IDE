/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers.services;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

/**
 * @author lahvac
 */
public class NBNames {

    public static final Context.Key<NBNames> nbNamesKey = new Context.Key<>();
    public final Name _org_netbeans_EnclosingMethod;
    public final Name _org_netbeans_TypeSignature;
    public final Name _org_netbeans_ParameterNames;
    public final Name _org_netbeans_SourceLevelAnnotations;
    public final Name _org_netbeans_SourceLevelParameterAnnotations;
    public final Name _org_netbeans_SourceLevelTypeAnnotations;

    protected NBNames(Context context) {
        Names n = Names.instance(context);

        _org_netbeans_EnclosingMethod = n.fromString("org.netbeans.EnclosingMethod");
        _org_netbeans_TypeSignature = n.fromString("org.netbeans.TypeSignature");
        _org_netbeans_ParameterNames = n.fromString("org.netbeans.ParameterNames");
        _org_netbeans_SourceLevelAnnotations = n.fromString("org.netbeans.SourceLevelAnnotations");
        _org_netbeans_SourceLevelParameterAnnotations = n.fromString("org.netbeans.SourceLevelParameterAnnotations");
        _org_netbeans_SourceLevelTypeAnnotations = n.fromString("org.netbeans.SourceLevelTypeAnnotations");
    }

    public static void preRegister(Context context) {
        context.put(nbNamesKey, (Context.Factory<NBNames>) NBNames::new);
    }

    public static NBNames instance(Context context) {
        NBNames instance = context.get(nbNamesKey);
        if (instance == null) {
            instance = new NBNames(context);
        }
        return instance;
    }

}
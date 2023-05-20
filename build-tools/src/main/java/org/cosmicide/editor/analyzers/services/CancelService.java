/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers.services;

import androidx.annotation.CallSuper;

import com.sun.tools.javac.util.Context;

/**
 * @author Tomas Zezula
 */
public class CancelService {

    /**
     * The context key for the parameter name resolver.
     */
    public static final Context.Key<CancelService> cancelServiceKey =
            new Context.Key<CancelService>();

    protected CancelService() {
    }

    public static CancelService instance(Context context) {
        CancelService instance = context.get(cancelServiceKey);
        if (instance == null) {
            instance = new CancelService();
            context.put(cancelServiceKey, instance);
        }
        return instance;
    }

    public boolean isCanceled() {
        return false;
    }

    @CallSuper
    public void abortIfCanceled() {
        if (isCanceled()) {
            onCancel();
            throw new CancelAbort();
        }
    }

    protected void onCancel() {

    }

}
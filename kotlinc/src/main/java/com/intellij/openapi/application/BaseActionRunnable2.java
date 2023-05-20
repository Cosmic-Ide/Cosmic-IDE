/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package com.intellij.openapi.application;

public abstract class BaseActionRunnable2<T> {

    private boolean mySilentExecution;

    public boolean isSilentExecution() {
        return mySilentExecution;
    }

    protected abstract void run(Result<T> result) throws Throwable;

    public abstract RunResult<T> execute();

    protected boolean canWriteNow() {
        return getApplication().isWriteAccessAllowed();
    }

    protected boolean canReadNow() {
        return getApplication().isReadAccessAllowed();
    }

    protected Application getApplication() {
        return ApplicationManager.getApplication();
    }

    /**
     * Same as execute() but do not log error if exception occurred.
     */
    public final RunResult<T> executeSilently() {
        mySilentExecution = true;
        return execute();
    }
}

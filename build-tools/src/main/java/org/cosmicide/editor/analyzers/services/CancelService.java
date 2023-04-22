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
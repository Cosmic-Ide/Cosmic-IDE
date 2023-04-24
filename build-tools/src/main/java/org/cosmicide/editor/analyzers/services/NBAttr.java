package org.cosmicide.editor.analyzers.services;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * @author lahvac
 */
public class NBAttr extends Attr {

    private final CancelService cancelService;
    private final TreeMaker tm;
    private boolean fullyAttribute;

    public NBAttr(Context context) {
        super(context);
        cancelService = CancelService.instance(context);
        tm = TreeMaker.instance(context);
    }

    public static void preRegister(Context context) {
        context.put(attrKey, (Context.Factory<Attr>) c -> new NBAttr(c));
    }

    @Override
    public void visitClassDef(JCClassDecl tree) {
        cancelService.abortIfCanceled();
        super.visitClassDef(tree);
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        cancelService.abortIfCanceled();
        super.visitMethodDef(tree);
    }

    @Override
    public void visitBlock(JCBlock tree) {
        cancelService.abortIfCanceled();
        super.visitBlock(tree);
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        //for erroneous "var", make sure the synthetic make.Error() has an invalid/synthetic position:
        tm.at(-1);
        super.visitVarDef(tree);
    }

    @Override
    public Type attribType(JCTree tree, Env<AttrContext> env) {
        cancelService.abortIfCanceled();
        return super.attribType(tree, env);
    }

    @Override
    public void visitCatch(JCCatch that) {
        super.visitBlock(tm.Block(0, List.of(that.param, that.body)));
    }

    protected void breakTreeFound(Env<AttrContext> env, Type result) {
        if (fullyAttribute) {
        } else {
            try {
                MethodHandles.lookup()
                        .findSpecial(Attr.class, "breakTreeFound", MethodType.methodType(void.class, Env.class, Type.class), NBAttr.class)
                        .invokeExact(this, env, result);
            } catch (Throwable ex) {
                sneakyThrows(ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Throwable> void sneakyThrows(Throwable t) throws T {
        throw (T) t;
    }

}
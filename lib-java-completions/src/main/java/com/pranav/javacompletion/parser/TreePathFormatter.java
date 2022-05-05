package com.pranav.javacompletion.parser;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;

/** A utility that converts {@link TreePath} to string for debugging. */
public class TreePathFormatter {
    private static final TreeFormattingVisitor VISITOR = new TreeFormattingVisitor();

    private TreePathFormatter() {}

    public static String formatTreePath(TreePath treePath) {
        if (treePath == null) {
            return "<empty>";
        }
        StringBuilder sb = new StringBuilder();
        formatTreePath(treePath, sb);
        sb.append("<end>");
        return sb.toString();
    }

    private static void formatTreePath(TreePath treePath, StringBuilder sb) {
        TreePath parent = treePath.getParentPath();
        if (parent != null) {
            formatTreePath(parent, sb);
        }

        Tree node = treePath.getLeaf();
        sb.append(node.accept(VISITOR, null));
        sb.append(' ');
    }

    public static class TreeFormattingVisitor extends SimpleTreeVisitor<CharSequence, Void> {
        @Override
        protected CharSequence defaultAction(Tree node, Void unused) {
            return "[" + node.getClass().getSimpleName() + "]";
        }

        @Override
        public CharSequence visitIdentifier(IdentifierTree node, Void unused) {
            return "[Identifier " + node.getName() + "]";
        }

        @Override
        public CharSequence visitMemberSelect(MemberSelectTree node, Void unused) {
            return "[MemberSelect id:" + node.getIdentifier() + "]";
        }

        @Override
        public CharSequence visitClass(ClassTree node, Void unused) {
            return formatNode(node, node.getSimpleName());
        }

        @Override
        public CharSequence visitMethod(MethodTree node, Void unused) {
            return formatNode(node, node.getName());
        }

        private static String formatNode(Tree node, CharSequence info) {
            return "[" + node.getClass().getSimpleName() + ": " + info + "]";
        }
    }
}
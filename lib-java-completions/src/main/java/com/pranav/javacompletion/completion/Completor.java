package com.pranav.javacompletion.completion;

import com.google.common.collect.ImmutableList;
import org.openjdk.source.tree.ExpressionTree;
import org.openjdk.source.tree.ImportTree;
import org.openjdk.source.tree.LiteralTree;
import org.openjdk.source.tree.MemberReferenceTree;
import org.openjdk.source.tree.MemberSelectTree;
import org.openjdk.source.tree.Tree;
import org.openjdk.source.util.TreePath;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import com.pranav.javacompletion.file.FileManager;
import com.pranav.javacompletion.logging.JLogger;
import com.pranav.javacompletion.project.ModuleManager;
import com.pranav.javacompletion.project.PositionContext;
import com.pranav.javacompletion.typesolver.ExpressionSolver;
import com.pranav.javacompletion.typesolver.MemberSolver;
import com.pranav.javacompletion.typesolver.OverloadSolver;
import com.pranav.javacompletion.typesolver.TypeSolver;

/**
 * Entry point of completion logic
 */
public class Completor {
    private static final JLogger logger = JLogger.createForEnclosingClass();

    private static final CompletionResult NO_CACHE =
            CompletionResult.builder()
                    .setFilePath(Paths.get(""))
                    .setLine(-1)
                    .setColumn(-1)
                    .setPrefix("")
                    .setCompletionCandidates(ImmutableList.of())
                    .setTextEditOptions(TextEditOptions.DEFAULT)
                    .build();

    private final FileManager fileManager;
    private final TypeSolver typeSolver;
    private final ExpressionSolver expressionSolver;

    private CompletionResult cachedCompletion = NO_CACHE;

    public Completor(FileManager fileManager) {
        this.fileManager = fileManager;
        this.typeSolver = new TypeSolver();
        OverloadSolver overloadSolver = new OverloadSolver(typeSolver);
        this.expressionSolver =
                new ExpressionSolver(
                        typeSolver, overloadSolver, new MemberSolver(typeSolver, overloadSolver));
    }

    /**
     * @param moduleManager the module of the project
     * @param filePath normalized path of the file to be completed
     * @param line 0-based line number of the completion point
     * @param column 0-based character offset from the beginning of the line to the completion point
     */
    public CompletionResult getCompletionResult(
            ModuleManager moduleManager, Path filePath, int line, int column) {
        // PositionContext gets the tree path whose leaf node includes the position
        // (position < node's endPosition). However, for completions, we want the leaf node either
        // includes the position, or just before the position (position == node's endPosition).
        // Decresing column by 1 will decrease position by 1, which makes
        // adjustedPosition == node's endPosition - 1 if the node is just before the actual position.
        int contextColumn = column > 0 ? column - 1 : 0;
        Optional<PositionContext> positionContext =
                PositionContext.createForPosition(moduleManager, filePath, line, contextColumn);

        if (!positionContext.isPresent()) {
            return CompletionResult.builder()
                    .setCompletionCandidates(ImmutableList.of())
                    .setLine(line)
                    .setColumn(column)
                    .setPrefix("")
                    .setFilePath(filePath)
                    .build();
        }

        ContentWithLineMap contentWithLineMap =
                ContentWithLineMap.create(positionContext.get().getFileScope(), fileManager, filePath);
        String prefix = contentWithLineMap.extractCompletionPrefix(line, column);
        // TODO: limit the number of the candidates.
        if (cachedCompletion.isIncrementalCompletion(filePath, line, column, prefix)) {
            return getCompletionCandidatesFromCache(line, column, prefix);
        } else {
            cachedCompletion =
                    computeCompletionResult(positionContext.get(), contentWithLineMap, line, column, prefix);
            return cachedCompletion;
        }
    }

    private CompletionResult computeCompletionResult(
            PositionContext positionContext,
            ContentWithLineMap contentWithLineMap,
            int line,
            int column,
            String prefix) {
        TreePath treePath = positionContext.getTreePath();
        CompletionAction action;
        TextEditOptions.Builder textEditOptions =
                TextEditOptions.builder().setAppendMethodArgumentSnippets(false);
        if (treePath.getLeaf() instanceof MemberSelectTree) {
            ExpressionTree parentExpression = ((MemberSelectTree) treePath.getLeaf()).getExpression();
            Optional<ImportTree> importNode = findNodeOfType(treePath, ImportTree.class);
            if (importNode.isPresent()) {
                if (importNode.get().isStatic()) {
                    action =
                            CompleteMemberAction.forImportStatic(parentExpression, typeSolver, expressionSolver);
                } else {
                    action = CompleteMemberAction.forImport(parentExpression, typeSolver, expressionSolver);
                }
            } else {
                action =
                        CompleteMemberAction.forMemberSelect(parentExpression, typeSolver, expressionSolver);
                textEditOptions.setAppendMethodArgumentSnippets(true);
            }
        } else if (treePath.getLeaf() instanceof MemberReferenceTree) {
            ExpressionTree parentExpression =
                    ((MemberReferenceTree) treePath.getLeaf()).getQualifierExpression();
            action =
                    CompleteMemberAction.forMethodReference(parentExpression, typeSolver, expressionSolver);
        } else if (treePath.getLeaf() instanceof LiteralTree) {
            // Do not complete on any literals, especially strings.
            action = NoCandidateAction.INSTANCE;
        } else {
            action = new CompleteSymbolAction(typeSolver, expressionSolver);
            textEditOptions.setAppendMethodArgumentSnippets(true);
        }

        // When the cursor is before an opening parenthesis, it's likely the user is
        // trying to change the name of a method invocation. In this case the
        // arguments are already there and we should not append method argument
        // snippet upon completion.
        if ("(".equals(contentWithLineMap.substring(line, column, 1))) {
            textEditOptions.setAppendMethodArgumentSnippets(false);
        }

        ImmutableList<CompletionCandidate> candidates =
                action.getCompletionCandidates(positionContext, prefix);
        return CompletionResult.builder()
                .setFilePath(contentWithLineMap.getFilePath())
                .setLine(line)
                .setColumn(column)
                .setPrefix(prefix)
                .setCompletionCandidates(candidates)
                .setTextEditOptions(textEditOptions.build())
                .build();
    }

    private CompletionResult getCompletionCandidatesFromCache(int line, int column, String prefix) {
        ImmutableList<CompletionCandidate> narrowedCandidates =
                new CompletionCandidateListBuilder(prefix)
                        .addCandidates(cachedCompletion.getCompletionCandidates())
                        .build();
        return cachedCompletion
                .toBuilder()
                .setCompletionCandidates(narrowedCandidates)
                .setLine(line)
                .setColumn(column)
                .setPrefix(prefix)
                .build();
    }

    private static <T extends Tree> Optional<T> findNodeOfType(TreePath treePath, Class<T> type) {
        while (treePath != null) {
            Tree leaf = treePath.getLeaf();
            if (type.isAssignableFrom(leaf.getClass())) {
                @SuppressWarnings("unchecked")
                T casted = (T) leaf;
                return Optional.of(casted);
            }
            treePath = treePath.getParentPath();
        }
        return Optional.empty();
    }

    /** A {@link CompletionAction} that always returns an empty list of candidates. */
    private static class NoCandidateAction implements CompletionAction {
        public static final NoCandidateAction INSTANCE = new NoCandidateAction();

        @Override
        public ImmutableList<CompletionCandidate> getCompletionCandidates(
                PositionContext positionContext, String prefix) {
            return ImmutableList.of();
        }
    }
}

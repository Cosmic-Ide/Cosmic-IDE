package com.tyron.kotlin_completion.util;

import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;

import kotlin.sequences.Sequence;
import kotlin.sequences.SequencesKt;

public class PsiUtils {

    public static <Find> Find findParent(PsiElement element, Class<Find> find) {
        Sequence<PsiElement> parentsWithSelf = PsiUtilsKt.getParentsWithSelf(element);
        Sequence<Find> sequence = SequencesKt.filterIsInstance(parentsWithSelf, find);
        return SequencesKt.firstOrNull(sequence);
    }
}
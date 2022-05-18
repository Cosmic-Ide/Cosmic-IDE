/*
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Initial code from https://github.com/Microsoft/vscode-textmate/
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 * <p>
 * Contributors:
 * - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package io.github.rosemoe.sora.textmate.core.grammar;

import io.github.rosemoe.sora.textmate.core.internal.types.IRawGrammar;

import java.util.Collection;

/**
 * TextMate grammar repository API.
 *
 * @see <a
 *     href="https://github.com/Microsoft/vscode-textmate/blob/master/src/grammar.ts">https://github.com/Microsoft/vscode-textmate/blob/master/src/grammar.ts</a>
 */
public interface IGrammarRepository {
    /** Lookup a raw grammar. */
    IRawGrammar lookup(String scopeName);

    /** Returns the injections for the given grammar */
    Collection<String> injections(String targetScope);
}

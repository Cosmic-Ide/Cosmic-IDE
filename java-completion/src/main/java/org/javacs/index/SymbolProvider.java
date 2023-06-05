/************************************************************************************
 * This file is part of Java Language Server (https://github.com/itsaky/java-language-server)
 *
 * Copyright (C) 2021 Akash Yadav
 *
 * Java Language Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java Language Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Language Server.  If not, see <https://www.gnu.org/licenses/>.
 *
 **************************************************************************************/

package org.javacs.index;

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.javacs.CompilerProvider;
import org.javacs.ParseTask;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SymbolProvider {

    private static final Logger LOG = Logger.getLogger("main");
    final CompilerProvider compiler;

    public SymbolProvider(CompilerProvider compiler) {
        this.compiler = compiler;
    }

    public List<SymbolInformation> findSymbols(CancelChecker checker, String query, int limit) {
        LOG.info(String.format("Searching for `%s`...", query));
        var result = new ArrayList<SymbolInformation>();
        var checked = 0;
        var parsed = 0;
        for (var file : compiler.search(query)) {
            checked++;
            // Parse the file and check class members for matches
            LOG.info(String.format("...%s contains text matches", file.getFileName()));
            var task = compiler.parse(file);
            checker.checkCanceled();
            var symbols = findSymbolsMatching(checker, task, query);
            checker.checkCanceled();
            parsed++;
            // If we confirm matches, add them to the results
            if (symbols.size() > 0) {
                LOG.info(String.format("...found %d occurrences", symbols.size()));
            }
            result.addAll(symbols);
            // If results are full, stop
            if (result.size() >= limit) break;
        }

        return result;
    }

    public List<Either<SymbolInformation, DocumentSymbol>> documentSymbols(CancelChecker checker, Path file) {
        var task = compiler.parse(file);
        checker.checkCanceled();
        var symbols = findSymbolsMatching(checker, task, "");
        var result = new ArrayList<Either<SymbolInformation, DocumentSymbol>>();
        for (var symbol : symbols) {
            result.add(Either.forLeft(symbol));
        }
        return result;
    }

    private List<SymbolInformation> findSymbolsMatching(CancelChecker checker, ParseTask task, String query) {
        var found = new ArrayList<SymbolInformation>();
        checker.checkCanceled();
        new FindSymbolsMatching(task, query).scan(task.root, found);
        return found;
    }
}

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

package org.javacs;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashHandler {

    private static LanguageClient client;

    public static void init(LanguageClient reciever) {
        client = reciever;
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logCrash(throwable);
        });
    }

    public static void logCrash(Throwable th) {
        if (client == null) return;
        var params = new MessageParams();
        params.setMessage(String.format("Java language server crashed. Stacktrace:\n%s", createStacktrace(th)));
        params.setType(MessageType.Error);
        client.logMessage(params);
    }

    public static String createStacktrace(Throwable th) {
        var r = new StringWriter();
        th.printStackTrace(new PrintWriter(r));
        return r.toString();
    }
}
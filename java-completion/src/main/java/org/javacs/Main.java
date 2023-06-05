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

import com.itsaky.lsp.services.IDELanguageClient;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.javacs.launch.JLSLauncher;
import org.javacs.services.JavaLanguageServer;

import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOG = Logger.getLogger("main");
    private static Future<Void> listening;

    public static void setRootFormat() {
        var root = Logger.getLogger("");

        for (var h : root.getHandlers()) {
            h.setFormatter(new LogFormat());
        }
    }

    public static void main(String[] args) {
        var stream = Arrays.stream(args);
        boolean quiet = stream.anyMatch("--quiet"::equals);

        if (quiet) {
            LOG.setLevel(Level.OFF);
        }

        try {
            setRootFormat();
            LOG.info("Launching server");

            var languageServer = new JavaLanguageServer();
            var provider = ConnectionFactory.getConnectionProvider();
            Launcher<IDELanguageClient> server = JLSLauncher.createServerLauncher(languageServer, provider.getInputStream(), provider.getOutputStream());
            listening = server.startListening();
            var client = server.getRemoteProxy();
            languageServer.connect(client);

            LOG.info("Client is now connected to server. Initializing CrashHandler...");
            CrashHandler.init(client);
            LOG.info("Logs will now be sent to client when JLS crashes");

            LOG.info("Server is now listening");

            listening.get();

            provider.exit();

            LOG.info("Server disconnected");

        } catch (Throwable t) {
            LOG.log(Level.SEVERE, t.getMessage(), t);
            System.exit(1);
        }
    }

    public static void exit() {
        if (listening != null) {
            listening.cancel(true);
        }
    }
}

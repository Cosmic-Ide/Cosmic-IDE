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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.util.concurrent.ExecutionException;

/**
 * Provides connection streams for LSP
 */
public class ConnectionFactory {

    public static final int PORT = 1234;

    public static ConnectionProvider getConnectionProvider() throws NumberFormatException, IOException, InterruptedException, ExecutionException {

        return new SocketConnectionProvider(PORT);
    }

    public interface ConnectionProvider {

        InputStream getInputStream() throws Exception;

        OutputStream getOutputStream() throws Exception;

        void exit() throws Exception;
    }

    /**
     * A socket connection. Connects to the provided host and port
     */
    public static class SocketConnectionProvider implements ConnectionProvider {

        private final Socket server;

        public SocketConnectionProvider(int port) throws IOException, InterruptedException, ExecutionException {
            this.server = new ServerSocket(port).accept();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return server != null ? server.getInputStream() : null;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return server != null ? server.getOutputStream() : null;
        }

        @Override
        public void exit() throws IOException {
            server.close();
        }
    }
}
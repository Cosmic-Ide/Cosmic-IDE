package com.pranav.common.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class BinaryExecutor {

    private final ProcessBuilder mProcess = new ProcessBuilder();
    private final StringWriter mWriter = new StringWriter();

    public String execute(ArrayList<String> arrayList) {
        mProcess.command(arrayList);
        try {
            var process = mProcess.start();
            var scanner = new Scanner(process.getErrorStream());
            while (scanner.hasNextLine()) {
                mWriter.append(scanner.nextLine());
                mWriter.append(System.lineSeparator());
            }

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace(new PrintWriter(mWriter));
        }
        return mWriter.toString();
    }

    public String execute(String command) {
        mProcess.command(command);
        try {
            var process = mProcess.start();
            var scanner = new Scanner(process.getErrorStream());
            while (scanner.hasNextLine()) {
                mWriter.append(scanner.nextLine());
                mWriter.append(System.lineSeparator());
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace(new PrintWriter(mWriter));
        }
        return mWriter.toString();
    }

    public String getLogs() {
        return mWriter.toString();
    }

    public void clear() {
        mWriter.flush();
    }
}

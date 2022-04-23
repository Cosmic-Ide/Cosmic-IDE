package com.pranav.lib_android.util;

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
            Process process = mProcess.start();
            Scanner scanner = new Scanner(process.getErrorStream());
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
            Process process = mProcess.start();
            Scanner scanner = new Scanner(process.getErrorStream());
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

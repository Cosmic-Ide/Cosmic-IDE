package com.pranav.lib_android.code.disassembler;

import com.sun.tools.javap.JavapTask;

import java.io.StringWriter;
import java.util.ArrayList;

public class JavapDisassembler {

    final String path;

    public JavapDisassembler(String classFile) {
        path = classFile;
    }

    public String disassemble() throws Throwable {
        // Create an arraylist for storing javap arguments
        var args = new ArrayList<String>();
        args.add("-c");
        args.add(path);
        // Create a StringWriter object that will store the output
        var writer = new StringWriter();
        // Create a JavapTask to handle the arguments
        var task = new JavapTask();
        task.handleOptions(args.toArray(new String[0]));
        task.setLog(writer);
        task.run();
        // return the disassembled file as string
        return writer.toString();
    }
}

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
        ArrayList<String> args = new ArrayList<>();
        args.add("-c");
        args.add(classFile);
        // Create a StringWriter object that will store the output
        StringWriter writer = new StringWriter();
        // Create a JavapTask to handle the arguments
        JavapTask task = new JavapTask();
        task.setLog(writer);
        task.run(args.toArray(new String[0]));
        // return the disassembled file as string
        return writer.toString();
    }
}

package com.pranav.java.ide.ui.treeview.helper;

import androidx.annotation.NonNull;

public class TreeCreateNewFileContent {

    public static String BUILD_NEW_FILE_CONTENT(@NonNull String fileName) {
        String content =
                "\n\nimport java.util.*;\n\n"
                        + "public class "
                        + fileName
                        + " {\n\n"
                        + "\tpublic static void main(String[] args) {\n"
                        + "\t\tSystem.out.println(\"Hello, World!\");\n"
                        + "\t}\n"
                        + "}\n";

        return content;
    }

    public static String BUILD_NEW_FILE_CONTENT_EXTEND_PACKAGE(
            @NonNull String fileName, String extendPackage) {
        String content =
                "package "
                        + extendPackage
                        + ";\n\nimport java.util.*;\n\n"
                        + "public class "
                        + fileName
                        + " {\n\n"
                        + "\tpublic static void main(String[] args) {\n"
                        + "\t\tSystem.out.print(\"Hello, World!\");\n"
                        + "\t}\n"
                        + "}\n";

        return content;
    }
}

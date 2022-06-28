package com.pranav.java.ide.ui.treeview.helper

import androidx.annotation.NonNull

object TreeCreateNewFileContent {

    @JvmStatic
    fun BUILD_NEW_FILE_CONTENT(@NonNull fileName: String) =
        """
import java.util.ArrayList;

public class $fileName {
    public static void main(String[] args) {
        System.out.println(\"Hello, World!\");
    }
}
        """

    @JvmStatic
    fun BUILD_NEW_FILE_CONTENT_EXTEND_PACKAGE(
            @NonNull fileName: String, extendPackage: String) =
        """
package $extendPackage;

import java.util.*;

public class $fileName {
    public static void main(String[] args) {
        System.out.print(\"Hello, World!\");
    }\n"
}
        """
}

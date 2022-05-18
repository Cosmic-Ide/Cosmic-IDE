package com.pranav.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class FileUtilTest {
    @Test
    public void mkdir_isWorking() {
        FileUtil.createDirectory("temp/dir");
    }

    @Test
    public void readWriter_isWorking() throws IOException {
        FileUtil.writeFile("tempFile.txt", "Temporary Text");
        Assertions.assertEquals("Temporary Text", FileUtil.readFile(new File("tempFile.txt")));
    }

    @Test
    public void getFileName_test() {
        String path = "TestFilePath.kt";
        Assertions.assertEquals(FileUtil.getFileName(path), "TestFilePath");
    }
}

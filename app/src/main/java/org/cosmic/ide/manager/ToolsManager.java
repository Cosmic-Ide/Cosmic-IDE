package org.cosmic.ide.manager;

import android.util.Log;

import org.cosmic.ide.App;
import org.cosmic.ide.common.util.FileUtil;
import org.cosmic.ide.common.util.ZipUtilKt;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ToolsManager {

    private static final String TAG = "ToolsManager";

    public static void init(Runnable onFinish) {

        CompletableFuture.runAsync(
                        () -> {
                            deleteCompilerModules();
                            extractAndroidJar();
                            writeKotlinStdLib();
                            writeKotlinStdLibDex();
                            writeKotlinCommonStdLib();
                            writeLambdaStubs();
                            writeIndex();
                        })
                .whenComplete(
                        (__, error) -> {
                            if (error != null) {
                                Log.d(TAG, "Error extracting tools " + error);
                            }

                            if (onFinish != null) {
                                onFinish.run();
                            }
                        });
    }

    private static void deleteCompilerModules() {
        final var compilerModules = new File(FileUtil.getDataDir(), "compiler-modules");

        if (compilerModules.exists()) {
            FileUtil.deleteFile(FileUtil.getDataDir() + "compiler-modules");
        }
    }

    private static void extractAndroidJar() {
        final var androidJar = new File(FileUtil.getClasspathDir(), "android.jar");

        if (!androidJar.exists()) {
            ZipUtilKt.unzipFromAssets(App.context, "android.jar.zip", FileUtil.getClasspathDir());
        }
    }

    private static void writeIndex() {
        final var index = new File(FileUtil.getDataDir(), "index.json");

        if (!index.exists()) {
            try {
                FileUtil.writeFile(
                        App.context.getAssets().open("index.json"), index.getAbsolutePath());
            } catch (IOException e) {
                Log.d(TAG, "Unable to extract index file");
            }
        }
    }

    private static void writeKotlinStdLib() {
        final var stdLib = new File(FileUtil.getClasspathDir(), "kotlin-stdlib-1.8.0.jar");

        if (!stdLib.exists()) {
            try {
                FileUtil.writeFile(
                        App.context.getAssets().open("kotlin-stdlib-1.8.0.jar"), stdLib.getAbsolutePath());
            } catch (IOException e) {
                Log.d(TAG, "Unable to extract kotlin stdlib file");
            }
        }
    }

    private static void writeKotlinStdLibDex() {
        final var dex = new File(FileUtil.getClasspathDir(), "kotlin-stdlib-1.8.0.dex");

        if (!dex.exists()) {
            try {
                FileUtil.writeFile(
                        App.context.getAssets().open("kotlin-stdlib-1.8.0.dex"), dex.getAbsolutePath());
            } catch (IOException e) {
                Log.d(TAG, "Unable to extract kotlin stdlib dex file");
            }
        }
    }

    private static void writeKotlinCommonStdLib() {
        final var commonStdLib =
                new File(FileUtil.getClasspathDir(), "kotlin-stdlib-common-1.8.0.jar");

        if (!commonStdLib.exists()) {
            try {
                FileUtil.writeFile(
                        App.context.getAssets().open("kotlin-stdlib-common-1.8.0.jar"),
                        commonStdLib.getAbsolutePath());
            } catch (IOException e) {
                Log.d(TAG, "Unable to extract kotlin common stdlib file");
            }
        }
    }

    private static void writeLambdaStubs() {
        final var lambdaStubs = new File(FileUtil.getClasspathDir(), "core-lambda-stubs.jar");

        if (!lambdaStubs.exists()) {
            try {
                FileUtil.writeFile(
                        App.context.getAssets().open("core-lambda-stubs.jar"),
                        lambdaStubs.getAbsolutePath());
            } catch (IOException e) {
                Log.d(TAG, "Unable to extract core lambda stubs file");
            }
        }
    }
}

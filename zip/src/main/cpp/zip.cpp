#include <iostream>
#include <fstream>
#include <string>
#include <filesystem>
#include <zlib.h>
#include <jni.h>
#include <stdio.h>
#include <vector>

namespace fs = std::filesystem;

void compressFile(const std::string &inputPath, const std::string &outputPath) {
    std::ifstream inputFile(inputPath, std::ios::binary);
    std::ofstream outputFile(outputPath, std::ios::binary);

    const int CHUNK_SIZE = 16384;
    char inBuffer[CHUNK_SIZE];
    char outBuffer[CHUNK_SIZE];

    z_stream zs;
    zs.zalloc = Z_NULL;
    zs.zfree = Z_NULL;
    zs.opaque = Z_NULL;
    deflateInit(&zs, Z_DEFAULT_COMPRESSION);

    while (!inputFile.eof()) {
        inputFile.read(inBuffer, CHUNK_SIZE);
        zs.avail_in = static_cast<uInt>(inputFile.gcount());
        zs.next_in = reinterpret_cast<Bytef *>(inBuffer);

        do {
            zs.avail_out = CHUNK_SIZE;
            zs.next_out = reinterpret_cast<Bytef *>(outBuffer);
            deflate(&zs, Z_FINISH);
            outputFile.write(outBuffer, CHUNK_SIZE - zs.avail_out);
        } while (zs.avail_out == 0);
    }

    deflateEnd(&zs);
    inputFile.close();
    outputFile.close();
}

void compressDirectory(const std::string &inputDir, const std::string &outputZip) {
    std::ofstream zipFile(outputZip, std::ios::binary);

    for (const auto &entry: fs::recursive_directory_iterator(inputDir)) {
        if (fs::is_regular_file(entry.path())) {
            const std::string relativePath = fs::relative(entry.path(), inputDir).string();
            const std::string zipEntryPath = fs::path(relativePath).generic_string();
            std::cout << "Compressing: " << zipEntryPath << std::endl;
            compressFile(entry.path().string(), zipEntryPath);
        }
    }

    zipFile.close();
}

extern "C" JNIEXPORT jstring JNICALL

Java_dev_pranav_zip_ZipHelper_compressDirectory(JNIEnv *env, jobject thiz, jstring input_path,
                                                jstring output_path) {
    const char *inputPath = env->GetStringUTFChars(input_path, 0);
    const char *outputPath = env->GetStringUTFChars(output_path, 0);

    compressDirectory(inputPath, outputPath);

    env->ReleaseStringUTFChars(input_path, inputPath);
    env->ReleaseStringUTFChars(output_path, outputPath);

    return env->NewStringUTF("File compressed successfully.");
}

}
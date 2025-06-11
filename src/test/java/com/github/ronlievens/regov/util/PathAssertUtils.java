package com.github.ronlievens.regov.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.ronlievens.regov.util.PathUtils.findFileInClasspath;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PathAssertUtils {

    public static String readFileAsStringFromClasspath(final String fileName) throws IOException {
        val file = findFileInClasspath(fileName);
        return Files.readString(file);
    }


    public static void writeToFileInTargetDirectory(final String value, final String fileName) throws IOException {
        writeToFileInTargetDirectory(value.getBytes(), fileName);
    }

    public static void writeToFileInTargetDirectory(final byte[] value, final String fileName) throws IOException {
        val file = findFileInTargetDirectory(fileName);
        Files.write(file, value);
    }


    public static Path findFileInTargetDirectory(final String fileName) throws IOException {
        val path = Paths.get("target", fileName);
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        return path;
    }
}

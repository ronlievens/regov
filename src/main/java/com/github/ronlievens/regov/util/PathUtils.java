package com.github.ronlievens.regov.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PathUtils {

    public static InputStream getFileFromClasspath(final String fileName) {
        return Objects.requireNonNull(PathUtils.class.getClassLoader().getResourceAsStream(fileName));
    }

    public static Path findFileInClasspath(@NonNull final String fileName) throws FileNotFoundException {
        try {
            return Paths.get(Objects.requireNonNull(PathUtils.class.getClassLoader().getResource(fileName)).toURI());
        } catch (URISyntaxException e) {
            throw new FileNotFoundException("Unable to find file: %s".formatted(fileName));
        }
    }

    public static boolean writeStringToFile(@NonNull final String content, @NonNull final Path file) {
        if (createDirectory(file.getParent())) {
            try {
                Files.write(file, content.getBytes());
            } catch (IOException ioe) {
                log.warn("Unable to create file, {}", ioe.getMessage());
                return false;
            }
            return true;
        }
        return false;
    }


    public static boolean createDirectory(@NonNull final Path path) {
        if (!Files.exists(path)) {
            log.trace("Creating directory: {}", path.toAbsolutePath());
            try {
                Files.createDirectories(path);
            } catch (IOException ioe) {
                log.warn("Unable to create directory, {}", ioe.getMessage());
                return false;
            }
        } else {
            log.trace("Directory exist, nothing to create: {}", path.toAbsolutePath());
        }
        return true;
    }

    public static boolean deleteDirectory(@NonNull final Path path) {
        if (Files.exists(path)) {
            log.trace("Deleting directory: {}", path.toAbsolutePath());
            try (Stream<Path> paths = Files.walk(path)) {
                paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(f -> {
                        try {
                            if (f.isDirectory()) {
                                FileUtils.deleteDirectory(f);
                            } else {
                                FileUtils.forceDelete(f);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            } catch (IOException ioe) {
                log.warn("Unable to delete directory, {}", ioe.getMessage());
                return false;
            }
        } else {
            log.trace("Directory doesn't exist , nothing to delete: {}", path.toAbsolutePath());
        }
        return true;
    }

    public static boolean deleteFolderWhenItExists(final Path path, final boolean force) throws IOException {
        if (Files.exists(path) && !isEmptyDirectory(path)) {
            if (!force) {
                val scanner = new Scanner(System.in);
                System.out.printf("Do you want to delete the directory '%s'? This cannot be undone! (Y|n)", path.toAbsolutePath());
                val userInput = scanner.next().trim().toUpperCase();
                if (!"Y".equals(userInput)) {
                    throw new IOException("Directory [%s] delete command is cancelled.".formatted(path.toAbsolutePath()));
                }
            }
            return deleteDirectory(path);
        }
        return true;
    }

    public static boolean isEmptyDirectory(Path dir) throws IOException {
        try (var entries = Files.list(dir)) {
            return entries.findAny().isEmpty();
        }
    }


    public static List<Path> pathFilter(@NonNull final Path directory, final List<String> filterEndsWith, final List<String> filterIn, final List<String> filterOut) throws IOException {
        try (val stream = Files.walk(directory)) {
            return pathFilter(stream, directory, filterEndsWith, filterIn, filterOut);
        }
    }

    public static List<Path> pathFilter(@NonNull final List<Path> files, @NonNull final Path directory, final List<String> filterEndsWith, final List<String> filterIn, final List<String> filterOut) throws IOException {
        try (val stream = files.stream()) {
            return pathFilter(stream, directory, filterEndsWith, filterIn, filterOut);
        }
    }

    public static List<Path> pathFilter(@NonNull final Stream<Path> stream, @NonNull final Path directory, final List<String> filterEndsWith, final List<String> filterIn, final List<String> filterOut) throws IOException {
        log.trace("List directory: {}", stream);
        return stream
            .filter(filterPath -> !pathContains(false, directory.relativize(filterPath), filterOut))
            .filter(filterPath -> pathEndsWith(directory.relativize(filterPath), filterEndsWith))
            .filter(filterPath -> pathContains(true, directory.relativize(filterPath), filterIn))
            .map(Path::toAbsolutePath)
            .toList();
    }

    public static boolean pathEndsWith(@NonNull final Path path, final List<String> endsWith) {
        if (endsWith == null) {
            return true;
        }

        for (val filter : endsWith) {
            if (isBlank(filter)) {
                return true;
            }

            val normalizedPath = path.normalize().toString();
            if (normalizedPath.contains(filter)) {
                log.trace("Filtering path [{}] with filter [{}] true", normalizedPath, filter);
                return true;
            }
            log.trace("Filtering path [{}] with filter [{}] false", normalizedPath, filter);
        }
        return false;
    }

    public static boolean pathContains(final boolean out, @NonNull final Path path, final List<String> contains) {
        if (contains == null) {
            return out;
        }

        for (val filter : contains) {
            if (isBlank(filter)) {
                return out;
            }

            val normalizedFilter = Path.of(filter).normalize().toString();
            val normalizedPath = path.normalize().toString();
            if (normalizedPath.contains(normalizedFilter)) {
                log.trace("Filtering path [{}] with filter [{}] true", normalizedPath, normalizedFilter);
                return true;
            }
            log.trace("Filtering path [{}] with filter [{}] false", normalizedPath, normalizedFilter);
        }
        return false;
    }
}

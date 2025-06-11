package com.github.ronlievens.regov.shell;

import com.github.ronlievens.regov.shell.model.ShellResult;
import com.github.ronlievens.regov.util.LogbackUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public class Shell {

    private static final Logger TRACE_LOGGER = LogbackUtils.getTraceLogger();

    public static boolean isCommandAvailable(String command) {
        val extensions = System.getProperty("os.name").toLowerCase().contains("win")
            ? new String[]{"", ".exe", ".cmd", ".bat"}
            : new String[]{"", ".sh"};

        return Stream.of(System.getenv("PATH").split(File.pathSeparator))
            .map(File::new)
            .filter(File::exists)
            .flatMap(path -> Stream.of(extensions)
                .map(ext -> new File(path, command + ext)))
            .anyMatch(file -> file.exists() && file.canExecute());
    }

    public ShellResult execute(@NonNull final String cmd, final boolean trace) {
        return execute(cmd, null, trace);
    }

    public ShellResult execute(@NonNull final String cmd, final Path path, final boolean trace) {
        if (trace) {
            log.info("Executing in path: {}", path);
            log.info("Executing: {}", cmd);
        }

        TRACE_LOGGER.trace("============================================================================================");
        if (path != null) {
            TRACE_LOGGER.trace("Path: {}", path.toAbsolutePath());
        }
        TRACE_LOGGER.trace("Command: {}", cmd);

        try {
            val process = Runtime.getRuntime().exec(getShellCommand(cmd), null, path != null ? path.toFile() : null);

            val result = Stream.of(process.getInputStream())
                .map(Shell::parseStream)
                .collect(Collectors.joining());

            val error = Stream.of(process.getErrorStream())
                .map(Shell::parseStream)
                .collect(Collectors.joining());

            process.waitFor();
            if (trace && StringUtils.isNotBlank(error)) {
                if (process.exitValue() != 0) {
                    log.warn(error.replace(System.lineSeparator(), ""));
                } else {
                    log.debug(error);
                }
            }

            TRACE_LOGGER.trace("Result: {}", result);
            TRACE_LOGGER.trace("============================================================================================");
            return new ShellResult(process.exitValue(), result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static StringBuilder parseStream(@NonNull final InputStream isForOutput) {
        val output = new StringBuilder();
        try (val br = new BufferedReader(new InputStreamReader(isForOutput))) {
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line);
                output.append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    private static String[] getShellCommand(@NonNull final String cmd) {
        log.trace("Arch: {}, OS: {}", SystemUtils.OS_ARCH, SystemUtils.OS_NAME);
        val cmdArray = new ArrayList<String>();
        if (SystemUtils.IS_OS_WINDOWS) {
            cmdArray.add("cmd");
            cmdArray.add("/c");
        } else {
            var shell = System.getenv("SHELL");
            if (isBlank(shell)) {
                shell = "/bin/bash";
            }
            cmdArray.add(shell);
            cmdArray.add("-c");
        }
        cmdArray.add(cmd);
        return cmdArray.toArray(new String[0]);
    }
}

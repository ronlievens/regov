package com.github.ronlievens.regov.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ronlievens.regov.task.config.model.SettingModel;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static com.github.ronlievens.regov.util.MapperUtils.createJsonMapper;
import static com.github.ronlievens.regov.util.PathUtils.createDirectory;
import static com.github.ronlievens.regov.util.PathUtils.writeStringToFile;

@Slf4j
public final class PropertyUtils {

    private static final String DIRECTORY_POSTFIX = ".config/regov";
    private static final String PROFILE = "%s.json";
    private static final ObjectMapper MAPPER = createJsonMapper();

    public static SettingModel load(@NonNull final String profile) {
        return load(profile, null);
    }

    public static SettingModel load(@NonNull final String profile, final Path path) {
        val configDirectory = Objects.requireNonNullElseGet(path, () -> Path.of(System.getProperty("user.home"), DIRECTORY_POSTFIX));

        if (!Files.exists(configDirectory)) {
            createDirectory(configDirectory);
        } else if (!Files.isDirectory(configDirectory)) {
            throw new RuntimeException("Config directory is not a directory: %s".formatted(configDirectory.toAbsolutePath()));
        }

        return read(configDirectory, PROFILE.formatted(profile));
    }

    private static SettingModel read(final Path path, final String filename) {
        val file = path.resolve(filename);
        log.debug("Reading properties from: {}", file.toAbsolutePath());
        if (Files.exists(file) && !Files.isDirectory(file)) {
            try (val input = new FileInputStream(file.toFile())) {
                return MAPPER.readValue(input, SettingModel.class);
            } catch (Exception io) {
                log.error("Exception: {}", io.getMessage());
                throw new RuntimeException(io);
            }
        }

        val settings = new SettingModel();
        try {
            writeStringToFile(MAPPER.writeValueAsString(settings), file);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return settings;
    }
}

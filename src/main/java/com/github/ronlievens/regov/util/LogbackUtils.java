package com.github.ronlievens.regov.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class LogbackUtils {

    private static final String TRACE_LOGGING_NAME = "REGOV_TRACE";

    public static void attachLogFile(@NonNull final String logFile) {
        val loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        val encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%date %-5level [%thread][%logger{0}:%line] %message%n");
        encoder.start();

        val fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("file-appender");
        fileAppender.setFile(logFile);
        fileAppender.setEncoder(encoder);
        fileAppender.start();

        val logbackLogger = loggerContext.getLogger("root");
        logbackLogger.addAppender(fileAppender);
    }

    public static void adjustLogLevel(@NonNull final String logLevel) {
        if (isNotBlank(logLevel)) {
            val loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            val logger = loggerContext.exists("com.github.ronlievens.regov");
            val newLevel = Level.toLevel(logLevel.toUpperCase(), null);
            logger.setLevel(newLevel);
        }
    }

    public static boolean isTrace() {
        val logger = LoggerFactory.getLogger(LogbackUtils.class);
        return logger.isTraceEnabled();
    }

    // ==[ TRACE LOGGING ]==============================================================================================
    public static Logger getTraceLogger() {
        return LoggerFactory.getLogger(TRACE_LOGGING_NAME);
    }

    public static void attachTraceLogging(@NonNull final Path output) {
        attachTraceLogging(null, output);
    }

    public static void attachTraceLogging(final String filenamePrefix, @NonNull final Path output) {

        var logFile = generateTraceLogFile(output, filenamePrefix, null);
        int postfixIndex = 0;
        while (Files.exists(logFile)) {
            logFile = generateTraceLogFile(output, filenamePrefix, postfixIndex);
            postfixIndex++;
        }

        val loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        val encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%message%n");
        encoder.start();

        val traceAppender = new FileAppender<ILoggingEvent>();
        traceAppender.setContext(loggerContext);
        traceAppender.setName("trace-appender");
        traceAppender.setFile(logFile.toAbsolutePath().toString());
        traceAppender.setEncoder(encoder);
        traceAppender.start();

        val logger = loggerContext.getLogger(TRACE_LOGGING_NAME);
        logger.addAppender(traceAppender);
        logger.setLevel(Level.TRACE);
        logger.setAdditive(false);
    }

    private static Path generateTraceLogFile(@NonNull final Path output, final String filenamePrefix, final Integer iteration) {
        if (iteration != null) {
            if (isNotBlank(filenamePrefix)) {
                return output.resolve("%s-trace.%s.log".formatted(filenamePrefix, iteration));
            }

            return output.resolve("trace.%s.log".formatted(iteration));
        }

        if (isNotBlank(filenamePrefix)) {
            return output.resolve("%s-trace.log".formatted(filenamePrefix));
        }

        return output.resolve("trace.log");
    }
}

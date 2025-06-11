package com.github.ronlievens.regov.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;

import java.util.TimeZone;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MapperUtils {

    public static ObjectMapper createJsonMapper() {
        return createJsonMapper(true, true);
    }

    public static XmlMapper createXmlMapper() {
        return createXmlMapper(true, true);
    }

    public static ObjectMapper createYamlMapper() {
        return createYamlMapper(true, true);
    }

    public static ObjectMapper createJsonMapper(final boolean ignoreUnknownFields, final boolean hideEmptyFields) {
        val mapper = new ObjectMapper();
        setupMapper(mapper, ignoreUnknownFields, hideEmptyFields);
        return mapper;
    }

    public static XmlMapper createXmlMapper(final boolean ignoreUnknownFields, final boolean hideEmptyFields) {
        val mapper = new XmlMapper();
        setupMapper(mapper, ignoreUnknownFields, hideEmptyFields);
        return mapper;
    }

    public static ObjectMapper createYamlMapper(final boolean ignoreUnknownFields, final boolean hideEmptyFields) {
        val mapper = new ObjectMapper(new YAMLFactory());
        setupMapper(mapper, ignoreUnknownFields, hideEmptyFields);
        return mapper;
    }

    private static void setupMapper(final ObjectMapper objectMapper, final boolean ignoreUnknownFields, final boolean hideEmptyFields) {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        if (hideEmptyFields) {
            objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
        }
        objectMapper.setTimeZone(TimeZone.getDefault());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, !ignoreUnknownFields);
    }
}

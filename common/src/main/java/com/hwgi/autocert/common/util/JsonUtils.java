package com.hwgi.autocert.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;

/**
 * JSON 직렬화/역직렬화 유틸리티
 */
public final class JsonUtils {

    private JsonUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    /**
     * ObjectMapper 인스턴스 반환
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 객체를 JSON 문자열로 변환
     */
    public static String toJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * 객체를 포맷된 JSON 문자열로 변환
     */
    public static String toPrettyJson(Object object) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to pretty JSON", e);
        }
    }

    /**
     * JSON 문자열을 객체로 변환
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize JSON to object", e);
        }
    }

    /**
     * JSON 문자열을 객체로 변환 (제네릭 타입 지원)
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize JSON to object", e);
        }
    }

    /**
     * 객체를 다른 타입으로 변환 (JSON을 거쳐서)
     */
    public static <T> T convert(Object object, Class<T> clazz) {
        return OBJECT_MAPPER.convertValue(object, clazz);
    }

    /**
     * 객체를 다른 타입으로 변환 (제네릭 타입 지원)
     */
    public static <T> T convert(Object object, TypeReference<T> typeReference) {
        return OBJECT_MAPPER.convertValue(object, typeReference);
    }
}
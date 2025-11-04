package com.hwgi.autocert.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonUtils 테스트")
class JsonUtilsTest {

    @Test
    @DisplayName("객체를 JSON으로 직렬화")
    void testToJson() {
        TestObject object = new TestObject("test", 123);

        String json = JsonUtils.toJson(object);

        assertTrue(json.contains("\"name\":\"test\""));
        assertTrue(json.contains("\"value\":123"));
    }

    @Test
    @DisplayName("JSON을 객체로 역직렬화")
    void testFromJson() {
        String json = "{\"name\":\"test\",\"value\":123}";

        TestObject object = JsonUtils.fromJson(json, TestObject.class);

        assertEquals("test", object.name);
        assertEquals(123, object.value);
    }

    @Test
    @DisplayName("JSON을 Map으로 역직렬화")
    void testFromJsonToMap() {
        String json = "{\"name\":\"test\",\"value\":123}";

        Map<String, Object> map = JsonUtils.fromJson(json, new TypeReference<Map<String, Object>>() {});

        assertEquals("test", map.get("name"));
        assertEquals(123, map.get("value"));
    }

    @Test
    @DisplayName("포맷된 JSON 생성")
    void testToPrettyJson() {
        TestObject object = new TestObject("test", 123);

        String json = JsonUtils.toPrettyJson(object);

        assertTrue(json.contains("\n"));
        assertTrue(json.contains("  "));
    }

    @Test
    @DisplayName("객체 타입 변환")
    void testConvert() {
        Map<String, Object> map = Map.of("name", "test", "value", 123);

        TestObject object = JsonUtils.convert(map, TestObject.class);

        assertEquals("test", object.name);
        assertEquals(123, object.value);
    }

    // 테스트용 클래스
    static class TestObject {
        public String name;
        public int value;

        public TestObject() {}

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
}
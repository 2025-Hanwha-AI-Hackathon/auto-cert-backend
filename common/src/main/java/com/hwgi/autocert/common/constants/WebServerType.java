package com.hwgi.autocert.common.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지원하는 웹서버 타입
 */
@Getter
@RequiredArgsConstructor
public enum WebServerType {
    
    NGINX("nginx", "Nginx"),
    APACHE("apache", "Apache HTTP Server"),
    TOMCAT("tomcat", "Apache Tomcat"),
    WEBTOB("webtob", "WebtoB"),
    IIS("iis", "Microsoft IIS"),
    JEUS("jeus", "JEUS"),
    WEBLOGIC("weblogic", "Oracle WebLogic");

    @JsonValue
    private final String code;
    private final String displayName;

    /**
     * code 문자열로부터 enum 조회
     */
    @JsonCreator
    public static WebServerType fromCode(String code) {
        if (code == null) {
            return null;
        }
        
        for (WebServerType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("지원하지 않는 웹서버 타입입니다: " + code);
    }

    /**
     * 문자열이 유효한 웹서버 타입 코드인지 확인
     */
    public static boolean isValidCode(String code) {
        if (code == null) {
            return false;
        }
        
        for (WebServerType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return true;
            }
        }
        
        return false;
    }
}

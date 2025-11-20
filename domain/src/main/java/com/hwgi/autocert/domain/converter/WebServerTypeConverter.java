package com.hwgi.autocert.domain.converter;

import com.hwgi.autocert.common.constants.WebServerType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * WebServerType enum과 DB 문자열 간 변환을 위한 JPA Converter
 */
@Converter(autoApply = true)
public class WebServerTypeConverter implements AttributeConverter<WebServerType, String> {

    @Override
    public String convertToDatabaseColumn(WebServerType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public WebServerType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return WebServerType.fromCode(dbData);
    }
}

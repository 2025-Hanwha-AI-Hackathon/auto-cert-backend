package com.hwgi.autocert.domain.converter;

import com.hwgi.autocert.domain.model.CertificateStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * CertificateStatus enum과 DB 문자열 간 변환을 위한 JPA Converter
 */
@Converter(autoApply = true)
public class CertificateStatusConverter implements AttributeConverter<CertificateStatus, String> {

    @Override
    public String convertToDatabaseColumn(CertificateStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public CertificateStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return CertificateStatus.fromCode(dbData);
    }
}

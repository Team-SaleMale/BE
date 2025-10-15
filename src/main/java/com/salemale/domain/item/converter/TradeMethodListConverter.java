package com.salemale.domain.item.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salemale.global.common.enums.TradeMethod;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Converter
public class TradeMethodListConverter implements AttributeConverter<List<TradeMethod>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<TradeMethod> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }

        try {
            List<String> names = attribute.stream()
                    .map(Enum::name)
                    .toList();
            return objectMapper.writeValueAsString(names);
        } catch (JsonProcessingException e) {
            log.error("TradeMethod List를 JSON으로 변환 실패", e);
            return "[]";
        }
    }

    @Override
    public List<TradeMethod> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty() || "[]".equals(dbData)) {
            return new ArrayList<>();
        }

        try {
            List<String> names = objectMapper.readValue(dbData, new TypeReference<List<String>>() {});
            return names.stream()
                    .map(TradeMethod::valueOf)
                    .toList();
        } catch (JsonProcessingException e) {
            log.error("JSON을 TradeMethod List로 변환 실패: {}", dbData, e);
            return new ArrayList<>();
        }
    }
}
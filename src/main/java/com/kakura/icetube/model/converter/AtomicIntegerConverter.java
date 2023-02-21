package com.kakura.icetube.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.concurrent.atomic.AtomicInteger;

@Converter(autoApply = true)
public class AtomicIntegerConverter implements AttributeConverter<AtomicInteger, Integer> {


    @Override
    public Integer convertToDatabaseColumn(AtomicInteger atomicInteger) {
        if (atomicInteger == null) {
            return null;
        } else {
            return atomicInteger.get();
        }
    }

    @Override
    public AtomicInteger convertToEntityAttribute(Integer integer) {
        return new AtomicInteger(integer);
    }
}

package com.mercateo.common.rest.schemagen.better.property;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.security.acl.AclNotFoundException;
import java.util.Arrays;
import java.util.stream.Stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mercateo.common.rest.schemagen.better.property.ImmutableRawProperty;
import com.mercateo.common.rest.schemagen.generictype.GenericType;

public class FieldCollector implements RawPropertyCollector {

    private final FieldCollectorConfig config;

    private final AnnotationMapBuilder annotationMapBuilder;

    public FieldCollector(FieldCollectorConfig config) {
        this.config = config;
        annotationMapBuilder = new AnnotationMapBuilder();
    }

    @Override
    public Stream<RawProperty> forType(GenericType<?> genericType) {
        return Arrays.stream(genericType.getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .filter(field -> config.includePrivateFields() || Modifier.isPublic(field.getModifiers()))
                .map(field -> mapRawDataProperty(field, genericType.getType()));
    }

    private RawProperty mapRawDataProperty(Field field, Type type) {

        return ImmutableRawProperty.of(field.getName(), GenericType.of(field, type), annotationMapBuilder.createMap(field.getAnnotations()), (Object object) -> valueAccessor(field, object));
    }

    private Object valueAccessor(Field field, Object object) {
        try {
            if (config.includePrivateFields()) {
                field.setAccessible(true);
            }
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}

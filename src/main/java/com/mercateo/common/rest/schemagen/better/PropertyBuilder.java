package com.mercateo.common.rest.schemagen.better;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.mercateo.common.rest.schemagen.PropertyType;
import com.mercateo.common.rest.schemagen.PropertyTypeMapper;
import com.mercateo.common.rest.schemagen.generictype.GenericType;

public class PropertyBuilder {

    ConcurrentHashMap<GenericType<?>, PropertyDescriptor> knownDescriptors = new ConcurrentHashMap<>();

    public Property from(Class<?> propertyClass) {
        return from(GenericType.of(propertyClass));
    }

    public Property from(Class<?> propertyClass, Type propertyType) {
        return from(GenericType.of(propertyType, propertyClass));
    }

    public Property from(GenericType<?> genericType) {
        return from("#", genericType, Collections.emptyList(), object -> {
            throw new IllegalStateException("cannot call value accessor for root element");
        });
    }

    public Property from(String name, GenericType<?> genericType, List<Annotation> annotations,
            Function valueAccessor) {
        final PropertyDescriptor propertyDescriptor = getPropertyDescriptor(genericType);

        Collection<Annotation> propertyAnnotations = new ArrayList<>(annotations);
        propertyAnnotations.addAll(propertyDescriptor.annotations());

        return ImmutableProperty.of(name, propertyDescriptor, valueAccessor, propertyAnnotations);
    }

    private PropertyDescriptor getPropertyDescriptor(GenericType<?> genericType) {
        return knownDescriptors.computeIfAbsent(genericType, this::createPropertyDescriptor);
    }

    private PropertyDescriptor createPropertyDescriptor(GenericType<?> genericType) {
        final PropertyType propertyType = PropertyTypeMapper.of(genericType);
        final List<Property> children = propertyType == PropertyType.OBJECT ? createChildProperties(
                genericType) : Collections.emptyList();

        final List<Annotation> classAnnotations = Arrays.asList(genericType.getRawType()
                .getAnnotations());
        return ImmutablePropertyDescriptor.of(genericType, children, classAnnotations);
    }

    private List<Property> createChildProperties(GenericType<?> genericType) {
        final List<Property> children = new ArrayList<>();
        do {
            if (genericType.getRawType() != Object.class) {
                final Field[] declaredFields = genericType.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    if (!declaredField.isSynthetic()) {
                        final Property child = createProperty(GenericType.of(declaredField,
                                genericType.getType()), declaredField, declaredField.getName());
                        children.add(child);
                    }
                }
            }
        } while (null != (genericType = genericType.getSuperType()));
        return children;
    }

    private Property createProperty(GenericType genericType, Field declaredField,
            String childName) {
        final List<Annotation> childAnnotations = Arrays.asList(declaredField.getAnnotations());
        return from(childName, genericType, childAnnotations, object -> {
            try {
                return declaredField.get(object);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        });
    }
}

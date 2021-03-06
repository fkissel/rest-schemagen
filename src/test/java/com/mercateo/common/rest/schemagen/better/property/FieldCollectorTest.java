package com.mercateo.common.rest.schemagen.better.property;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import com.mercateo.common.rest.schemagen.better.property.FieldCollector;
import com.mercateo.common.rest.schemagen.better.property.FieldCollectorConfig;
import com.mercateo.common.rest.schemagen.better.property.RawProperty;
import org.junit.Before;
import org.junit.Test;

import com.mercateo.common.rest.schemagen.generictype.GenericType;

public class FieldCollectorTest {

    private FieldCollector fieldCollector;

    @Before
    public void setUp() throws Exception {
        fieldCollector = new FieldCollector(FieldCollectorConfig.builder().build());
    }

    @Test
    public void mapAllDeclaredFields() throws Exception {
        final List<RawProperty> properties = fieldCollector.forType(GenericType.of(
                PropertyHolder.class)).collect(Collectors.toList());

        assertThat(properties).extracting(RawProperty::name).containsExactlyInAnyOrder("hidden",
                "visible");
    }

    @Test
    public void limitToPublicFieldsIfConfigured() throws Exception {
        fieldCollector = new FieldCollector(FieldCollectorConfig.builder().withIncludePrivateFields(false).build());
        final List<RawProperty> properties = fieldCollector.forType(GenericType.of(
                PropertyHolder.class)).collect(Collectors.toList());

        assertThat(properties).extracting(RawProperty::name).containsExactly("visible");
    }

    @Test
    public void mapAllDaeclaredFields() throws Exception {
        final RawProperty hidden = fieldCollector.forType(GenericType.of(PropertyHolder.class))
                .filter(p -> p.name().equals("hidden")).findFirst().get();

        final PropertyHolder propertyHolder = new PropertyHolder();
        hidden.valueAccessor().apply(propertyHolder);
    }

    public static class PropertyHolder {
        public String visible;

        private String hidden;
    }
}
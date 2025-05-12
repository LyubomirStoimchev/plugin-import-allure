package com.epam.reportportal.extension.allure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a label with a name and a value. The label can be used
 * to provide additional information or metadata.
 *
 * The {@code Label} class is designed to hold a pair of related
 * string values: {@code name} and {@code value}. This can be used to
 * classify or categorize data. It is particularly useful when associating
 * labels with objects in broader contexts such as reporting or analytics.
 *
 * The class supports serialization and deserialization while
 * ignoring unknown properties during JSON processing to provide
 * flexibility in handling external data sources.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Label {
    private String name;
    private String value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
package com.epam.reportportal.extension.allure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents an attachment associated with a step or test result,
 * typically used in the context of a testing framework like Allure.
 *
 * An Attachment provides metadata about a related artifact, such as:
 * - The name of the attachment.
 * - The source location where the attachment is stored (e.g., file path, URL).
 * - The type of the attachment, indicating the content type (e.g., image/png, text/plain).
 *
 * This class supports serialization to facilitate integration with structured reporting tools
 * and deserialization to reconstruct attachment objects from stored data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Attachment {
    private String name;
    private String source;
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

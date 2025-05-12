package com.epam.reportportal.extension.allure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents detailed information about a status, providing additional context or explanation.
 *
 * The {@code StatusDetails} class is commonly used to encapsulate supplementary data
 * about the outcome of a test execution, such as error messages or stack traces.
 * It enables structured reporting by associating human-readable information with a status.
 *
 * This class supports the following key fields:
 * - {@code message}: A descriptive message providing context or detail about the status.
 * - {@code trace}: A stack trace or additional debug information related to the status.
 *
 * This class is designed to be flexible and interoperable, with support for JSON
 * serialization and deserialization. Unknown properties during JSON processing are ignored
 * to facilitate integration with various data sources.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusDetails {
    private String message;
    private String trace;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }
}
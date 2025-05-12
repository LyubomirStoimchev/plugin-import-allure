package com.epam.reportportal.extension.allure.model;

import com.epam.ta.reportportal.ws.reporting.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.reporting.Mode;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Set;

/**
 * Represents a request object for launching an import operation.
 * This class encapsulates all necessary attributes that are required
 * to initialize and configure a launch import.
 *
 * The LaunchImportRQ class includes the following fields:
 * - `name` (String): The name of the launch.
 * - `description` (String): A brief description of the launch.
 * - `attributes` (Set<ItemAttributesRQ>): Additional attributes or metadata
 *   associated with the launch. Can include tags or other descriptors.
 * - `startTime` (Instant): The timestamp indicating when the launch started.
 * - `mode` (Mode): The mode of the launch, which determines its execution mode.
 *
 * Getters provide read access to these fields, and the `startTime` field also
 * has a setter to allow modification. The class relies on annotations for JSON
 * serialization and deserialization.
 */
public class LaunchImportRQ {
    @JsonProperty(value = "name")
    protected String name;

    @JsonProperty(value = "description")
    private String description;

    @JsonProperty("attributes")
    @JsonAlias({"attributes", "tags"})
    private Set<ItemAttributesRQ> attributes;

    @JsonProperty
    @JsonAlias({"startTime", "start_time"})
    private Instant startTime;

    @JsonProperty("mode")
    private Mode mode;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Set<ItemAttributesRQ> getAttributes() {
        return attributes;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Mode getMode() {
        return mode;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
}
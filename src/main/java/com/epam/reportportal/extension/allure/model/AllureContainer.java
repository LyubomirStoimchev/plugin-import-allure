package com.epam.reportportal.extension.allure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents a container in the Allure reporting system.
 * The container encapsulates metadata and hierarchical relationships
 * of test steps and their children.
 *
 * This class supports serialization and deserialization as part
 * of generating structured reports for test execution.
 *
 * The container may include pre-execution steps (befores),
 * post-execution steps (afters), and direct child elements.
 *
 * Fields in this class:
 * - uuid: Unique identifier for the container.
 * - name: The name of the container.
 * - befores: A list of setup steps executed before the container's main process.
 * - afters: A list of cleanup steps executed after the container's main process.
 * - children: A list of UUIDs representing child elements associated with this container.
 *
 * The class is intended to be used as part of an Allure report structure,
 * providing organization of test execution details.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllureContainer {
    private String uuid;
    private String name;
    private List<AllureStep> befores;
    private List<AllureStep> afters;
    private List<String> children;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<AllureStep> getBefores() {
        return befores;
    }

    public void setBefores(List<AllureStep> befores) {
        this.befores = befores;
    }

    public List<AllureStep> getAfters() {
        return afters;
    }

    public void setAfters(List<AllureStep> afters) {
        this.afters = afters;
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }
}
package com.epam.reportportal.extension.allure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents a step in an Allure report, defining the granular details of a test process.
 *
 * An AllureStep encapsulates information about a single test step, including:
 * - The name of the step.
 * - The status of the step (e.g., passed, failed, etc.).
 * - Status details, which provide additional information about the step's outcome.
 * - Nested child steps that represent a hierarchy of test operations.
 * - Attachments that provide additional context or artifacts related to the step (e.g., screenshots, logs).
 * - Timing information (start and stop), which records when the step occurred.
 *
 * This class supports serialization and deserialization for inclusion in structured reports.
 * It can be used to model detailed test execution processes in the Allure reporting framework.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllureStep {
    private String name;
    private String status;
    private StatusDetails statusDetails;
    private List<AllureStep> steps;
    private List<Attachment> attachments;
    private Long start;
    private Long stop;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public StatusDetails getStatusDetails() {
        return statusDetails;
    }

    public void setStatusDetails(StatusDetails statusDetails) {
        this.statusDetails = statusDetails;
    }

    public List<AllureStep> getSteps() {
        return steps;
    }

    public void setSteps(List<AllureStep> steps) {
        this.steps = steps;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getStop() {
        return stop;
    }

    public void setStop(Long stop) {
        this.stop = stop;
    }
}

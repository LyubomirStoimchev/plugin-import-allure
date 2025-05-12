package com.epam.reportportal.extension.allure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents the result of a test in an Allure report.
 *
 * This class models the various attributes and metadata associated with a single test execution.
 * It includes details such as the test's unique identifier, name, execution status, associated labels,
 * steps, attachments, and timing information.
 *
 * Fields:
 * - uuid: A unique identifier for the test case.
 * - name: The display name of the test.
 * - fullName: The fully qualified name of the test.
 * - status: The test execution status (e.g., passed, failed, skipped, etc.).
 * - statusDetails: Additional information about the test's status, such as error messages or stack traces.
 * - labels: A collection of metadata labels associated with the test, defining categories or characteristics.
 * - steps: A list of steps executed as part of the test, represented by the AllureStep class.
 * - attachments: A list of artifacts related to the test, such as logs or screenshots.
 * - start: The start time of the test, represented as a UNIX timestamp.
 * - stop: The stop time of the test, represented as a UNIX timestamp.
 * - historyId: A unique identifier linking related test runs in test history.
 *
 * This class supports serialization and deserialization for inclusion in Allure reports.
 * It is capable of providing a specific label's value via the helper method getLabelValue.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AllureTestResult {
    private String uuid;
    private String name;
    private String fullName;
    private String status;
    private StatusDetails statusDetails;
    private List<Label> labels;
    private List<AllureStep> steps;
    private List<Attachment> attachments;
    private Long start;
    private Long stop;
    private String historyId;

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    // Helper to get a label's value by name
    public String getLabelValue(String name) {
        if (labels == null) return null;
        for (Label l : labels) {
            if (name.equals(l.getName())) return l.getValue();
        }
        return null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public List<Label> getLabels() {
        return labels;
    }

    public void setLabels(List<Label> labels) {
        this.labels = labels;
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
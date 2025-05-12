package com.epam.reportportal.extension.allure.service;

import com.epam.reportportal.events.*;
import com.epam.reportportal.extension.allure.model.*;
import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.epam.ta.reportportal.ws.reporting.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.epam.reportportal.rules.exception.ErrorType.BAD_REQUEST_ERROR;

public class AllureZipImportStrategy extends AbstractImportStrategy {

    private static final String ALLURE_RESULTS_DIR = "allure-results/";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ApplicationEventPublisher _eventPublisher;
    private LaunchRepository _launchRepo;

    public AllureZipImportStrategy(ApplicationEventPublisher eventPublisher, LaunchRepository launchRepo) {
        super(eventPublisher, launchRepo);
        _eventPublisher = eventPublisher;
        _launchRepo = launchRepo;
    }

    private File transferToTempFile(MultipartFile file) {
        try {
            File tmp = File.createTempFile(file.getOriginalFilename(),
                    "." + FilenameUtils.getExtension(file.getOriginalFilename()));
            file.transferTo(tmp);
            return tmp;
        } catch (IOException e) {
            throw new ReportPortalException("Error during transferring multipart file.", e);
        }
    }

    @Override
    public String importLaunch(MultipartFile file, String projectName, LaunchImportRQ launchRq) {
        // Transfer zip to temp file and open
        File zipFile = transferToTempFile(file);
        String launchUuid = null;
        try (ZipFile zip = new ZipFile(zipFile)) {
            // Verify required folder
            if (zip.getEntry(ALLURE_RESULTS_DIR) == null) {
                throw new ReportPortalException(BAD_REQUEST_ERROR, "Zip does not contain an 'allure-results' folder");
            }
            // Start a new launch in ReportPortal
            launchUuid = startLaunch(
                    launchRq.getName() != null ? launchRq.getName() : "allure-import-" + System.currentTimeMillis(),
                    projectName,
                    launchRq
            );
            // Collections for parsed Allure data
            List<AllureTestResult> results = new ArrayList<>();
            List<AllureContainer> containers = new ArrayList<>();
            Map<String, ZipEntry> attachmentEntries = new HashMap<>();

            // Read all entries in the allure-results directory
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String entryName = entry.getName();
                if (!entryName.startsWith(ALLURE_RESULTS_DIR)) continue; // ignore anything outside allure-results
                String fileName = entryName.substring(ALLURE_RESULTS_DIR.length());
                if (fileName.endsWith("-result.json")) {
                    // Parse test result
                    try (InputStream in = zip.getInputStream(entry)) {
                        AllureTestResult result = objectMapper.readValue(in, AllureTestResult.class);
                        results.add(result);
                    }
                } else if (fileName.endsWith("-container.json")) {
                    // Parse container with fixtures and links
                    try (InputStream in = zip.getInputStream(entry)) {
                        AllureContainer container = objectMapper.readValue(in, AllureContainer.class);
                        containers.add(container);
                    }
                } else if (fileName.contains("-attachment")) {
                    // Defer loading attachments until we attach them
                    attachmentEntries.put(fileName, entry);
                } else {
                    // Other files like executor.json or environment.properties – could handle if needed
                    if (fileName.equals("executor.json")) {
                        // (Optional) use executor info for launch attributes or description
                    }
                }
            }

            // **Aggregate all before/after steps by test UUID**
            Map<String, List<AllureStep>> allBeforeStepsByTest = new HashMap<>();
            Map<String, List<AllureStep>> allAfterStepsByTest = new HashMap<>();
            for (AllureContainer container : containers) {
                if (container.getChildren() != null) {
                    for (String childUuid : container.getChildren()) {
                        if (container.getBefores() != null) {
                            allBeforeStepsByTest.computeIfAbsent(childUuid, k -> new ArrayList<>())
                                    .addAll(container.getBefores());
                        }
                        if (container.getAfters() != null) {
                            allAfterStepsByTest.computeIfAbsent(childUuid, k -> new ArrayList<>())
                                    .addAll(container.getAfters());
                        }
                    }
                }
            }
            // Sort fixture steps by their start time to preserve execution order
            for (List<AllureStep> stepList : allBeforeStepsByTest.values()) {
                stepList.sort((s1, s2) -> {
                    Long t1 = s1.getStart(), t2 = s2.getStart();
                    if (t1 == null && t2 == null) {
                        Long e1 = s1.getStop(), e2 = s2.getStop();
                        if (e1 == null || e2 == null) return 0;
                        return Long.compare(e1, e2);
                    }
                    if (t1 == null) return -1;
                    if (t2 == null) return 1;
                    int cmp = Long.compare(t1, t2);
                    if (cmp != 0) return cmp;
                    // if start times equal or missing, compare stop times
                    Long e1 = s1.getStop(), e2 = s2.getStop();
                    if (e1 == null || e2 == null) return 0;
                    return Long.compare(e1, e2);
                });
            }
            for (List<AllureStep> stepList : allAfterStepsByTest.values()) {
                stepList.sort((s1, s2) -> {
                    Long t1 = s1.getStart(), t2 = s2.getStart();
                    if (t1 == null && t2 == null) {
                        Long e1 = s1.getStop(), e2 = s2.getStop();
                        if (e1 == null || e2 == null) return 0;
                        return Long.compare(e1, e2);
                    }
                    if (t1 == null) return -1;
                    if (t2 == null) return 1;
                    int cmp = Long.compare(t1, t2);
                    if (cmp != 0) return cmp;
                    Long e1 = s1.getStop(), e2 = s2.getStop();
                    if (e1 == null || e2 == null) return 0;
                    return Long.compare(e1, e2);
                });
            }

            // Determine if skipped tests should be marked NOT_ISSUE in RP
            boolean skippedIsNotIssue = launchRq.getAttributes() != null && launchRq.getAttributes().stream()
                    .anyMatch(attr -> "skippedIssue".equalsIgnoreCase(attr.getKey())
                            && "false".equalsIgnoreCase(attr.getValue()));

            // Track overall launch start and end times for timeline
            Instant launchStart = Instant.now();
            long launchEndMillis = 0L;
            long totalDuration = 0L;

            // Group test results by feature (derived from fullName)
            Map<String, List<AllureTestResult>> resultsByFeature = new LinkedHashMap<>();
            for (AllureTestResult result : results) {
                String featureKey;
                if (result.getFullName() != null && !result.getFullName().isEmpty()) {
                    String fullName = result.getFullName();
                    // If fullName contains a scenario separator '#', remove the scenario part
                    int hashIndex = fullName.indexOf('#');
                    String containerPart = hashIndex >= 0 ? fullName.substring(0, hashIndex) : fullName;
                    if (containerPart.contains("/")) {
                        int lastSlash = containerPart.lastIndexOf('/');
                        String fileName = lastSlash >= 0 ? containerPart.substring(lastSlash + 1) : containerPart;
                        int dotIndex = fileName.indexOf('.');
                        featureKey = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
                    } else if (containerPart.contains(".")) {
                        int lastDot = containerPart.lastIndexOf('.');
                        String className = lastDot >= 0 ? containerPart.substring(lastDot + 1) : containerPart;
                        featureKey = className;
                    } else {
                        featureKey = containerPart;
                    }
                } else {
                    featureKey = "<no_feature>";
                }
                resultsByFeature.computeIfAbsent(featureKey, k -> new ArrayList<>()).add(result);
                // update launch start to earliest test start
                if (result.getStart() != null) {
                    Instant testStart = Instant.ofEpochMilli(result.getStart());
                    if (launchStart.isAfter(testStart)) {
                        launchStart = testStart;
                    }
                }
                // accumulate total duration (sum of test durations)
                if (result.getStart() != null && result.getStop() != null) {
                    long duration = result.getStop() - result.getStart();
                    totalDuration += duration;
                }
                // track latest test stop time
                if (result.getStop() != null && result.getStop() > launchEndMillis) {
                    launchEndMillis = result.getStop();
                }
            }
            // If we have a detected overall end, adjust totalDuration to actual span (for overlapping tests)
            if (launchEndMillis != 0L) {
                totalDuration = launchEndMillis - launchStart.toEpochMilli();
            }
            // Publish events for each feature group and its test cases
            for (Map.Entry<String, List<AllureTestResult>> featureEntry : resultsByFeature.entrySet()) {
                String featureName = featureEntry.getKey();
                StartTestItemRQ featureStartRq = new StartTestItemRQ();
                featureStartRq.setName("Feature: " + featureName);
                featureStartRq.setType("TEST");
                featureStartRq.setStartTime(launchStart);
                featureStartRq.setLaunchUuid(launchUuid);
                featureStartRq.setUuid(UUID.randomUUID().toString());
                _eventPublisher.publishEvent(new StartRootItemRqEvent(this, projectName, featureStartRq));
                String featureItemId = featureStartRq.getUuid();
                // Process each test result (test case) under this feature
                for (AllureTestResult result : featureEntry.getValue()) {
                    // Start test item (scenario)
                    StartTestItemRQ testStartRq = new StartTestItemRQ();
                    testStartRq.setName(result.getName());
                    testStartRq.setUuid(UUID.randomUUID().toString());
                    testStartRq.setLaunchUuid(launchUuid);
                    testStartRq.setType("STEP");
                    testStartRq.setStartTime(result.getStart() != null
                            ? Instant.ofEpochMilli(result.getStart())
                            : Instant.now());
                    // Map Allure labels to RP attributes (excluding suite grouping labels)
                    if (result.getLabels() != null) {
                        Set<ItemAttributesRQ> attributes = new HashSet<>();
                        for (Label label : result.getLabels()) {
                            String name = label.getName();
                            String value = label.getValue();
                            if ("parentSuite".equals(name) || "suite".equals(name) || "subSuite".equals(name)) continue;
                            ItemAttributesRQ attr = new ItemAttributesRQ();
                            if ("tag".equals(name) || name == null || name.isEmpty()) {
                                // treat as an unkeyed tag
                                attr.setKey(null);
                                attr.setValue(value);
                            } else {
                                attr.setKey(name);
                                attr.setValue(value);
                            }
                            attributes.add(attr);
                        }
                        testStartRq.setAttributes(attributes);
                    }
                    _eventPublisher.publishEvent(new StartChildItemRqEvent(this, projectName, featureItemId, testStartRq));
                    String scenarioItemUuid = testStartRq.getUuid();

                    // Retrieve all fixture steps for this test
                    List<AllureStep> beforeSteps = allBeforeStepsByTest.getOrDefault(result.getUuid(), Collections.emptyList());
                    List<AllureStep> afterSteps = allAfterStepsByTest.getOrDefault(result.getUuid(), Collections.emptyList());

                    // Process Before steps
                    for (AllureStep step : beforeSteps) {
                        processStep(step, scenarioItemUuid, projectName, skippedIsNotIssue,
                                /* isBefore = */ true, /* isAfter = */ false,
                                launchUuid, zip, attachmentEntries);
                    }
                    // Process test's own steps
                    if (result.getSteps() != null) {
                        for (AllureStep step : result.getSteps()) {
                            processStep(step, scenarioItemUuid, projectName, skippedIsNotIssue,
                                    /* isBefore = */ false, /* isAfter = */ false,
                                    launchUuid, zip, attachmentEntries);
                        }
                    }
                    // Process After steps
                    for (AllureStep step : afterSteps) {
                        processStep(step, scenarioItemUuid, projectName, skippedIsNotIssue,
                                /* isBefore = */ false, /* isAfter = */ true,
                                launchUuid, zip, attachmentEntries);
                    }

                    // Attach any test-level attachments outside of steps
                    if (result.getAttachments() != null) {
                        for (Attachment att : result.getAttachments()) {
                            ZipEntry attEntry = attachmentEntries.get(att.getSource());
                            if (attEntry != null) {
                                try (InputStream attIn = zip.getInputStream(attEntry)) {
                                    byte[] content = attIn.readAllBytes();
                                    SaveLogRQ logRq = new SaveLogRQ();
                                    logRq.setItemUuid(scenarioItemUuid);
                                    logRq.setLevel("INFO");
                                    logRq.setLogTime(result.getStop() != null
                                            ? Instant.ofEpochMilli(result.getStop())
                                            : Instant.now());
                                    logRq.setMessage(att.getName() != null ? att.getName() : "Attachment");
                                    MultipartFile multipartFile = new InMemoryMultipartFile(
                                            att.getName() != null ? att.getName() : att.getSource(),
                                            att.getSource(),
                                            att.getType(),
                                            content
                                    );
                                    _eventPublisher.publishEvent(new SaveLogRqEvent(this, projectName, logRq, multipartFile));
                                } catch (IOException e) {
                                    e.printStackTrace(); // log read error
                                }
                            }
                        }
                    }

                    // Log errors at step level only to avoid duplication
                    boolean anyStepFailed = false;
                    // Check if any before step failed
                    for (AllureStep step : beforeSteps) {
                        if (hasFailedStep(step)) {
                            anyStepFailed = true;
                            break;
                        }
                    }
                    // Check if any test step failed
                    if (!anyStepFailed && result.getSteps() != null) {
                        for (AllureStep step : result.getSteps()) {
                            if (hasFailedStep(step)) {
                                anyStepFailed = true;
                                break;
                            }
                        }
                    }
                    // Check if any after step failed
                    if (!anyStepFailed && afterSteps != null) {
                        for (AllureStep step : afterSteps) {
                            if (hasFailedStep(step)) {
                                anyStepFailed = true;
                                break;
                            }
                        }
                    }
                    if (!anyStepFailed && result.getStatusDetails() != null && result.getStatusDetails().getMessage() != null
                            && ("failed".equalsIgnoreCase(result.getStatus()) || "broken".equalsIgnoreCase(result.getStatus()))) {
                        // If the test itself has an error (and none of the steps already handled it), log it at test level
                        SaveLogRQ errorLog = new SaveLogRQ();
                        errorLog.setItemUuid(scenarioItemUuid);
                        errorLog.setLevel("ERROR");
                        Instant errorTime = (result.getStop() != null
                                ? Instant.ofEpochMilli(result.getStop())
                                : Instant.now());
                        errorLog.setLogTime(errorTime);
                        errorLog.setMessage(result.getStatusDetails().getMessage());
                        _eventPublisher.publishEvent(new SaveLogRqEvent(this, projectName, errorLog, null));
                        if (result.getStatusDetails().getTrace() != null) {
                            SaveLogRQ traceLog = new SaveLogRQ();
                            traceLog.setItemUuid(scenarioItemUuid);
                            traceLog.setLevel("ERROR");
                            traceLog.setLogTime(errorTime);
                            traceLog.setMessage(result.getStatusDetails().getTrace());
                            _eventPublisher.publishEvent(new SaveLogRqEvent(this, projectName, traceLog, null));
                        }
                    }

                    // Finish the scenario item
                    FinishTestItemRQ finishTestRq = new FinishTestItemRQ();
                    finishTestRq.setLaunchUuid(launchUuid);
                    finishTestRq.setEndTime(result.getStop() != null
                            ? Instant.ofEpochMilli(result.getStop())
                            : Instant.now());
                    finishTestRq.setStatus(mapStatus(result.getStatus()));
                    if ("SKIPPED".equals(finishTestRq.getStatus()) && skippedIsNotIssue) {
                        Issue issue = new Issue();
                        issue.setIssueType("NOT_ISSUE");
                        finishTestRq.setIssue(issue);
                    }
                    _eventPublisher.publishEvent(new FinishItemRqEvent(this, projectName, scenarioItemUuid, finishTestRq));
                }
                // Finish the feature item
                long featureEndMillis = 0L;
                for (AllureTestResult res : featureEntry.getValue()) {
                    if (res.getStop() != null && res.getStop() > featureEndMillis) {
                        featureEndMillis = res.getStop();
                    }
                }
                Instant featureEndTime = (featureEndMillis != 0L ? Instant.ofEpochMilli(featureEndMillis) : Instant.now());
                FinishTestItemRQ finishFeatureRq = new FinishTestItemRQ();
                finishFeatureRq.setLaunchUuid(launchUuid);
                finishFeatureRq.setEndTime(featureEndTime);
                _eventPublisher.publishEvent(new FinishItemRqEvent(this, projectName, featureItemId, finishFeatureRq));
            }

            // Finish the launch with corrected timing
            ParseResults parseResults = new ParseResults(launchStart, totalDuration);
            finishLaunch(launchUuid, projectName, parseResults);
            updateStartTime(launchUuid, parseResults.getStartTime());
            return launchUuid;
        } catch (IOException e) {
            // Abort launch if any error during processing
            if (launchUuid != null) {
                abortLaunch(launchUuid, projectName);
            }
            throw new ReportPortalException("Failed to import Allure results: " + e.getMessage(), e);
        }
    }

    private void processStep(AllureStep step,
                             String parentItemId,
                             String projectName,
                             boolean skippedIsNotIssue,
                             boolean isBefore,
                             boolean isAfter,
                             String launchUuid,
                             ZipFile zip,
                             Map<String, ZipEntry> attachmentEntries) {
        // Start step item
        StartTestItemRQ stepStartRq = new StartTestItemRQ();
        String prefix = isBefore ? "Before: " : (isAfter ? "After: " : "");
        String stepName = (step.getName() != null && !step.getName().isEmpty()) ? step.getName() : "unknown";
        stepStartRq.setName(prefix + stepName);
        stepStartRq.setType("STEP");
        if (isBefore) {
            stepStartRq.setType("BEFORE_TEST");

        }
        if (isAfter) {
            stepStartRq.setType("AFTER_TEST");
            stepStartRq.setHasStats(true);
        }
        stepStartRq.setHasStats(false); // required!
        stepStartRq.setUuid(UUID.randomUUID().toString());
        stepStartRq.setStartTime(step.getStart() != null
                ? Instant.ofEpochMilli(step.getStart())
                : Instant.now());
        stepStartRq.setLaunchUuid(launchUuid);

        _eventPublisher.publishEvent(new StartChildItemRqEvent(this, projectName, parentItemId, stepStartRq));
        String stepItemUuid = stepStartRq.getUuid();

        // Recursively handle substeps
        if (step.getSteps() != null) {
            for (AllureStep subStep : step.getSteps()) {
                processStep(subStep, stepItemUuid, projectName, skippedIsNotIssue,
                        false, false, launchUuid, zip, attachmentEntries);
            }
        }

        // Handle attachments
        if (step.getAttachments() != null) {
            for (Attachment att : step.getAttachments()) {
                ZipEntry attEntry = attachmentEntries.get(att.getSource());
                if (attEntry != null) {
                    try (InputStream attIn = zip.getInputStream(attEntry)) {
                        byte[] content = attIn.readAllBytes();
                        SaveLogRQ logRq = new SaveLogRQ();
                        logRq.setItemUuid(stepItemUuid);
                        logRq.setLevel("INFO");
                        logRq.setLogTime(step.getStop() != null
                                ? Instant.ofEpochMilli(step.getStop())
                                : Instant.now());
                        logRq.setMessage(att.getName() != null ? att.getName() : "Attachment");
                        MultipartFile multipartFile = new InMemoryMultipartFile(
                                att.getName() != null ? att.getName() : att.getSource(),
                                att.getSource(),
                                att.getType(),
                                content
                        );
                        _eventPublisher.publishEvent(new SaveLogRqEvent(this, projectName, logRq, multipartFile));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Log error message if the step failed
        if (step.getStatusDetails() != null && step.getStatusDetails().getMessage() != null
                && ("failed".equalsIgnoreCase(step.getStatus()) || "broken".equalsIgnoreCase(step.getStatus()))) {
            SaveLogRQ errorLog = new SaveLogRQ();
            errorLog.setItemUuid(stepItemUuid);
            errorLog.setLevel("ERROR");
            errorLog.setLogTime(step.getStop() != null
                    ? Instant.ofEpochMilli(step.getStop())
                    : Instant.now());
            errorLog.setMessage(step.getStatusDetails().getMessage());
            _eventPublisher.publishEvent(new SaveLogRqEvent(this, projectName, errorLog, null));
            if (step.getStatusDetails().getTrace() != null) {
                SaveLogRQ traceLog = new SaveLogRQ();
                traceLog.setItemUuid(stepItemUuid);
                traceLog.setLevel("ERROR");
                traceLog.setLogTime(step.getStop() != null
                        ? Instant.ofEpochMilli(step.getStop())
                        : Instant.now());
                traceLog.setMessage(step.getStatusDetails().getTrace());
                _eventPublisher.publishEvent(new SaveLogRqEvent(this, projectName, traceLog, null));
            }
        }

        // Finish step
        FinishTestItemRQ finishStepRq = new FinishTestItemRQ();
        finishStepRq.setLaunchUuid(launchUuid);
        finishStepRq.setEndTime(step.getStop() != null
                ? Instant.ofEpochMilli(step.getStop())
                : Instant.now());
        finishStepRq.setStatus(mapStatus(step.getStatus()));
        if ("SKIPPED".equals(finishStepRq.getStatus()) && skippedIsNotIssue) {
            Issue issue = new Issue();
            issue.setIssueType("NOT_ISSUE");
            finishStepRq.setIssue(issue);
        }
        _eventPublisher.publishEvent(new FinishItemRqEvent(this, projectName, stepItemUuid, finishStepRq));
    }


    private void abortLaunch(String launchUuid, String projectName) {
        FinishExecutionRQ finishLaunchRq = new FinishExecutionRQ();
        finishLaunchRq.setEndTime(Instant.now());
        finishLaunchRq.setStatus("INTERRUPTED");
        _eventPublisher.publishEvent(new FinishLaunchRqEvent(this, projectName, launchUuid, finishLaunchRq));
    }

    // Map Allure status to ReportPortal status
    private String mapStatus(String allureStatus) {
        if (allureStatus == null) return "PASSED";
        switch (allureStatus.toLowerCase()) {
            case "passed":
                return "PASSED";
            case "failed":
                return "FAILED";
            case "broken":
                return "FAILED";
            case "skipped":
                return "SKIPPED";
            case "unknown":
                return "SKIPPED";
            default:
                return "SKIPPED";
        }
    }

    private boolean hasFailedStep(AllureStep step) {
        if (step.getStatus() != null && ("failed".equalsIgnoreCase(step.getStatus())
                || "broken".equalsIgnoreCase(step.getStatus()))) {
            return true;
        }
        if (step.getSteps() != null) {
            for (AllureStep sub : step.getSteps()) {
                if (hasFailedStep(sub)) {
                    return true;
                }
            }
        }
        return false;
    }
}

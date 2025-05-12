package com.epam.reportportal.extension.allure.service;

import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Optional;

import static com.epam.reportportal.rules.exception.ErrorType.BAD_REQUEST_ERROR;

public class ImportStrategyFactory {
    private final Map<String, ImportStrategy> STRATEGY_MAPPING;

    public ImportStrategyFactory(ApplicationEventPublisher eventPublisher, LaunchRepository launchRepo) {
        STRATEGY_MAPPING = ImmutableMap.<String, ImportStrategy>builder()
                .put(FileExtensionConstant.ZIP_EXTENSION, new AllureZipImportStrategy(eventPublisher, launchRepo))
                .build();
    }

    public ImportStrategy getImportStrategy(String filename) {
        String ext = FilenameUtils.getExtension(filename);
        return Optional.ofNullable(STRATEGY_MAPPING.get(ext))
                .orElseThrow(() -> new ReportPortalException(BAD_REQUEST_ERROR, "Incorrect file extension."));
    }
}

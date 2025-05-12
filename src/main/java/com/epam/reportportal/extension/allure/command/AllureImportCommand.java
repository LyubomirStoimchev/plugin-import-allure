package com.epam.reportportal.extension.allure.command;

import com.epam.reportportal.extension.CommonPluginCommand;
import com.epam.reportportal.extension.allure.model.LaunchImportRQ;
import com.epam.reportportal.extension.allure.service.ImportStrategy;
import com.epam.reportportal.extension.allure.service.ImportStrategyFactory;
import com.epam.reportportal.extension.util.RequestEntityConverter;
import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.epam.ta.reportportal.ws.reporting.StartLaunchRS;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.epam.reportportal.extension.allure.service.FileExtensionConstant.ZIP_EXTENSION;
import static com.epam.reportportal.extension.util.CommandParamUtils.ENTITY_PARAM;
import static com.epam.reportportal.rules.commons.validation.BusinessRule.expect;
import static com.epam.reportportal.rules.exception.ErrorType.BAD_REQUEST_ERROR;
import static com.epam.reportportal.rules.exception.ErrorType.INCORRECT_REQUEST;
import static org.apache.commons.io.FileUtils.ONE_MB;

/**
 * Allure Import Command - based on XUnitImportCommand, modified for Allure ZIP support only
 */
public class AllureImportCommand implements CommonPluginCommand<StartLaunchRS> {

    // Max upload size is 1024MB, because it can include attachments
    public static final long MAX_FILE_SIZE = 1024 * ONE_MB;
    private static final String FILE_PARAM = "file";
    private static final String PROJECT_NAME = "projectName";

    private final RequestEntityConverter requestEntityConverter;
    private final ImportStrategyFactory importStrategyFactory;
    private final LaunchRepository launchRepository;

    public AllureImportCommand(RequestEntityConverter requestEntityConverter,
                               ApplicationEventPublisher eventPublisher,
                               LaunchRepository launchRepository) {
        this.requestEntityConverter = requestEntityConverter;
        this.launchRepository = launchRepository;
        this.importStrategyFactory = new ImportStrategyFactory(eventPublisher, launchRepository);
    }

    @Override
    public StartLaunchRS executeCommand(Map<String, Object> params) {
        LaunchImportRQ launchImportRQ = Optional.ofNullable(params.get(ENTITY_PARAM))
                .map(it -> requestEntityConverter.getEntity(ENTITY_PARAM, params, LaunchImportRQ.class))
                .orElseGet(LaunchImportRQ::new);

        MultipartFile file = (MultipartFile) Optional.ofNullable(params.get(FILE_PARAM))
                .orElseThrow(() -> new ReportPortalException(
                        BAD_REQUEST_ERROR, "File for import wasn't provided"));

        validate(file);

        ImportStrategy importStrategy = importStrategyFactory.getImportStrategy(file.getOriginalFilename());

        String projectName = Optional.ofNullable(params.get(PROJECT_NAME))
                .map(String::valueOf)
                .orElseThrow(() ->
                        new ReportPortalException(BAD_REQUEST_ERROR, "Project name wasn't provided"));

        String launchUuid = importStrategy.importLaunch(file, projectName, launchImportRQ);
        return prepareLaunchImportResponse(launchUuid);
    }

    @Override
    public String getName() {
        return "import";
    }

    private void validate(MultipartFile file) {
        expect(file.getOriginalFilename(), Objects::nonNull).verify(INCORRECT_REQUEST,
                "File name should be not empty."
        );
        expect(file.getOriginalFilename(), it -> it.endsWith(ZIP_EXTENSION))
                .verify(INCORRECT_REQUEST,
                        "Only ZIP archives are supported for Allure import: " + file.getOriginalFilename());
        expect(file.getSize(), size -> size <= MAX_FILE_SIZE).verify(INCORRECT_REQUEST,
                "File size is more than 1024 Mb."
        );
    }

    private StartLaunchRS prepareLaunchImportResponse(String uuid) {
        var data = new StartLaunchRS();
        data.setId(uuid);
        return data;
    }
}

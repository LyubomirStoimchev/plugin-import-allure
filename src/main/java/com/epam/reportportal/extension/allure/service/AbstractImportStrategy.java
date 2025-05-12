/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.extension.allure.service;

import com.epam.reportportal.events.FinishLaunchRqEvent;
import com.epam.reportportal.events.StartLaunchRqEvent;
import com.epam.reportportal.extension.allure.model.LaunchImportRQ;
import com.epam.reportportal.rules.exception.ErrorType;
import com.epam.reportportal.rules.exception.ReportPortalException;
import com.epam.ta.reportportal.dao.LaunchRepository;
import com.epam.ta.reportportal.entity.launch.Launch;
import com.epam.ta.reportportal.ws.reporting.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.reporting.Mode;
import com.epam.ta.reportportal.ws.reporting.StartLaunchRQ;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

import static java.util.Optional.ofNullable;

/**
 * Abstract base class for implementing different import strategies.
 * Provides shared methods and utilities to manage the lifecycle of launch imports,
 * including starting launches, finishing launches, and updating launch start times.
 * This class serves as a foundational layer for custom import strategies,
 * allowing them to focus on implementing specific behaviors while leveraging
 * common launch-related functionalities.
 */
public abstract class AbstractImportStrategy implements ImportStrategy {

    private final ApplicationEventPublisher eventPublisher;

    private final LaunchRepository launchRepository;


    public AbstractImportStrategy(ApplicationEventPublisher eventPublisher,
                                  LaunchRepository launchRepository) {
        this.eventPublisher = eventPublisher;
        this.launchRepository = launchRepository;
    }

    protected String startLaunch(String launchName, String projectName, LaunchImportRQ rq) {
        String launchUuid = UUID.randomUUID().toString();
        StartLaunchRQ startLaunchRQ = new StartLaunchRQ();
        startLaunchRQ.setUuid(launchUuid);
        startLaunchRQ.setStartTime(ofNullable(rq.getStartTime()).orElse(Instant.EPOCH.minusSeconds(0)));
        startLaunchRQ.setName(ofNullable(rq.getName()).orElse(launchName));
        ofNullable(rq.getDescription()).ifPresent(startLaunchRQ::setDescription);
        startLaunchRQ.setMode(ofNullable(rq.getMode()).orElse(Mode.DEFAULT));
        startLaunchRQ.setAttributes(ofNullable(rq.getAttributes()).orElse(new HashSet<>()));
        eventPublisher.publishEvent(new StartLaunchRqEvent(this, projectName, startLaunchRQ));
        return launchUuid;
    }

    protected void finishLaunch(String launchUuid, String projectName, ParseResults results) {
        FinishExecutionRQ finishExecutionRQ = new FinishExecutionRQ();
        finishExecutionRQ.setEndTime(results.getEndTime());
        eventPublisher.publishEvent(
                new FinishLaunchRqEvent(this, projectName, launchUuid, finishExecutionRQ));
    }

    protected void updateStartTime(String launchUuid, Instant startTime) {
        Launch launch = launchRepository.findByUuid(launchUuid)
                .orElseThrow(() -> new ReportPortalException(ErrorType.LAUNCH_NOT_FOUND, launchUuid));
        launch.setStartTime(startTime);
        launchRepository.save(launch);
    }
}

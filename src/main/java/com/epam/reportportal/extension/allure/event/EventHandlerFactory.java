package com.epam.reportportal.extension.allure.event;

import com.epam.reportportal.extension.allure.event.handler.EventHandler;

public interface EventHandlerFactory<T> {

	EventHandler<T> getEventHandler(String key);
}

package com.epam.reportportal.extension.allure.event.handler;

public interface EventHandler<T> {

	void handle(T event);
}

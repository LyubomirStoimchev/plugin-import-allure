package com.epam.reportportal.extension.allure.event.plugin;

import com.epam.reportportal.extension.event.PluginEvent;
import com.epam.reportportal.extension.allure.event.EventHandlerFactory;
import com.epam.reportportal.extension.allure.event.handler.EventHandler;
import com.epam.reportportal.extension.allure.event.handler.plugin.PluginLoadedEventHandler;
import com.epam.ta.reportportal.dao.IntegrationRepository;
import com.epam.ta.reportportal.dao.IntegrationTypeRepository;
import java.util.HashMap;
import java.util.Map;

public class PluginEventHandlerFactory implements EventHandlerFactory<PluginEvent> {

	public static final String LOAD_KEY = "load";

	private final Map<String, EventHandler<PluginEvent>> eventHandlerMapping;

	public PluginEventHandlerFactory(String resourcesDir, IntegrationTypeRepository integrationTypeRepository,
			IntegrationRepository integrationRepository) {
		this.eventHandlerMapping = new HashMap<>();
		this.eventHandlerMapping.put(LOAD_KEY,
				new PluginLoadedEventHandler(resourcesDir, integrationTypeRepository, integrationRepository)
		);
	}

	@Override
	public EventHandler<PluginEvent> getEventHandler(String key) {
		return eventHandlerMapping.get(key);
	}
}

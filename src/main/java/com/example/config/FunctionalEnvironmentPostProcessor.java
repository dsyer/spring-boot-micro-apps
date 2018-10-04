/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * @author Dave Syer
 *
 */
public class FunctionalEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@SuppressWarnings("unchecked")
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		if (environment.getProperty("spring.functional.enabled")==null) {
			Map<String, Object> map;
			if (!environment.getPropertySources().contains("defaultProperties")) {
				map = new HashMap<>();
				environment.getPropertySources().addLast(new MapPropertySource("defaultProperties", map));
			} else {
				// Make sure the map is mutable...
				map = new HashMap<>((Map<String, Object>) environment.getPropertySources().get("defaultProperties").getSource());
				environment.getPropertySources().replace("defaultProperties", new MapPropertySource("defaultProperties", map));
			}
			map = (Map<String, Object>) environment.getPropertySources().get("defaultProperties").getSource();
			map.put("spring.functional.enabled", "true");
		}
	}

}

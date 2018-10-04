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

package com.example.func;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Dave Syer
 *
 */
public class SpringApplication extends org.springframework.boot.SpringApplication {

	public static ConfigurableApplicationContext run(Class<?> primarySource,
			String... args) {
		return run(new Class<?>[] {}, args);
	}

	public static ConfigurableApplicationContext run(Class<?>[] primarySources,
			String[] args) {
		return new SpringApplication().run(args);
	}

	public SpringApplication() {
		super(SpringApplication.class);
		// In a native image we are forced to have Tomcat on the "classpath"
		setWebApplicationType(WebApplicationType.REACTIVE);
	}
	
	@Override
	public ConfigurableApplicationContext run(String... args) {
		System.err.println("WebApplicationType: " + getWebApplicationType());
		if (getWebApplicationType() == WebApplicationType.REACTIVE) {
			setApplicationContextClass(ReactiveWebServerApplicationContext.class);
		}
		return super.run(args);
	}

	@Override
	protected void load(ApplicationContext context, Object[] sources) {
	}
}

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

import java.util.function.Function;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionType;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author Dave Syer
 *
 */
public class SpringApplication extends org.springframework.boot.SpringApplication {

	public static ConfigurableApplicationContext run(Class<?> primarySource,
			String... args) {
		return run(new Class<?>[] { primarySource }, args);
	}

	public static ConfigurableApplicationContext run(Class<?>[] primarySources,
			String[] args) {
		return new SpringApplication(primarySources).run(args);
	}

	public SpringApplication(Class<?>... primarySources) {
		super(primarySources);
		// In a native image we are forced to have Tomcat on the "classpath"
		setWebApplicationType(WebApplicationType.REACTIVE);
	}

	@Override
	public ConfigurableApplicationContext run(String... args) {
		System.err.println("WebApplicationType: " + getWebApplicationType());
		if (getWebApplicationType() == WebApplicationType.REACTIVE) {
			setApplicationContextClass(ReactiveWebServerApplicationContext.class);
		}
		try {
			return super.run(args);
		}
		catch (Throwable t) {
			IllegalStateException e = new IllegalStateException(t);
			System.err.println(e.getClass() + ": " + e);
			e.printStackTrace();
			throw new IllegalStateException("Failed to run");
		}
	}

	@Override
	protected void load(ApplicationContext context, Object[] sources) {
		GenericApplicationContext generic = (GenericApplicationContext) context;
		for (Object source : sources) {
			if (source instanceof Class<?>) {
				Class<?> type = (Class<?>) source;
				if (ApplicationContextInitializer.class.isAssignableFrom(type)) {
					@SuppressWarnings("unchecked")
					ApplicationContextInitializer<GenericApplicationContext> initializer = BeanUtils
							.instantiateClass(type, ApplicationContextInitializer.class);
					initializer.initialize(generic);
				}
				else if (Function.class.isAssignableFrom(type)) {
					Function<?, ?> function = BeanUtils.instantiateClass(type,
							Function.class);
					generic.registerBean("function", FunctionRegistration.class,
							() -> new FunctionRegistration<>(function)
									.type(FunctionType.of(type)));

				}
			}
		}
	}
}

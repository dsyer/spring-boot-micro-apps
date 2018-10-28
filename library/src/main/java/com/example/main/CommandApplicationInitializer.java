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

package com.example.main;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import org.springframework.boot.Banner.Mode;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.FunctionalSpringApplication;
import org.springframework.cloud.function.context.catalog.FunctionInspector;
import org.springframework.cloud.function.json.JsonMapper;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StreamUtils;

import reactor.core.publisher.Flux;

/**
 * @author Dave Syer
 *
 */
public class CommandApplicationInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	public static void main(String[] args) throws Exception {
		FunctionalSpringApplication application = new FunctionalSpringApplication(CommandApplicationInitializer.class);
		application.setWebApplicationType(WebApplicationType.NONE);
		application.setBannerMode(Mode.OFF);
		application.setDefaultProperties(Collections.singletonMap("logging.level.root", "off"));
		application.run(args);
	}

	@Override
	public void initialize(GenericApplicationContext context) {
		context.registerBean("command", CommandApplicationRunner.class,
				() -> new CommandApplicationRunner(context.getEnvironment(),
						context.getBean(FunctionInspector.class),
						context.getBean(FunctionCatalog.class),
						context.getBean(JsonMapper.class)));
	}

}

class CommandApplicationRunner implements CommandLineRunner {

	private Function<Publisher<?>, Publisher<?>> function;

	private FunctionInspector inspector;

	private FunctionCatalog catalog;

	private JsonMapper mapper;

	private Environment environment;

	public CommandApplicationRunner(Environment environment, FunctionInspector inspector,
			FunctionCatalog catalog, JsonMapper mapper) {
		this.environment = environment;
		this.inspector = inspector;
		this.catalog = catalog;
		this.mapper = mapper;
	}

	@Override
	public void run(String... args) throws Exception {
		initialize();
		Class<?> inputType = this.inspector.getInputType(this.function);
		String body = StreamUtils.copyToString(System.in, Charset.defaultCharset());
		Flux<?> result = null;
		if (String.class.isAssignableFrom(inputType)) {
			result = Flux.from(this.function.apply(Flux.just(body)));
		}
		else {
			result = Flux
					.from(this.function
							.apply(Flux.just(mapper.toObject(body, inputType))))
					.map(value -> mapper.toString(value));
		}
		result.subscribe(System.out::print);
	}

	protected void initialize() {
		String name = environment.getProperty("function.name");
		if (name == null) {
			name = "function";
		}
		Set<String> functionNames = this.catalog.getNames(Function.class);
		if (functionNames.size() == 1) {
			this.function = this.catalog.lookup(Function.class,
					functionNames.iterator().next());
		}
		else {
			this.function = this.catalog.lookup(Function.class, name);
		}
	}

}

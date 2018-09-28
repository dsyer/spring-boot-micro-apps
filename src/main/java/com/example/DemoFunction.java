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

package com.example;

import java.util.function.Function;

import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionType;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author Dave Syer
 *
 */
public class DemoFunction implements Function<String, String>,
		ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public String apply(String value) {
		return value.toUpperCase();
	}

	@Override
	public void initialize(GenericApplicationContext context) {
		context.registerBean("demo", FunctionRegistration.class,
				() -> new FunctionRegistration<DemoFunction>(this, "demo")
						.type(FunctionType.from(String.class).to(String.class)));
	}

}

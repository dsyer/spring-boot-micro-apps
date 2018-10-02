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

package com.example.app;

import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.catalog.FunctionInspector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import reactor.core.publisher.Flux;

/**
 * @author Dave Syer
 *
 */
@Configuration
public class Endpoint {

	private static Log logger = LogFactory.getLog(Endpoint.class);

	private Function<Flux<?>, Flux<?>> function;

	private FunctionInspector inspector;

	public Endpoint(FunctionCatalog catalog, FunctionInspector inspector) {
		this.inspector = inspector;
		this.function = extract(catalog);
	}

	private Function<Flux<?>, Flux<?>> extract(FunctionCatalog catalog) {
		Set<String> names = catalog.getNames(Function.class);
		if (!names.isEmpty()) {
			// TODO: function.name configuration
			logger.info("Found functions: " + names);
			return catalog.lookup(Function.class, names.iterator().next());
		}
		throw new IllegalStateException("No function defined");
	}

	@SuppressWarnings({ "unchecked" })
	@Bean
	public <T> RouterFunction<?> userEndpoints() {
		return route(POST("/"), request -> {
			Class<?> inputType = this.inspector.getInputType(this.function);
			Class<T> outputType = (Class<T>) this.inspector.getOutputType(this.function);
			return ok().body((Flux<T>) this.function.apply(request.bodyToFlux(inputType)).log().onErrorContinue((e,o) -> {
				e.printStackTrace();
				System.err.println("########" + o);
			}),
					outputType);
		});
	}

}

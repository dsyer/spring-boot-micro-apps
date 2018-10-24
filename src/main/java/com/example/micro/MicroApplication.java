/*
 * Copyright 2016-2017 the original author or authors.
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
package com.example.micro;

import com.example.config.ApplicationBuilder;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.server.WebExceptionHandler;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import reactor.core.publisher.Mono;

/**
 * @author Dave Syer
 *
 */
public class MicroApplication {

	public static void main(String[] args) throws Exception {
		long t0 = System.currentTimeMillis();
		GenericApplicationContext context = new MicroApplication().run();
		ApplicationBuilder.start(context, b -> {
			System.err.println(
					"Started HttpServer: " + (System.currentTimeMillis() - t0) + "ms");
		});
	}

	public GenericApplicationContext run() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean(RouterFunction.class,
				() -> RouterFunctions
						.route(GET("/"),
								request -> ok().body(Mono.just("Hello"), String.class))
						.andRoute(POST("/"),
								request -> ok().body(
										request.bodyToMono(String.class)
												.map(value -> value.toUpperCase()),
										String.class)));
		context.registerBean(DefaultErrorWebExceptionHandler.class,
				() -> errorHandler(context));
		context.registerBean(HttpHandler.class, () -> httpHandler(context));
		context.refresh();
		return context;
	}

	private HttpHandler httpHandler(GenericApplicationContext context) {
		return RouterFunctions.toHttpHandler(context.getBean(RouterFunction.class),
				HandlerStrategies.empty()
						.exceptionHandler(context.getBean(WebExceptionHandler.class))
						.codecs(config -> config.registerDefaults(true)).build());
	}

	private DefaultErrorWebExceptionHandler errorHandler(
			GenericApplicationContext context) {
		context.registerBean(ErrorAttributes.class, () -> new DefaultErrorAttributes());
		context.registerBean(ErrorProperties.class, () -> new ErrorProperties());
		context.registerBean(ResourceProperties.class, () -> new ResourceProperties());
		DefaultErrorWebExceptionHandler handler = new DefaultErrorWebExceptionHandler(
				context.getBean(ErrorAttributes.class),
				context.getBean(ResourceProperties.class),
				context.getBean(ErrorProperties.class), context);
		ServerCodecConfigurer codecs = ServerCodecConfigurer.create();
		handler.setMessageWriters(codecs.getWriters());
		handler.setMessageReaders(codecs.getReaders());
		return handler;
	}
}
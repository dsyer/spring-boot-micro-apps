package com.example.func;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.catalog.FunctionInspector;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.ClassUtils;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

public class FunctionEndpointInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	@Override
	public void initialize(GenericApplicationContext context) {
		registerEndpoint(context);
		registerWebFluxAutoConfiguration(context);
	}

	private void registerWebFluxAutoConfiguration(GenericApplicationContext context) {
		context.registerBean("webHandler", WebHandler.class, () -> RouterFunctions
				.toWebHandler(context.getBean(RouterFunction.class)));
		context.addApplicationListener(new ServerListener(context));
	}

	private void registerEndpoint(GenericApplicationContext context) {
		context.registerBean(FunctionEndpointFactory.class,
				() -> new FunctionEndpointFactory(context.getBean(FunctionCatalog.class),
						context.getBean(FunctionInspector.class),
						context.getEnvironment()));
		context.registerBean(RouterFunction.class,
				() -> context.getBean(FunctionEndpointFactory.class).functionEndpoints());
	}

	private static class ServerListener implements SmartApplicationListener {

		private static Log logger = LogFactory.getLog(ServerListener.class);

		private GenericApplicationContext context;

		public ServerListener(GenericApplicationContext context) {
			this.context = context;
		}

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			ApplicationContext context = ((ContextRefreshedEvent) event)
					.getApplicationContext();
			if (context != this.context) {
				return;
			}
			if (!ClassUtils.isPresent(
					"org.springframework.http.server.reactive.HttpHandler", null)) {
				logger.info("No web server classes found so no server to start");
				return;
			}
			Integer port = Integer.valueOf(context.getEnvironment()
					.resolvePlaceholders("${server.port:${PORT:8080}}"));
			if (port >= 0) {
				HttpHandler handler = WebHttpHandlerBuilder.applicationContext(context)
						.build();
				ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(
						handler);
				HttpServer httpServer = HttpServer.create().host("localhost").port(port)
						.handle(adapter);
				Thread thread = new Thread(() -> httpServer
						.bindUntilJavaShutdown(Duration.ofSeconds(60), this::callback),
						"server-startup");
				thread.setDaemon(false);
				thread.start();
			}
		}

		private void callback(DisposableServer server) {
			logger.info("Server started");
			try {
				double uptime = ManagementFactory.getRuntimeMXBean().getUptime();
				System.err.println("JVM running for " + uptime + "ms");
			}
			catch (Throwable e) {
			}
		}

		@Override
		public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
			return eventType.isAssignableFrom(ContextRefreshedEvent.class);
		}

	}

}

class FunctionEndpointFactory {

	private static Log logger = LogFactory.getLog(FunctionEndpointFactory.class);

	private Function<Flux<?>, Flux<?>> function;

	private FunctionInspector inspector;

	public FunctionEndpointFactory(FunctionCatalog catalog, FunctionInspector inspector,
			Environment environment) {
		String handler = environment.resolvePlaceholders("${function.handler}");
		if (handler.startsWith("$")) {
			handler = null;
		}
		this.inspector = inspector;
		this.function = extract(catalog, handler);
	}

	private Function<Flux<?>, Flux<?>> extract(FunctionCatalog catalog, String handler) {
		Set<String> names = catalog.getNames(Function.class);
		if (!names.isEmpty()) {
			logger.info("Found functions: " + names);
			if (handler != null) {
				logger.info("Configured function: " + handler);
				if (!names.contains(handler)) {
					throw new IllegalStateException("Cannot locate function: " + handler);
				}
				return catalog.lookup(Function.class, handler);
			}
			return catalog.lookup(Function.class, names.iterator().next());
		}
		throw new IllegalStateException("No function defined");
	}

	@SuppressWarnings({ "unchecked" })
	public <T> RouterFunction<?> functionEndpoints() {
		return route(POST("/"), request -> {
			Class<?> inputType = this.inspector.getInputType(this.function);
			Class<T> outputType = (Class<T>) this.inspector.getOutputType(this.function);
			return ok().body(
					Mono.from(
							(Flux<T>) this.function.apply(request.bodyToFlux(inputType))),
					outputType);
		});
	}

}
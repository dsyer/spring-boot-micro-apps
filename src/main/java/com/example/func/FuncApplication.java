package com.example.func;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import com.example.config.BeanCountingApplicationListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.boot.autoconfigure.gson.GsonProperties;
import org.springframework.boot.autoconfigure.http.HttpEncodingProperties;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.reactor.core.ReactorCoreProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.EnableWebFluxConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.WebFluxConfig;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationBeanFactoryMetadata;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.function.server.support.ServerResponseResultHandler;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityResultHandler;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.i18n.LocaleContextResolver;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

public class FuncApplication implements Runnable, Closeable,
		ApplicationContextInitializer<GenericApplicationContext> {

	public static final String MARKER = "Benchmark app started";

	private GenericApplicationContext context;

	@Bean
	public RouterFunction<?> userEndpoints() {
		return route(GET("/"), request -> ok().body(Mono.just("Hello"), String.class));
	}

	public static void main(String[] args) throws Exception {
		long t0 = System.currentTimeMillis();
		FuncApplication bean = new FuncApplication();
		bean.run();
		System.err.println(
				"Started HttpServer: " + (System.currentTimeMillis() - t0) + "ms");
		if (Boolean.getBoolean("demo.close")) {
			bean.close();
		}
	}

	@Override
	public void close() throws IOException {
		if (context != null) {
			context.close();
		}
	}

	@Override
	public void run() {
		ReactiveWebServerApplicationContext context = new ReactiveWebServerApplicationContext();
		// context.registerShutdownHook();
		initialize(context);
		context.refresh();
		System.err.println(MARKER);
		new BeanCountingApplicationListener().log(context);
	}

	@Override
	public void initialize(GenericApplicationContext context) {
		this.context = context;
		((AbstractAutowireCapableBeanFactory) context.getDefaultListableBeanFactory())
				.setParameterNameDiscoverer(new NoopParameterNameDiscoverer());
		if (context.getEnvironment().getProperty("boot.active", Boolean.class, false)) {
			performPreinitialization();
		}
		context.registerBean(AutowiredAnnotationBeanPostProcessor.class);
		registerConverters();
		registerDemoApplication();
		registerWebServerFactoryCustomizerBeanPostProcessor();
		registerConfigurationProperties();
		// context.registerBean(LazyInitBeanFactoryPostProcessor.class);
		registerPropertyPlaceholderAutoConfiguration();
		registerReactiveWebServerFactoryAutoConfiguration();
		registerErrorWebFluxAutoConfiguration();
		registerWebFluxAutoConfiguration();
		registerHttpHandlerAutoConfiguration();
		registerGsonAutoConfiguration();
		registerHttpMessageConvertersAutoConfiguration();
		registerReactorCoreAutoConfiguration();
		registerRestTemplateAutoConfiguration();
		registerWebClientAutoConfiguration();
	}

	private void registerConverters() {
		// Graal needs this?
		context.registerBean(Converter.class, () -> new SerializingConverter());
		context.registerBean(GenericConverter.class, () -> new DummyGenericConverter());
		context.registerBean(WebFilter.class, () -> new DummyWebFilter());
	}

	static class DummyWebFilter implements WebFilter {
		@Override
		public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
			return chain.filter(exchange);
		}
	}

	static class DummyGenericConverter implements GenericConverter {

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.emptySet();
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType,
				TypeDescriptor targetType) {
			return null;
		}

	}

	private void performPreinitialization() {
		try {
			Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {
					runSafely(() -> new DefaultFormattingConversionService());
				}

				public void runSafely(Runnable runnable) {
					try {
						runnable.run();
					}
					catch (Throwable ex) {
						// Ignore
					}
				}

			}, "background-preinit");
			thread.start();
		}
		catch (Exception ex) {
		}
	}

	private void registerConfigurationProperties() {
		context.registerBean(ConfigurationBeanFactoryMetadata.BEAN_NAME,
				ConfigurationBeanFactoryMetadata.class,
				() -> new ConfigurationBeanFactoryMetadata());
		context.registerBean(ConfigurationPropertiesBindingPostProcessor.BEAN_NAME,
				ConfigurationPropertiesBindingPostProcessor.class,
				() -> new ConfigurationPropertiesBindingPostProcessor());
		context.registerBean(ServerProperties.class, () -> new ServerProperties());
		context.registerBean(ResourceProperties.class, () -> new ResourceProperties());
		context.registerBean(WebFluxProperties.class, () -> new WebFluxProperties());
		context.registerBean(GsonProperties.class, () -> new GsonProperties());
		context.registerBean(HttpEncodingProperties.class,
				() -> new HttpEncodingProperties());
		context.registerBean(ReactorCoreProperties.class,
				() -> new ReactorCoreProperties());
	}

	private void registerWebServerFactoryCustomizerBeanPostProcessor() {
		context.registerBean("webServerFactoryCustomizerBeanPostProcessor",
				WebServerFactoryCustomizerBeanPostProcessor.class);
	}

	private void registerPropertyPlaceholderAutoConfiguration() {
		context.registerBean(PropertySourcesPlaceholderConfigurer.class,
				() -> PropertyPlaceholderAutoConfiguration
						.propertySourcesPlaceholderConfigurer());
	}

	private void registerReactiveWebServerFactoryAutoConfiguration() {
		ReactiveWebServerFactoryAutoConfiguration config = new ReactiveWebServerFactoryAutoConfiguration();
		context.registerBean(ReactiveWebServerFactoryCustomizer.class,
				() -> config.reactiveWebServerFactoryCustomizer(
						context.getBean(ServerProperties.class)));
		context.registerBean(JettyReactiveWebServerFactory.class,
				() -> new JettyReactiveWebServerFactory());
	}

	private void registerErrorWebFluxAutoConfiguration() {
		context.registerBean(ErrorAttributes.class, () -> new DefaultErrorAttributes(
				context.getBean(ServerProperties.class).getError().isIncludeException()));
		context.registerBean(ErrorWebFluxAutoConfiguration.class);
		context.registerBean(ErrorWebExceptionHandler.class, () -> {
			return context.getBean(ErrorWebFluxAutoConfiguration.class)
					.errorWebExceptionHandler(context.getBean(ErrorAttributes.class));
		});
	}

	private void registerWebFluxAutoConfiguration() {
		context.registerBean(EnableWebFluxConfigurationWrapper.class,
				() -> new EnableWebFluxConfigurationWrapper(
						context.getBean(WebFluxProperties.class)));
		context.registerBean(HandlerFunctionAdapter.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.handlerFunctionAdapter());
		context.registerBean(WebHttpHandlerBuilder.LOCALE_CONTEXT_RESOLVER_BEAN_NAME,
				LocaleContextResolver.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.localeContextResolver());
		context.registerBean(RequestMappingHandlerAdapter.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.requestMappingHandlerAdapter());
		context.registerBean(RequestMappingHandlerMapping.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.requestMappingHandlerMapping());
		context.registerBean(HandlerMapping.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.resourceHandlerMapping());
		context.registerBean(ResponseBodyResultHandler.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.responseBodyResultHandler());
		context.registerBean(ResponseEntityResultHandler.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.responseEntityResultHandler());
		context.registerBean(WebExceptionHandler.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.responseStatusExceptionHandler());
		context.registerBean(RouterFunctionMapping.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.routerFunctionMapping());
		context.registerBean(WebHttpHandlerBuilder.SERVER_CODEC_CONFIGURER_BEAN_NAME,
				ServerCodecConfigurer.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.serverCodecConfigurer());
		context.registerBean(ServerResponseResultHandler.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.serverResponseResultHandler());
		context.registerBean(SimpleHandlerAdapter.class, () -> context
				.getBean(EnableWebFluxConfigurationWrapper.class).simpleHandlerAdapter());
		context.registerBean(ViewResolutionResultHandler.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.viewResolutionResultHandler());
		context.registerBean(ReactiveAdapterRegistry.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.webFluxAdapterRegistry());
		context.registerBean(RequestedContentTypeResolver.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.webFluxContentTypeResolver());
		context.registerBean(FormattingConversionService.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.webFluxConversionService());
		context.registerBean(Validator.class, () -> context
				.getBean(EnableWebFluxConfigurationWrapper.class).webFluxValidator());
		context.registerBean(WebHttpHandlerBuilder.WEB_HANDLER_BEAN_NAME,
				DispatcherHandler.class, () -> context
						.getBean(EnableWebFluxConfigurationWrapper.class).webHandler());
		context.registerBean(WebFluxConfig.class);
	}

	private void registerHttpHandlerAutoConfiguration() {
		context.registerBean(HttpHandler.class,
				() -> WebHttpHandlerBuilder.applicationContext(context).build());
	}

	private void registerDemoApplication() {
		context.registerBean(RouterFunction.class, () -> userEndpoints());
	}

	private void registerGsonAutoConfiguration() {
		GsonAutoConfiguration config = new GsonAutoConfiguration();
		context.registerBean(GsonBuilder.class, () -> config.gsonBuilder(new ArrayList<>(
				context.getBeansOfType(GsonBuilderCustomizer.class).values())));
		context.registerBean(Gson.class,
				() -> config.gson(context.getBean(GsonBuilder.class)));
		context.registerBean(GsonBuilderCustomizer.class, () -> config
				.standardGsonBuilderCustomizer(context.getBean(GsonProperties.class)));
	}

	private void registerHttpMessageConvertersAutoConfiguration() {
		// TODO: re-instate default message converters
		context.registerBean(HttpMessageConverters.class,
				() -> new HttpMessageConverters(false, Collections.emptyList()));
		context.registerBean(StringHttpMessageConverter.class,
				this::stringHttpMessageConverter);
		context.registerBean(GsonHttpMessageConverter.class,
				() -> new GsonHttpMessageConverter(context.getBean(Gson.class)));
	}

	StringHttpMessageConverter stringHttpMessageConverter() {
		StringHttpMessageConverter converter = new StringHttpMessageConverter(
				context.getBean(HttpEncodingProperties.class).getCharset());
		converter.setWriteAcceptCharset(false);
		return converter;
	}

	private void registerReactorCoreAutoConfiguration() {
		context.registerBean(ReactorConfiguration.class,
				() -> new ReactorConfiguration());
	}

	private void registerRestTemplateAutoConfiguration() {
		context.registerBean(RestTemplateAutoConfiguration.class);
		context.registerBean(RestTemplateBuilder.class, () -> context
				.getBean(RestTemplateAutoConfiguration.class).restTemplateBuilder());
	}

	private void registerWebClientAutoConfiguration() {
		context.registerBean(WebClientAutoConfiguration.class);
		context.registerBean(WebClient.Builder.class, () -> context
				.getBean(WebClientAutoConfiguration.class).webClientBuilder());
	}

}

class ReactorConfiguration {

	@Autowired
	protected void initialize(ReactorCoreProperties properties) {
		if (properties.getStacktraceMode().isEnabled()) {
			Hooks.onOperatorDebug();
		}
	}

}

class EnableWebFluxConfigurationWrapper extends EnableWebFluxConfiguration {

	public EnableWebFluxConfigurationWrapper(WebFluxProperties properties) {
		super(properties);
	}

}

class NoopParameterNameDiscoverer implements ParameterNameDiscoverer {

	@Override
	public String[] getParameterNames(Method method) {
		return null;
	}

	@Override
	public String[] getParameterNames(Constructor<?> ctor) {
		return null;
	}

}

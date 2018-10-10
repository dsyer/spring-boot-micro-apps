package com.example.func;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.example.config.FunctionalEnvironmentPostProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.http.HttpProperties;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.boot.autoconfigure.reactor.core.ReactorCoreProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.reactive.ResourceHandlerRegistrationCustomizer;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.EnableWebFluxConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.WebFluxConfig;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxRegistrations;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.catalog.FunctionInspector;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogInitializer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.function.server.support.ServerResponseResultHandler;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityResultHandler;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.i18n.LocaleContextResolver;

import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

public class FuncInitializer implements
		ApplicationContextInitializer<GenericApplicationContext> {

	public static final String MARKER = "Benchmark app started";

	private static final String PREFERRED_MAPPER_PROPERTY = "spring.http.converters.preferred-json-mapper";

	private GenericApplicationContext context;

	@Override
	public void initialize(GenericApplicationContext context) {
		this.context = context;
		((AbstractAutowireCapableBeanFactory) context.getDefaultListableBeanFactory())
				.setParameterNameDiscoverer(new NoopParameterNameDiscoverer());
		if (context.getEnvironment().getProperty("boot.active", Boolean.class, false)) {
			System.err.println("Boot active...");
			performPreinitialization();
		}
		else {
			System.err.println("Boot not active...");
			new FunctionalEnvironmentPostProcessor()
					.postProcessEnvironment(context.getEnvironment(), null);
			new ConfigFileApplicationListener().postProcessEnvironment(
					context.getEnvironment(), new SpringApplication());
			registerFunctionContext();
		}
		registerConverters();
		registerConfigurationProperties();
		context.registerBean(AutowiredAnnotationBeanPostProcessor.class);
		registerDemoApplication();
		registerWebServerFactoryCustomizerBeanPostProcessor();
		registerReactiveWebServerFactoryAutoConfiguration();
		registerErrorWebFluxAutoConfiguration();
		registerWebFluxAutoConfiguration();
		registerHttpHandlerAutoConfiguration();
		registerHttpMessageConvertersAutoConfiguration();
		registerReactorCoreAutoConfiguration();
		registerRestTemplateAutoConfiguration();
		registerWebClientAutoConfiguration();
	}

	private void registerFunctionContext() {
		new ContextFunctionCatalogInitializer().initialize(context);
	}

	private void registerConverters() {
		// Graal needs this?
		context.registerBean(Converter.class, () -> new SerializingConverter());
		context.registerBean(GenericConverter.class, () -> new DummyGenericConverter());
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
		context.registerBean(JacksonProperties.class, () -> new JacksonProperties());
		context.registerBean(ServerProperties.class, () -> new ServerProperties());
		context.registerBean(ResourceProperties.class, () -> new ResourceProperties());
		context.registerBean(WebFluxProperties.class, () -> new WebFluxProperties());
		context.registerBean(HttpProperties.class, () -> new HttpProperties());
		context.registerBean(ReactorCoreProperties.class,
				() -> new ReactorCoreProperties());
	}

	private void registerWebServerFactoryCustomizerBeanPostProcessor() {
		context.registerBean("webServerFactoryCustomizerBeanPostProcessor",
				WebServerFactoryCustomizerBeanPostProcessor.class);
	}

	private void registerReactiveWebServerFactoryAutoConfiguration() {
		ReactiveWebServerFactoryAutoConfiguration config = new ReactiveWebServerFactoryAutoConfiguration();
		context.registerBean(ReactiveWebServerFactoryCustomizer.class,
				() -> config.reactiveWebServerFactoryCustomizer(
						context.getBean(ServerProperties.class)));
		context.registerBean(NettyReactiveWebServerFactory.class,
				() -> new NettyReactiveWebServerFactory());
	}

	private void registerErrorWebFluxAutoConfiguration() {
		context.registerBean(ErrorAttributes.class, () -> new DefaultErrorAttributes(
				context.getBean(ServerProperties.class).getError().isIncludeException()));
		context.registerBean(ErrorWebExceptionHandler.class, () -> {
			return errorWebFluxAutoConfiguration()
					.errorWebExceptionHandler(context.getBean(ErrorAttributes.class));
		});
	}

	private ErrorWebFluxAutoConfiguration errorWebFluxAutoConfiguration() {
		ServerProperties serverProperties = context.getBean(ServerProperties.class);
		ResourceProperties resourceProperties = context.getBean(ResourceProperties.class);
		ServerCodecConfigurer serverCodecs = context.getBean(ServerCodecConfigurer.class);
		return new ErrorWebFluxAutoConfiguration(serverProperties, resourceProperties,
				context.getDefaultListableBeanFactory().getBeanProvider(ResolvableType
						.forClassWithGenerics(List.class, ViewResolver.class)),
				serverCodecs, context);
	}

	private void registerWebFluxAutoConfiguration() {
		context.registerBean(EnableWebFluxConfigurationWrapper.class,
				() -> new EnableWebFluxConfigurationWrapper(context));
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
		context.registerBean(WebFluxConfigurer.class,
				() -> new WebFluxConfig(context.getBean(ResourceProperties.class),
						context.getBean(WebFluxProperties.class), context,
						context.getBeanProvider(ResolvableType.forClassWithGenerics(
								List.class, HandlerMethodArgumentResolver.class)),
						context.getBeanProvider(ResolvableType
								.forClassWithGenerics(List.class, CodecCustomizer.class)),
						context.getBeanProvider(ResolvableType.forClassWithGenerics(
								List.class, ResourceHandlerRegistrationCustomizer.class)),
						context.getBeanProvider(ResolvableType
								.forClassWithGenerics(List.class, ViewResolver.class))));
	}

	private void registerHttpHandlerAutoConfiguration() {
		context.registerBean(HttpHandler.class,
				() -> WebHttpHandlerBuilder.applicationContext(context).build());
	}

	private void registerDemoApplication() {
		context.registerBean(Endpoint.class,
				() -> new Endpoint(context.getBean(FunctionCatalog.class),
						context.getBean(FunctionInspector.class), context.getEnvironment()));
		context.registerBean(RouterFunction.class,
				() -> context.getBean(Endpoint.class).userEndpoints());
	}

	private void registerHttpMessageConvertersAutoConfiguration() {
		// TODO: re-instate default message converters
		context.registerBean(HttpMessageConverters.class,
				() -> new HttpMessageConverters(false, Collections.emptyList()));
		context.registerBean(StringHttpMessageConverter.class,
				this::stringHttpMessageConverter);
		if (ClassUtils.isPresent("com.google.gson.Gson", null)
				&& "gson".equals(context.getEnvironment().getProperty(
						FuncInitializer.PREFERRED_MAPPER_PROPERTY,
						"gson"))) { 
					context.registerBean(GsonHttpMessageConverter.class,
									() -> new GsonHttpMessageConverter(context.getBean(Gson.class)));
			
		} else if (ClassUtils.isPresent(
				"com.fasterxml.jackson.databind.ObjectMapper", null)) {
			context.registerBean(MappingJackson2HttpMessageConverter.class,
					() -> new MappingJackson2HttpMessageConverter(
							context.getBean(ObjectMapper.class)));
		}
	}

	StringHttpMessageConverter stringHttpMessageConverter() {
		StringHttpMessageConverter converter = new StringHttpMessageConverter(
				context.getBean(HttpProperties.class).getEncoding().getCharset());
		converter.setWriteAcceptCharset(false);
		return converter;
	}

	private void registerReactorCoreAutoConfiguration() {
		context.registerBean(ReactorConfiguration.class,
				() -> new ReactorConfiguration());
	}

	private void registerRestTemplateAutoConfiguration() {
		RestTemplateAutoConfiguration config = new RestTemplateAutoConfiguration(
				context.getDefaultListableBeanFactory()
						.getBeanProvider(HttpMessageConverters.class),
				context.getDefaultListableBeanFactory().getBeanProvider(ResolvableType
						.forClassWithGenerics(List.class, RestTemplateCustomizer.class)));
		context.registerBean(RestTemplateBuilder.class,
				() -> config.restTemplateBuilder());
	}

	private void registerWebClientAutoConfiguration() {
		context.registerBean(WebClient.Builder.class, () -> {
			WebClientAutoConfiguration config = new WebClientAutoConfiguration(
					context.getBeanProvider(ResolvableType.forClassWithGenerics(
							List.class, WebClientCustomizer.class)));
			return config.webClientBuilder();
		});
	}

	// https://jira.spring.io/browse/SPR-17333
	static class ClassUtils {

		public static boolean isPresent(String string, ClassLoader classLoader) {
			if (classLoader==null) {
				classLoader = ClassUtils.class.getClassLoader();
			}
			try {
				return Class.forName(string, false, classLoader) != null;
			}
			catch (Throwable e) {
				try {
					return Class.forName(string) != null;
				}
				catch (Throwable t) {
					return false;
				}
			}
		}
		
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

	public EnableWebFluxConfigurationWrapper(GenericApplicationContext context) {
		super(context.getBean(WebFluxProperties.class),
				context.getBeanProvider(WebFluxRegistrations.class));
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

package com.example.func;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.http.HttpProperties;
import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
import org.springframework.boot.autoconfigure.reactor.core.ReactorCoreProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.reactive.ResourceHandlerRegistrationCustomizer;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.EnableWebFluxConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration.WebFluxConfig;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxRegistrations;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.catalog.FunctionInspector;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.ClassUtils;
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
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.i18n.LocaleContextResolver;

import reactor.core.publisher.Hooks;

public class FuncInitializer
		implements ApplicationContextInitializer<GenericApplicationContext> {

	public static final String MARKER = "Benchmark app started";

	private static final String PREFERRED_MAPPER_PROPERTY = "spring.http.converters.preferred-json-mapper";

	@Override
	public void initialize(GenericApplicationContext context) {
		registerConfigurationProperties(context);
		registerEndpoint(context);
		registerWebServerFactoryCustomizerBeanPostProcessor(context);
		registerReactiveWebServerFactoryAutoConfiguration(context);
		registerErrorWebFluxAutoConfiguration(context);
		registerWebFluxAutoConfiguration(context);
		registerHttpHandlerAutoConfiguration(context);
		registerHttpMessageConvertersAutoConfiguration(context);
		registerReactorCoreAutoConfiguration(context);
		registerWebClientAutoConfiguration(context);
	}

	private void registerConfigurationProperties(GenericApplicationContext context) {
		context.registerBean(JacksonProperties.class, () -> new JacksonProperties());
		context.registerBean(ServerProperties.class, () -> new ServerProperties());
		context.registerBean(ResourceProperties.class, () -> new ResourceProperties());
		context.registerBean(WebFluxProperties.class, () -> new WebFluxProperties());
		context.registerBean(HttpProperties.class, () -> new HttpProperties());
		context.registerBean(ReactorCoreProperties.class,
				() -> new ReactorCoreProperties());
	}

	private void registerWebServerFactoryCustomizerBeanPostProcessor(
			GenericApplicationContext context) {
		context.registerBean("webServerFactoryCustomizerBeanPostProcessor",
				WebServerFactoryCustomizerBeanPostProcessor.class);
	}

	private void registerReactiveWebServerFactoryAutoConfiguration(
			GenericApplicationContext context) {
		ReactiveWebServerFactoryAutoConfiguration config = new ReactiveWebServerFactoryAutoConfiguration();
		context.registerBean(ReactiveWebServerFactoryCustomizer.class,
				() -> config.reactiveWebServerFactoryCustomizer(
						context.getBean(ServerProperties.class)));
		context.registerBean(NettyReactiveWebServerFactory.class,
				() -> new NettyReactiveWebServerFactory());
	}

	private void registerErrorWebFluxAutoConfiguration(
			GenericApplicationContext context) {
		context.registerBean(ErrorAttributes.class, () -> new DefaultErrorAttributes(
				context.getBean(ServerProperties.class).getError().isIncludeException()));
		context.registerBean(ErrorWebExceptionHandler.class, () -> {
			return errorWebFluxAutoConfiguration(context)
					.errorWebExceptionHandler(context.getBean(ErrorAttributes.class));
		});
	}

	private ErrorWebFluxAutoConfiguration errorWebFluxAutoConfiguration(
			GenericApplicationContext context) {
		ServerProperties serverProperties = context.getBean(ServerProperties.class);
		ResourceProperties resourceProperties = context.getBean(ResourceProperties.class);
		ServerCodecConfigurer serverCodecs = context.getBean(ServerCodecConfigurer.class);
		return new ErrorWebFluxAutoConfiguration(serverProperties, resourceProperties,
				context.getDefaultListableBeanFactory().getBeanProvider(ResolvableType
						.forClassWithGenerics(List.class, ViewResolver.class)),
				serverCodecs, context);
	}

	private void registerWebFluxAutoConfiguration(GenericApplicationContext context) {
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

	private void registerHttpHandlerAutoConfiguration(GenericApplicationContext context) {
		context.registerBean(HttpHandler.class,
				() -> WebHttpHandlerBuilder.applicationContext(context).build());
	}

	private void registerEndpoint(GenericApplicationContext context) {
		context.registerBean(Endpoint.class,
				() -> new Endpoint(context.getBean(FunctionCatalog.class),
						context.getBean(FunctionInspector.class),
						context.getEnvironment()));
		context.registerBean(RouterFunction.class,
				() -> context.getBean(Endpoint.class).userEndpoints());
	}

	private void registerHttpMessageConvertersAutoConfiguration(
			GenericApplicationContext context) {
		// TODO: re-instate default message converters
		context.registerBean(HttpMessageConverters.class,
				() -> new HttpMessageConverters(false, Collections.emptyList()));
		context.registerBean(StringHttpMessageConverter.class,
				() -> stringHttpMessageConverter(context));
		if (ClassUtils.isPresent("com.google.gson.Gson", null)
				&& "gson".equals(context.getEnvironment().getProperty(
						FuncInitializer.PREFERRED_MAPPER_PROPERTY, "gson"))) {
			context.registerBean(GsonHttpMessageConverter.class,
					() -> new GsonHttpMessageConverter(context.getBean(Gson.class)));

		}
		else if (ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
				null)) {
			context.registerBean(MappingJackson2HttpMessageConverter.class,
					() -> new MappingJackson2HttpMessageConverter(
							context.getBean(ObjectMapper.class)));
		}
	}

	private StringHttpMessageConverter stringHttpMessageConverter(
			GenericApplicationContext context) {
		StringHttpMessageConverter converter = new StringHttpMessageConverter(
				context.getBean(HttpProperties.class).getEncoding().getCharset());
		converter.setWriteAcceptCharset(false);
		return converter;
	}

	private void registerReactorCoreAutoConfiguration(GenericApplicationContext context) {
		context.registerBean(ReactorConfiguration.class,
				() -> new ReactorConfiguration());
	}

	private void registerWebClientAutoConfiguration(GenericApplicationContext context) {
		context.registerBean(WebClient.Builder.class, () -> {
			WebClientAutoConfiguration config = new WebClientAutoConfiguration(
					context.getBeanProvider(ResolvableType.forClassWithGenerics(
							List.class, WebClientCustomizer.class)));
			return config.webClientBuilder();
		});
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

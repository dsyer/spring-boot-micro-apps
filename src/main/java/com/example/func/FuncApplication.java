package com.example.func;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.example.config.LazyInitBeanFactoryPostProcessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer;
import org.springframework.boot.autoconfigure.gson.GsonProperties;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
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
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
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
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter;
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping;
import org.springframework.web.reactive.function.server.support.ServerResponseResultHandler;
import org.springframework.web.reactive.resource.ResourceUrlProvider;
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

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

public class FuncApplication implements Runnable, Closeable, ApplicationContextInitializer<GenericApplicationContext> {

	private static Log logger = LogFactory.getLog(FuncApplication.class);

	public static final String MARKER = "Benchmark app started";

	private GenericApplicationContext context;

	@Bean
	public RouterFunction<?> userEndpoints() {
		return route(GET("/"), request -> ok().body(Mono.just("Hello"), String.class)).andRoute(POST("/"),
				request -> ok().body(request.bodyToFlux(String.class).map(value -> value.toUpperCase()), String.class));
	}

	public static void main(String[] args) throws Exception {
		long t0 = System.currentTimeMillis();
		FuncApplication bean = new FuncApplication();
		bean.run();
		System.err.println("Started HttpServer: " + (System.currentTimeMillis() - t0) + "ms");
		if (Boolean.getBoolean("demo.close")) {
			bean.close();
		}
	}

	public void log(ConfigurableApplicationContext context) {
		int count = 0;
		String id = context.getId();
		List<String> names = new ArrayList<>();
		while (context != null) {
			count += context.getBeanDefinitionCount();
			names.addAll(Arrays.asList(context.getBeanDefinitionNames()));
			context = (ConfigurableApplicationContext) context.getParent();
		}
		logger.info("Bean count: " + id + "=" + count);
		logger.debug("Bean names: " + id + "=" + names);
		try {
			logger.info(
					"Class count: " + id + "=" + ManagementFactory.getClassLoadingMXBean().getTotalLoadedClassCount());
		}
		catch (Throwable e) {
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
		context.addBeanFactoryPostProcessor(new LazyInitBeanFactoryPostProcessor());
		context.setId("application");
		initialize(context);
		context.refresh();
		log(context);
		System.err.println(MARKER);
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
		registerWebClientAutoConfiguration();
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
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
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
		ConfigurationPropertiesBindingPostProcessor.register(context);
		context.registerBean(ServerProperties.class, () -> new ServerProperties());
		context.registerBean(ResourceProperties.class, () -> new ResourceProperties());
		context.registerBean(WebFluxProperties.class, () -> new WebFluxProperties());
		context.registerBean(GsonProperties.class, () -> new GsonProperties());
	}

	private void registerWebServerFactoryCustomizerBeanPostProcessor() {
		context.registerBean("webServerFactoryCustomizerBeanPostProcessor",
				WebServerFactoryCustomizerBeanPostProcessor.class);
	}

	private void registerPropertyPlaceholderAutoConfiguration() {
		context.registerBean(PropertySourcesPlaceholderConfigurer.class,
				() -> PropertyPlaceholderAutoConfiguration.propertySourcesPlaceholderConfigurer());
	}

	private void registerReactiveWebServerFactoryAutoConfiguration() {
		ReactiveWebServerFactoryAutoConfiguration config = new ReactiveWebServerFactoryAutoConfiguration();
		context.registerBean(ReactiveWebServerFactoryCustomizer.class,
				() -> config.reactiveWebServerFactoryCustomizer(context.getBean(ServerProperties.class)));
		context.registerBean(NettyReactiveWebServerFactory.class, () -> new NettyReactiveWebServerFactory());
	}

	private void registerErrorWebFluxAutoConfiguration() {
		context.registerBean(ErrorAttributes.class, () -> new DefaultErrorAttributes());
		context.registerBean(ErrorWebExceptionHandler.class, () -> {
			return errorWebFluxAutoConfiguration().errorWebExceptionHandler(context.getBean(ErrorAttributes.class),
					context.getBean(ResourceProperties.class),
					context.getDefaultListableBeanFactory()
							.getBeanProvider(ResolvableType.forClassWithGenerics(List.class, ViewResolver.class)),
					context.getBean(ServerCodecConfigurer.class), context);
		});
	}

	private ErrorWebFluxAutoConfiguration errorWebFluxAutoConfiguration() {
		ServerProperties serverProperties = context.getBean(ServerProperties.class);
		return new ErrorWebFluxAutoConfiguration(serverProperties);
	}

	private void registerWebFluxAutoConfiguration() {
		context.registerBean(EnableWebFluxConfigurationWrapper.class,
				() -> new EnableWebFluxConfigurationWrapper(context.getBean(WebFluxProperties.class),
						context.getBeanProvider(WebFluxRegistrations.class)));
		context.registerBean(HandlerFunctionAdapter.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).handlerFunctionAdapter());
		context.registerBean(WebHttpHandlerBuilder.LOCALE_CONTEXT_RESOLVER_BEAN_NAME, LocaleContextResolver.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).localeContextResolver());
		context.registerBean(RequestMappingHandlerAdapter.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).requestMappingHandlerAdapter(
						context.getBean(ReactiveAdapterRegistry.class), context.getBean(ServerCodecConfigurer.class),
						context.getBean(FormattingConversionService.class), context.getBean(Validator.class)));
		context.registerBean(RequestMappingHandlerMapping.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.requestMappingHandlerMapping(context.getBean(RequestedContentTypeResolver.class)));
		context.registerBean(ResourceUrlProvider.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).resourceUrlProvider());
		context.registerBean(HandlerMapping.class, () -> context.getBean(EnableWebFluxConfigurationWrapper.class)
				.resourceHandlerMapping(context.getBean(ResourceUrlProvider.class)));
		context.registerBean(ResponseBodyResultHandler.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).responseBodyResultHandler(
						context.getBean(ReactiveAdapterRegistry.class), context.getBean(ServerCodecConfigurer.class),
						context.getBean(RequestedContentTypeResolver.class)));
		context.registerBean(ResponseEntityResultHandler.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).responseEntityResultHandler(
						context.getBean(ReactiveAdapterRegistry.class), context.getBean(ServerCodecConfigurer.class),
						context.getBean(RequestedContentTypeResolver.class)));
		context.registerBean(WebExceptionHandler.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).responseStatusExceptionHandler());
		context.registerBean(RouterFunctionMapping.class, () -> context.getBean(EnableWebFluxConfigurationWrapper.class)
				.routerFunctionMapping(context.getBean(ServerCodecConfigurer.class)));
		context.registerBean(WebHttpHandlerBuilder.SERVER_CODEC_CONFIGURER_BEAN_NAME, ServerCodecConfigurer.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).serverCodecConfigurer());
		context.registerBean(ServerResponseResultHandler.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class)
						.serverResponseResultHandler(context.getBean(ServerCodecConfigurer.class)));
		context.registerBean(SimpleHandlerAdapter.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).simpleHandlerAdapter());
		context.registerBean(ViewResolutionResultHandler.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).viewResolutionResultHandler(
						context.getBean(ReactiveAdapterRegistry.class),
						context.getBean(RequestedContentTypeResolver.class)));
		context.registerBean(ReactiveAdapterRegistry.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).webFluxAdapterRegistry());
		context.registerBean(RequestedContentTypeResolver.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).webFluxContentTypeResolver());
		context.registerBean(FormattingConversionService.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).webFluxConversionService());
		context.registerBean(Validator.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).webFluxValidator());
		context.registerBean(WebHttpHandlerBuilder.WEB_HANDLER_BEAN_NAME, DispatcherHandler.class,
				() -> context.getBean(EnableWebFluxConfigurationWrapper.class).webHandler());
		context.registerBean(WebFluxConfigurer.class,
				() -> new WebFluxConfig(context.getBean(ResourceProperties.class),
						context.getBean(WebFluxProperties.class), context,
						context.getBeanProvider(HandlerMethodArgumentResolver.class),
						context.getBeanProvider(CodecCustomizer.class),
						context.getBeanProvider(ResourceHandlerRegistrationCustomizer.class),
						context.getBeanProvider(ViewResolver.class)));
	}

	private void registerHttpHandlerAutoConfiguration() {
		context.registerBean(HttpHandler.class, () -> WebHttpHandlerBuilder.applicationContext(context).build());
	}

	private void registerDemoApplication() {
		context.registerBean(RouterFunction.class, () -> userEndpoints());
	}

	private void registerGsonAutoConfiguration() {
		GsonAutoConfiguration config = new GsonAutoConfiguration();
		context.registerBean(GsonBuilder.class, () -> config
				.gsonBuilder(new ArrayList<>(context.getBeansOfType(GsonBuilderCustomizer.class).values())));
		context.registerBean(Gson.class, () -> config.gson(context.getBean(GsonBuilder.class)));
		context.registerBean(GsonBuilderCustomizer.class,
				() -> config.standardGsonBuilderCustomizer(context.getBean(GsonProperties.class)));
	}

	private void registerHttpMessageConvertersAutoConfiguration() {
		// TODO: re-instate default message converters
		context.registerBean(HttpMessageConverters.class,
				() -> new HttpMessageConverters(false, Collections.emptyList()));
		context.registerBean(StringHttpMessageConverter.class, this::stringHttpMessageConverter);
		context.registerBean(GsonHttpMessageConverter.class,
				() -> new GsonHttpMessageConverter(context.getBean(Gson.class)));
	}

	StringHttpMessageConverter stringHttpMessageConverter() {
		StringHttpMessageConverter converter = new StringHttpMessageConverter(
				context.getBean(ServerProperties.class).getServlet().getEncoding().getCharset());
		converter.setWriteAcceptCharset(false);
		return converter;
	}

	private void registerWebClientAutoConfiguration() {
		context.registerBean(WebClient.Builder.class, () -> new WebClientAutoConfiguration().webClientBuilder(
				context.getBeanProvider(ResolvableType.forClassWithGenerics(List.class, WebClientCustomizer.class))));
	}

}

class EnableWebFluxConfigurationWrapper extends EnableWebFluxConfiguration {

	public EnableWebFluxConfigurationWrapper(WebFluxProperties webFluxProperties,
			ObjectProvider<WebFluxRegistrations> webFluxRegistrations) {
		super(webFluxProperties, webFluxRegistrations);
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

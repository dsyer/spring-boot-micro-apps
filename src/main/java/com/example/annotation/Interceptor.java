package com.example.annotation;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.ClassUtils;

@Aspect
public class Interceptor {

	private static Log logger = LogFactory.getLog(Interceptor.class);

	@Around("within(org.springframework.context.annotation.ConfigurationClassUtils) && "
			+ "execution( * hasNestedConfigurationClass(..))")
	public Object nested(ProceedingJoinPoint joinPoint) throws Throwable {
		if (logger.isDebugEnabled()) {
			logger.debug(joinPoint.toShortString() + ": "
					+ Arrays.asList(joinPoint.getArgs()));
		}
		return false;
	}

	@Around("execution( * org.springframework.context.annotation.ConfigurationClassUtils.checkConfigurationClassCandidate(..))")
	public Object check(ProceedingJoinPoint joinPoint) throws Throwable {
		if (logger.isDebugEnabled()) {
			logger.debug(joinPoint.toShortString() + ": "
					+ Arrays.asList(joinPoint.getArgs()));
		}
		return false;
	}

	@Around("execution(* org.springframework.beans.factory.support.AbstractBeanFactory+.getBean(String)) && args(bean)")
	public Object bean(ProceedingJoinPoint joinPoint, String bean) throws Throwable {
		if (logger.isDebugEnabled()) {
			logger.debug("Getting: " + bean);
		}
		if (joinPoint.getThis() instanceof ListableBeanFactory
				&& bean.contains("GenericConverter")) {
			ListableBeanFactory factory = (ListableBeanFactory) joinPoint.getThis();
			if (logger.isDebugEnabled()) {
				logger.debug(
						"Bean names: " + Arrays.asList(factory.getBeanDefinitionNames()));
			}
		}
		return joinPoint.proceed();
	}

	@Around("execution(* org.springframework.boot.system.ApplicationHome.findSource(..))")
	public Object source(ProceedingJoinPoint joinPoint) throws Throwable {
		return proceed(joinPoint);
	}

	@Around("execution(* org.springframework.boot.SpringApplication+.getSpringFactoriesInstances(..))")
	public Object factories(ProceedingJoinPoint joinPoint) throws Throwable {
		return proceed(joinPoint);
	}

	@Around("within(org.springframework.boot.autoconfigure.BackgroundPreinitializer) && call(* run*(..))")
	public Object background(ProceedingJoinPoint joinPoint) throws Throwable {
		return proceed(joinPoint);
	}

	@Around("execution(* org.springframework.boot.logging.LoggingSystem+.*(..))")
	public Object logging(ProceedingJoinPoint joinPoint) throws Throwable {
		return proceed(joinPoint);
	}

	@Around("execution(* org.springframework.boot.autoconfigure.BackgroundPreinitializer.ValidationInitializer.run(..))")
	public Object validation(ProceedingJoinPoint joinPoint) throws Throwable {
		if (!ClassUtils.isPresent("javax.validation.Validation", null)) {
			return null;
		}
		return proceed(joinPoint);
	}

	@Around("execution(* org.springframework.boot.autoconfigure.BackgroundPreinitializer.MBeanFactoryInitializer.run(..))")
	public Object tomcat(ProceedingJoinPoint joinPoint) throws Throwable {
		if (!ClassUtils.isPresent("org.apache.catalina.mbeans.MBeanFactory", null)) {
			return null;
		}
		return proceed(joinPoint);
	}

	@Around("within(org.springframework.boot.autoconfigure.BackgroundPreinitializer.JacksonInitializer) && execution(* run(..))")
	public Object jackson(ProceedingJoinPoint joinPoint) throws Throwable {
		if (!ClassUtils.isPresent("org.springframework.http.converter.json.JsonFactory",
				null)) {
			return null;
		}
		return proceed(joinPoint);
	}

	@Around("execution(* org.springframework.util.ClassUtils.isPresent(String,..)) && args(name,..)")
	public Object present(ProceedingJoinPoint joinPoint, String name) throws Throwable {
		try {
			Class.forName(name);
			System.err.println("ClassUtils present:     " + name);
			return true;
		}
		catch (Throwable e) {
			System.err.println("ClassUtils not present: " + name);
			return false;
		}
	}

	@Around("execution(* org.springframework.util.ClassUtils.forName(String,..)) && args(name,..)")
	public Object source(ProceedingJoinPoint joinPoint, String name) throws Throwable {
		try {
			Class<?> type = Class.forName(name);
			System.err.println("ClassUtils found:     " + name);
			return type;
		}
		catch (Throwable e) {
			System.err.println("ClassUtils not found: " + name);
			throw e;
		}
	}

	private Object proceed(ProceedingJoinPoint joinPoint) {
		try {
			Object result = joinPoint.proceed();
			if (logger.isDebugEnabled()) {
				logger.debug(joinPoint.toShortString() + ": " + result);
			}
			return result;
		}
		catch (Throwable t) {
			if (logger.isDebugEnabled()) {
				logger.debug(joinPoint.toShortString() + ": " + t);
			}
			return null;
		}
	}

	static Interceptor instance = new Interceptor();

	public static Interceptor aspectOf() {
		return instance;
	}
}

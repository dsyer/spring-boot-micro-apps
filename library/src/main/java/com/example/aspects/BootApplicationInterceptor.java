package com.example.aspects;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.springframework.beans.factory.ListableBeanFactory;

@Aspect
public class BootApplicationInterceptor {

	private static Log logger = LogFactory.getLog(BootApplicationInterceptor.class);

	@Around("execution(* org.springframework.beans.factory.support.AbstractBeanFactory+.getBean(String)) && args(bean)")
	public Object bean(ProceedingJoinPoint joinPoint, String bean) throws Throwable {
		if (logger.isDebugEnabled()) {
			logger.debug("Getting: " + bean);
		}
		if (joinPoint.getThis() instanceof ListableBeanFactory && bean.contains("GenericConverter")) {
			ListableBeanFactory factory = (ListableBeanFactory) joinPoint.getThis();
			if (logger.isDebugEnabled()) {
				logger.debug("Bean names: " + Arrays.asList(factory.getBeanDefinitionNames()));
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

	@Around("execution(* org.springframework.boot.logging.LoggingSystem+.*(..))")
	public Object logging(ProceedingJoinPoint joinPoint) throws Throwable {
		return proceed(joinPoint);
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

	static BootApplicationInterceptor instance = new BootApplicationInterceptor();

	public static BootApplicationInterceptor aspectOf() {
		return instance;
	}
}

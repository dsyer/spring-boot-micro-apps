package com.example.aspects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.springframework.util.ClassUtils;

@Aspect
public class BootAutoConfigureInterceptor {

	private static Log logger = LogFactory.getLog(BootAutoConfigureInterceptor.class);

	@Around("within(org.springframework.boot.autoconfigure.BackgroundPreinitializer) && call(* run*(..))")
	public Object background(ProceedingJoinPoint joinPoint) throws Throwable {
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

	static BootAutoConfigureInterceptor instance = new BootAutoConfigureInterceptor();

	public static BootAutoConfigureInterceptor aspectOf() {
		return instance;
	}
}

package com.example.aspects;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class AnnotationProcessingInterceptor {

	private static Log logger = LogFactory.getLog(AnnotationProcessingInterceptor.class);

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

	static AnnotationProcessingInterceptor instance = new AnnotationProcessingInterceptor();

	public static AnnotationProcessingInterceptor aspectOf() {
		return instance;
	}
}

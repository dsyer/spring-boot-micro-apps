package com.example.aspects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.springframework.web.server.ServerWebExchange;

@Aspect
public class WebInterceptor {

	private static Log logger = LogFactory.getLog(WebInterceptor.class);

	@Around("execution(* org.springframework.web.server.adapter.HttpWebHandlerAdapter.*(..))")
	public Object source(ProceedingJoinPoint joinPoint) throws Throwable {
		Throwable t = joinPoint.getArgs().length > 1
				&& joinPoint.getArgs()[1] instanceof Throwable
						? (Throwable) joinPoint.getArgs()[1]
						: null;
		if (t != null) {
			t.printStackTrace();
		}
		return proceed(joinPoint);
	}

	private Object proceed(ProceedingJoinPoint joinPoint) {
		try {
			Object result = joinPoint.proceed();
			System.err.println(joinPoint.toShortString() + ": " + result);
			return result;
		}
		catch (Throwable t) {
			System.err.println(joinPoint.toShortString() + ": " + t);
			return null;
		}
	}

	static WebInterceptor instance = new WebInterceptor();

	public static WebInterceptor aspectOf() {
		return instance;
	}
}

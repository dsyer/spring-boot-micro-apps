package com.example.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class ClassInterceptor {

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

	static ClassInterceptor instance = new ClassInterceptor();

	public static ClassInterceptor aspectOf() {
		return instance;
	}
}

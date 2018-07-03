/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.func;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;

/**
 * @author Dave Syer
 *
 */
public class ObjectProviders {

	/**
	 * Create an {@link ObjectProvider} for the unique constructor argument in the target
	 * class.
	 */
	public static <T> ObjectProvider<T> provider(ApplicationContext context,
			Class<?> target) {
		return new LazyObjectProvider<>(context, target, -1);
	}

	/**
	 * Create an {@link ObjectProvider} for the constructor argument with the provided
	 * index, in the target class with a single constructor.
	 */
	public static <T> ObjectProvider<T> provider(ApplicationContext context,
			Class<?> target, int index) {
		return new LazyObjectProvider<>(context, target, index);
	}

	/**
	 * Create an {@link ObjectProvider} for the constructor argument with the provided
	 * index, in the target class with a constructor having the parameter type provided.
	 */
	public static <T> ObjectProvider<T> provider(ApplicationContext context,
			Class<?> target, int index, Class<?>... params) {
		return new LazyObjectProvider<>(context, target, index, params);
	}

	static class LazyObjectProvider<T> implements ObjectProvider<T> {

		private Map<Class<?>, Constructor<?>> constructors = new HashMap<>();

		private ApplicationContext context;
		private Class<?> target;
		private int index;
		private Class<?>[] params;
		private ObjectProvider<T> delegate;

		public LazyObjectProvider(ApplicationContext context, Class<?> target, int index,
				Class<?>... params) {
			this.context = context;
			this.target = target;
			this.index = index;
			this.params = params;
		}

		@Override
		public T getObject() throws BeansException {
			if (delegate == null) {
				delegate = provider(context, target, params);
			}
			return delegate.getObject();
		}

		@Override
		public T getObject(Object... args) throws BeansException {
			if (delegate == null) {
				delegate = provider(context, target, params);
			}
			return delegate.getObject(args);
		}

		@Override
		public T getIfAvailable() throws BeansException {
			if (delegate == null) {
				delegate = provider(context, target, params);
			}
			return delegate.getIfAvailable();
		}

		@Override
		public T getIfUnique() throws BeansException {
			if (delegate == null) {
				delegate = provider(context, target, params);
			}
			return delegate.getIfUnique();
		}

		private ObjectProvider<T> provider(ApplicationContext context, Class<?> target,
				Class<?>[] params) {
			Constructor<?> constructor = constructors.computeIfAbsent(target,
					this::constructor);
			int index = index(constructor);
			MethodParameter methodParameter = new MethodParameter(constructor, index);
			@SuppressWarnings("unchecked")
			ObjectProvider<T> provider = (ObjectProvider<T>) context
					.getAutowireCapableBeanFactory()
					.resolveDependency(new DependencyDescriptor(methodParameter, false),
							target.getName());
			return provider;
		}

		private int index(Constructor<?> constructor) {
			if (this.index >= 0) {
				return this.index;
			}
			Class<?>[] types = constructor.getParameterTypes();
			for (int i = 0; i < types.length; i++) {
				Class<?> type = types[i];
				if (ObjectProvider.class.isAssignableFrom(type)) {
					return i;
				}
			}
			return 0;
		}

		private Constructor<?> constructor(Class<?> target) {
			Constructor<?> constructor;
			try {
				Constructor<?>[] constructors = target.getConstructors();
				if (constructors.length == 1) {
					return constructors[0];
				}
				constructor = target.getConstructor(params);
			}
			catch (Exception e) {
				throw new IllegalStateException("Cannot resolve constructor", e);
			}
			return constructor;
		}

	}
}

package com.example.config;

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.ReflectionUtils;

class SpringBootBeanInfo implements BeanInfo {

	private final PropertyDescriptor[] propertyDescriptors;

	public SpringBootBeanInfo(Class<?> beanClass) throws IntrospectionException {
		this.propertyDescriptors = extractPropertyDescriptors(beanClass);
	}

	private PropertyDescriptor[] extractPropertyDescriptors(Class<?> beanClass)
			throws IntrospectionException {
		Map<String, Method> getters = new LinkedHashMap<>();
		Map<String, Method> setters = new LinkedHashMap<>();
		Method[] methods = ReflectionUtils.getAllDeclaredMethods(beanClass);
		for (Method method : methods) {
			collectGetterSetterMethod(method, getters, setters);
		}
		List<PropertyDescriptor> descriptors = new ArrayList<>(methods.length);
		for (Map.Entry<String, Method> entry : getters.entrySet()) {
			String name = entry.getKey();
			Method getter = entry.getValue();
			Method setter = setters.remove(name);
			if (setter != null && !getter.getReturnType()
					.isAssignableFrom(setter.getParameterTypes()[0])) {
				setter = null;
			}
			descriptors.add(new SlimPropertyDescriptor(name, getter, setter));
		}
		for (Map.Entry<String, Method> entry : setters.entrySet()) {
			Method setter = entry.getValue();
			String name = entry.getKey();
			// System.err.println("**************** " + setter);
			descriptors.add(new SlimPropertyDescriptor(name, null, setter));
		}
		return descriptors.toArray(new SlimPropertyDescriptor[descriptors.size()]);
	}

	private void collectGetterSetterMethod(Method method, Map<String, Method> getters,
			Map<String, Method> setters) {
		int argSize = method.getParameterTypes().length;
		if (!Modifier.isStatic(method.getModifiers())
				&& Modifier.isPublic(method.getModifiers()) && argSize <= 1) {
			String name = method.getName();
			if (argSize == 0 && name.length() > 3 && name.startsWith("get")
					&& !name.equals("getClass")) {
				getters.putIfAbsent(name.substring(3), method);
			}
			else if (argSize == 0 && name.length() > 2 && name.startsWith("is")
					&& method.getReturnType() == boolean.class) {
				getters.putIfAbsent(name.substring(2), method);
			}
			else if (argSize == 1 && name.length() > 3 && name.startsWith("set")) {
				setters.putIfAbsent(name.substring(3), method);
			}
		}
	}

	@Override
	public BeanDescriptor getBeanDescriptor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public EventSetDescriptor[] getEventSetDescriptors() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDefaultEventIndex() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getDefaultPropertyIndex() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MethodDescriptor[] getMethodDescriptors() {
		throw new UnsupportedOperationException();
	}

	@Override
	public BeanInfo[] getAdditionalBeanInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Image getIcon(int iconKind) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PropertyDescriptor[] getPropertyDescriptors() {
		return this.propertyDescriptors;
	}

}

class SlimPropertyDescriptor extends PropertyDescriptor {

	private Method readMethod;
	private Method writeMethod;

	public SlimPropertyDescriptor(String name, Method getter, Method setter)
			throws IntrospectionException {
		super(name, getter, setter);
	}

	@Override
	public synchronized void setReadMethod(Method readMethod)
			throws IntrospectionException {
		this.readMethod = readMethod;
	}

	@Override
	public synchronized void setWriteMethod(Method writeMethod)
			throws IntrospectionException {
		this.writeMethod = writeMethod;
	}

	@Override
	public Method getReadMethod() {
		return readMethod;
	}

	@Override
	public Method getWriteMethod() {
		return writeMethod;
	}
}

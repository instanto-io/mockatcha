/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd.jvm;

import io.instanto.mockatcha.bdd.PropertyMatching;
import io.instanto.mockatcha.bdd.PropertyReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Builds {@link PropertyReader} implementations on the JVM, matching the rules of {@code
 * TeaVmObjectMatchers}.
 */
public final class JvmObjectMatchers {

  private static final ClassValue<PropertyReader<?>> READERS =
      new ClassValue<>() {
        @Override
        protected PropertyReader<?> computeValue(Class<?> type) {
          return createReader(type);
        }
      };

  private JvmObjectMatchers() {}

  @SuppressWarnings("unchecked")
  public static <T> T objectContaining(Class<T> type, String property, Object expected) {
    return (T) PropertyMatching.matchOne(propertiesOf(type), property, expected);
  }

  @SuppressWarnings("unchecked")
  public static <T> T objectContaining(Class<T> type, Map<String, Object> properties) {
    return (T) PropertyMatching.matchAll(propertiesOf(type), properties);
  }

  @SuppressWarnings("unchecked")
  public static <T> PropertyReader<T> propertiesOf(Class<T> type) {
    Objects.requireNonNull(type, "type");
    return (PropertyReader<T>) READERS.get(type);
  }

  private static <T> PropertyReader<T> createReader(Class<T> type) {
    if (type.isPrimitive() || type.isArray() || type.isInterface()) {
      PropertyMatching.refuseType(
          "objectContaining needs a class with readable properties, but "
              + type.getName()
              + " is not one");
    }
    Map<String, Function<Object, Object>> properties = readablePropertiesOf(type);
    if (properties.isEmpty()) {
      PropertyMatching.refuseType(
          "objectContaining found no readable property on "
              + type.getName()
              + ". A property is a visible no-argument accessor or field.");
    }
    String[] names = properties.keySet().toArray(new String[0]);
    return new PropertyReader<>() {
      @Override
      public Object read(T target, String property) {
        Function<Object, Object> reader = properties.get(property);
        return reader == null ? PropertyMatching.UNKNOWN : reader.apply(target);
      }

      @Override
      public boolean canRead(Object target) {
        return type.isInstance(target);
      }

      @Override
      public String[] names() {
        return names.clone();
      }
    };
  }

  private static Map<String, Function<Object, Object>> readablePropertiesOf(Class<?> type) {
    Map<String, Function<Object, Object>> properties = new LinkedHashMap<>();
    for (Class<?> current = type; current != null; current = current.getSuperclass()) {
      for (Method method : current.getDeclaredMethods()) {
        if (method.getParameterCount() != 0
            || method.getReturnType() == void.class
            || !isVisibleInstanceMember(method.getModifiers())
            || isObjectMethod(method.getName())) {
          continue;
        }
        properties.putIfAbsent(propertyNameOf(method.getName()), target -> invoke(method, target));
      }
    }
    for (Class<?> current = type; current != null; current = current.getSuperclass()) {
      for (Field field : current.getDeclaredFields()) {
        if (!isVisibleInstanceMember(field.getModifiers())) {
          continue;
        }
        properties.putIfAbsent(field.getName(), target -> readField(field, target));
      }
    }
    return properties;
  }

  private static Object invoke(Method method, Object target) {
    if (!method.canAccess(target)) {
      method.trySetAccessible();
    }
    try {
      return method.invoke(target);
    } catch (InvocationTargetException failure) {
      throw new IllegalStateException(
          "Could not read property from " + method.getName(), failure.getCause());
    } catch (IllegalAccessException failure) {
      throw new IllegalStateException("Could not access property " + method.getName(), failure);
    }
  }

  private static Object readField(Field field, Object target) {
    if (!field.canAccess(target)) {
      field.trySetAccessible();
    }
    try {
      return field.get(target);
    } catch (IllegalAccessException failure) {
      throw new IllegalStateException("Could not access field " + field.getName(), failure);
    }
  }

  private static boolean isVisibleInstanceMember(int modifiers) {
    return !Modifier.isStatic(modifiers)
        && (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers));
  }

  private static boolean isObjectMethod(String name) {
    return name.equals("toString")
        || name.equals("hashCode")
        || name.equals("getClass")
        || name.equals("clone");
  }

  private static String propertyNameOf(String methodName) {
    if (methodName.startsWith("get") && methodName.length() > 3) {
      return decapitalise(methodName.substring(3));
    }
    if (methodName.startsWith("is") && methodName.length() > 2) {
      return decapitalise(methodName.substring(2));
    }
    return methodName;
  }

  private static String decapitalise(String name) {
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }
}

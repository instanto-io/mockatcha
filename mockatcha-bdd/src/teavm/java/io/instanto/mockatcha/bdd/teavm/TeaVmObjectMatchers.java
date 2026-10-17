/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd.teavm;

import static org.teavm.metaprogramming.Metaprogramming.accessor;
import static org.teavm.metaprogramming.Metaprogramming.caller;
import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.environment;
import static org.teavm.metaprogramming.Metaprogramming.exit;
import static org.teavm.metaprogramming.Metaprogramming.handle;
import static org.teavm.metaprogramming.Metaprogramming.proxy;

import io.instanto.mockatcha.bdd.PropertyMatching;
import io.instanto.mockatcha.bdd.PropertyReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectField;
import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.metaprogramming.ClassHandle;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.FieldAccessor;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.MethodCaller;
import org.teavm.metaprogramming.Value;

/**
 * Generates {@link PropertyReader} implementations while TeaVM compiles, with no reflection at
 * runtime.
 */
@CompileTime
public final class TeaVmObjectMatchers {

  private TeaVmObjectMatchers() {}

  @Meta
  public static native <T> T objectContaining(Class<T> type, String property, Object expected);

  @Meta
  public static native <T> T objectContaining(Class<T> type, Map<String, Object> properties);

  @Meta
  public static native <T> PropertyReader<T> propertiesOf(Class<T> type);

  static <T> void objectContaining(
      IntrospectClass<T> type, Value<String> property, Value<Object> expected) {
    Map<String, Property> properties = readablePropertiesOf(type);
    String rejection = rejectionReason(type, properties);
    if (rejection != null) {
      emit(() -> PropertyMatching.refuseType(rejection));
      exit(() -> null);
      return;
    }
    Value<PropertyReader<T>> reader = generateReader(type, properties);
    emit(() -> PropertyMatching.matchOne(reader.get(), property.get(), expected.get()));
    exit(() -> null);
  }

  static <T> void objectContaining(
      IntrospectClass<T> type, Value<Map<String, Object>> properties) {
    Map<String, Property> readable = readablePropertiesOf(type);
    String rejection = rejectionReason(type, readable);
    if (rejection != null) {
      emit(() -> PropertyMatching.refuseType(rejection));
      exit(() -> null);
      return;
    }
    Value<PropertyReader<T>> reader = generateReader(type, readable);
    emit(() -> PropertyMatching.matchAll(reader.get(), properties.get()));
    exit(() -> null);
  }

  static <T> void propertiesOf(IntrospectClass<T> type) {
    Map<String, Property> properties = readablePropertiesOf(type);
    String rejection = rejectionReason(type, properties);
    if (rejection != null) {
      emit(() -> PropertyMatching.refuseType(rejection));
      exit(() -> null);
      return;
    }
    Value<PropertyReader<T>> reader = generateReader(type, properties);
    exit(() -> reader.get());
  }

  private static String rejectionReason(
      IntrospectClass<?> type, Map<String, Property> properties) {
    if (type.isPrimitive() || type.isArray() || type.isInterface()) {
      return "objectContaining needs a class with readable properties, but "
          + type.name()
          + " is not one";
    }
    if (properties.isEmpty()) {
      return "objectContaining found no readable property on "
          + type.name()
          + ". A property is a visible no-argument accessor or field.";
    }
    return null;
  }

  /**
   * Emits a reader whose {@code read} is a chain of name comparisons, each guarding one direct
   * accessor call or field read.
   */
  @SuppressWarnings("rawtypes")
  private static <T> Value<PropertyReader<T>> generateReader(
      IntrospectClass<T> type, Map<String, Property> properties) {
    Value<PropertyReader> generated =
        proxy(
            environment().findClass(PropertyReader.class),
            (instance, method, arguments) -> {
              switch (method.name()) {
                case "read":
                  emitRead(properties, arguments[0], arguments[1]);
                  break;
                case "canRead":
                  ClassHandle<T> readableType = handle(type);
                  Value<Object> candidate = arguments[0];
                  exit(() -> readableType.isInstance(candidate.get()));
                  break;
                case "names":
                  emitNames(properties.keySet());
                  break;
                default:
                  break;
              }
            });

    @SuppressWarnings("unchecked")
    Value<PropertyReader<T>> reader = (Value<PropertyReader<T>>) (Value<?>) generated;
    return reader;
  }

  private static void emitRead(
      Map<String, Property> properties, Value<Object> target, Value<Object> property) {
    // A one-element array because a Value cannot be reassigned as the chain accumulates a result.
    Value<Object[]> found = emit(() -> new Object[] {PropertyMatching.UNKNOWN});
    properties.forEach(
        (name, source) -> {
          if (source.method != null) {
            MethodCaller accessor = caller(source.method);
            emit(
                () -> {
                  if (name.equals(property.get())) {
                    found.get()[0] = accessor.call(target.get());
                  }
                });
          } else {
            FieldAccessor field = accessor(source.field);
            emit(
                () -> {
                  if (name.equals(property.get())) {
                    found.get()[0] = field.get(target.get());
                  }
                });
          }
        });
    exit(() -> found.get()[0]);
  }

  private static void emitNames(Collection<String> propertyNames) {
    List<String> names = new ArrayList<>(propertyNames);
    int count = names.size();
    Value<String[]> array = emit(() -> new String[count]);
    for (int index = 0; index < count; index++) {
      int position = index;
      String name = names.get(index);
      emit(() -> array.get()[position] = name);
    }
    exit(() -> array.get());
  }

  /**
   * Collects the readable properties of a type, an accessor winning over a field of the same name.
   */
  private static Map<String, Property> readablePropertiesOf(IntrospectClass<?> type) {
    Map<String, Property> properties = new LinkedHashMap<>();
    for (IntrospectMethod method : type.methods()) {
      if (method.isConstructor()
          || !method.parameters().isEmpty()
          || method.returnType().name().equals("void")
          || !isVisibleInstanceMember(method.modifiers())
          || isObjectMethod(method.name())) {
        continue;
      }
      properties.putIfAbsent(propertyNameOf(method.name()), new Property(method, null));
    }
    for (IntrospectField field : type.fields()) {
      if (!isVisibleInstanceMember(field.modifiers())) {
        continue;
      }
      properties.putIfAbsent(field.name(), new Property(null, field));
    }
    return properties;
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

  private static final class Property {
    private final IntrospectMethod method;
    private final IntrospectField field;

    private Property(IntrospectMethod method, IntrospectField field) {
      this.method = method;
      this.field = field;
    }
  }
}

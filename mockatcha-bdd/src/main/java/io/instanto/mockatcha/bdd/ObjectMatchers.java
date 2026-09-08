/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

import static org.teavm.metaprogramming.Metaprogramming.accessor;
import static org.teavm.metaprogramming.Metaprogramming.caller;
import static org.teavm.metaprogramming.Metaprogramming.currentLocation;
import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.environment;
import static org.teavm.metaprogramming.Metaprogramming.exit;
import static org.teavm.metaprogramming.Metaprogramming.handle;
import static org.teavm.metaprogramming.Metaprogramming.proxy;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
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
 * Matches an object by naming some of its properties and ignoring the rest.
 *
 * <p>The property reader is generated while TeaVM compiles the test, so nothing is looked up
 * reflectively at runtime.
 *
 * <pre>{@code
 * verify(repository).save(objectContaining(Timesheet.class, "employeeId", "A-17"));
 *
 * verify(audit).log(objectContaining(Timesheet.class,
 *         Map.of("employeeId", "A-17", "hours", 38)));
 * }</pre>
 *
 * <p>The class must be a compile-time constant. A property is a no-argument accessor such as
 * {@code hours()}, {@code getHours()}, or {@code isActive()}, or a visible field. Accessors and
 * fields may be declared or inherited. An unknown property name fails when the matcher is built.
 */
@CompileTime
public final class ObjectMatchers {

  private ObjectMatchers() {}

  /** Matches an object of this type whose named property equals a value. */
  @Meta
  public static native <T> T objectContaining(Class<T> type, String property, Object expected);

  /** Matches an object of this type whose named properties all equal their values. */
  @Meta
  public static native <T> T objectContaining(Class<T> type, Map<String, Object> properties);

  /**
   * Builds a reader for a type, for inspecting a value directly.
   *
   * <pre>{@code
   * PropertyReader<Timesheet> reader = propertiesOf(Timesheet.class);
   * assertEquals("A-17", reader.read(saved.getValue(), "employeeId"));
   * }</pre>
   */
  @Meta
  public static native <T> PropertyReader<T> propertiesOf(Class<T> type);

  private static <T> void objectContaining(
      IntrospectClass<T> type, Value<String> property, Value<Object> expected) {
    Value<PropertyReader<T>> reader = generateReader(type);
    if (reader == null) {
      exit(() -> null);
      return;
    }
    emit(() -> PropertyMatching.matchOne(reader.get(), property.get(), expected.get()));
    exit(() -> null);
  }

  private static <T> void objectContaining(
      IntrospectClass<T> type, Value<Map<String, Object>> properties) {
    Value<PropertyReader<T>> reader = generateReader(type);
    if (reader == null) {
      exit(() -> null);
      return;
    }
    emit(() -> PropertyMatching.matchAll(reader.get(), properties.get()));
    exit(() -> null);
  }

  private static <T> void propertiesOf(IntrospectClass<T> type) {
    Value<PropertyReader<T>> reader = generateReader(type);
    if (reader == null) {
      exit(() -> null);
      return;
    }
    exit(() -> reader.get());
  }

  /**
   * Emits a reader whose {@code read} is a chain of name comparisons, each guarding one direct
   * accessor call or field read.
   */
  @SuppressWarnings("rawtypes")
  private static <T> Value<PropertyReader<T>> generateReader(IntrospectClass<T> type) {
    if (type.isPrimitive() || type.isArray() || type.isInterface()) {
      environment()
          .diagnostics()
          .error(
              currentLocation(),
              "objectContaining needs a class with readable properties, but " + type.name()
                  + " is not one");
      return null;
    }

    Map<String, Property> properties = readablePropertiesOf(type);
    if (properties.isEmpty()) {
      environment()
          .diagnostics()
          .error(
              currentLocation(),
              "objectContaining found no readable property on " + type.name()
                  + ". A property is a visible no-argument accessor or field.");
      return null;
    }

    List<String> names = new ArrayList<>(properties.keySet());
    ClassHandle<T> readableType = handle(type);

    Value<PropertyReader> generated =
        proxy(
            environment().findClass(PropertyReader.class),
            (instance, method, arguments) -> {
              switch (method.name()) {
                case "read":
                  emitRead(properties, arguments[0], arguments[1]);
                  break;
                case "canRead":
                  Value<Object> candidate = arguments[0];
                  exit(() -> readableType.isInstance(candidate.get()));
                  break;
                case "names":
                  emitNames(names);
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

  private static void emitNames(List<String> names) {
    int count = names.size();
    Value<String[]> array = emit(() -> new String[count]);
    for (int index = 0; index < count; index++) {
      int position = index;
      String name = names.get(index);
      emit(() -> array.get()[position] = name);
    }
    exit(() -> array.get());
  }

  /** Collects the readable properties of a type, an accessor winning over a field of the same name. */
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

/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import io.instanto.mockatcha.internal.MockRuntime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the arguments a call was made with, so a test can assert on them afterwards.
 *
 * <pre>{@code
 * ArgumentCaptor<Timesheet> saved = ArgumentCaptor.forClass(Timesheet.class);
 *
 * verify(repository).save(saved.capture());
 *
 * assertEquals("A-17", saved.getValue().employeeId());
 * }</pre>
 *
 * <p>{@link #capture()} is a matcher, so the usual rule applies: if one argument of a call uses a
 * matcher, every argument must use one.
 */
public final class ArgumentCaptor<T> {

  private final List<T> captured = new ArrayList<>();
  private final Class<?> type;

  private ArgumentCaptor(Class<?> type) {
    this.type = type;
  }

  /**
   * Creates a captor for a type.
   *
   * <p>Required when capturing an argument of a primitive parameter.
   */
  public static <T> ArgumentCaptor<T> forClass(Class<T> type) {
    return new ArgumentCaptor<>(type);
  }

  /** Creates a captor whose type comes from the variable it is assigned to. */
  public static <T> ArgumentCaptor<T> captor() {
    return new ArgumentCaptor<>(null);
  }

  /**
   * Matches any argument and keeps it. Write this where the argument would go.
   *
   * <p>Values accumulate across verifications.
   */
  @SuppressWarnings("unchecked")
  public T capture() {
    MockRuntime.registerMatcher(
        argument -> true, "capture()", argument -> captured.add((T) argument));
    return (T) emptyValueFor(type);
  }

  /** The most recently captured argument. */
  public T getValue() {
    if (captured.isEmpty()) {
      throw new IllegalStateException(
          "No argument was captured. Check that capture() was passed to a verified or stubbed"
              + " call, and that the call actually happened.");
    }
    return captured.get(captured.size() - 1);
  }

  /** Every captured argument, oldest first. */
  public List<T> getAllValues() {
    return Collections.unmodifiableList(new ArrayList<>(captured));
  }

  /** Forgets everything captured so far, so one captor can serve two verifications. */
  public void clear() {
    captured.clear();
  }

  private static final Map<Class<?>, Object> DEFAULT_VALUES = createDefaultValues();

  private static Map<Class<?>, Object> createDefaultValues() {
    Map<Class<?>, Object> map = new HashMap<>();
    map.put(Boolean.class, Boolean.FALSE);
    map.put(boolean.class, Boolean.FALSE);
    map.put(Byte.class, (byte) 0);
    map.put(byte.class, (byte) 0);
    map.put(Short.class, (short) 0);
    map.put(short.class, (short) 0);
    map.put(Character.class, '\0');
    map.put(char.class, '\0');
    map.put(Integer.class, 0);
    map.put(int.class, 0);
    map.put(Long.class, 0L);
    map.put(long.class, 0L);
    map.put(Float.class, 0F);
    map.put(float.class, 0F);
    map.put(Double.class, 0D);
    map.put(double.class, 0D);
    return Collections.unmodifiableMap(map);
  }

  // capture() stands in for an argument, so a primitive parameter needs something that unboxes.
  // The value is never used; the matcher accepts whatever the call really passed.
  private static Object emptyValueFor(Class<?> type) {
    return type == null ? null : DEFAULT_VALUES.get(type);
  }

  @Override
  public String toString() {
    return "ArgumentCaptor" + captured;
  }
}

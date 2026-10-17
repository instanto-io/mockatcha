/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

import io.instanto.mockatcha.bdd.jvm.JvmObjectMatchers;
import io.instanto.mockatcha.bdd.teavm.TeaVmObjectMatchers;
import java.util.Map;
import org.teavm.interop.PlatformMarker;

/**
 * Matches an object by naming some of its properties and ignoring the rest.
 *
 * <p>Under TeaVM the property reader is generated while TeaVM compiles the test, so nothing is
 * looked up reflectively in the browser.
 *
 * <pre>{@code
 * verify(repository).save(objectContaining(Timesheet.class, "employeeId", "A-17"));
 *
 * verify(audit).log(objectContaining(Timesheet.class,
 *         Map.of("employeeId", "A-17", "hours", 38)));
 * }</pre>
 *
 * <p>The class must be a compile-time constant. A property is a no-argument accessor such as {@code
 * hours()}, {@code getHours()}, or {@code isActive()}, or a visible field. Accessors and fields may
 * be declared or inherited. An unknown property name fails when the matcher is built.
 */
public final class ObjectMatchers {

  private ObjectMatchers() {}

  @PlatformMarker
  private static boolean onTeaVm() {
    return false;
  }

  /** Matches an object of this type whose named property equals a value. */
  public static <T> T objectContaining(Class<T> type, String property, Object expected) {
    if (onTeaVm()) {
      return TeaVmObjectMatchers.objectContaining(type, property, expected);
    }
    return JvmObjectMatchers.objectContaining(type, property, expected);
  }

  /** Matches an object of this type whose named properties all equal their values. */
  public static <T> T objectContaining(Class<T> type, Map<String, Object> properties) {
    if (onTeaVm()) {
      return TeaVmObjectMatchers.objectContaining(type, properties);
    }
    return JvmObjectMatchers.objectContaining(type, properties);
  }

  /**
   * Builds a reader for a type, for inspecting a value directly.
   *
   * <pre>{@code
   * PropertyReader<Timesheet> reader = propertiesOf(Timesheet.class);
   * assertEquals("A-17", reader.read(saved.getValue(), "employeeId"));
   * }</pre>
   */
  public static <T> PropertyReader<T> propertiesOf(Class<T> type) {
    if (onTeaVm()) {
      return TeaVmObjectMatchers.propertiesOf(type);
    }
    return JvmObjectMatchers.propertiesOf(type);
  }
}

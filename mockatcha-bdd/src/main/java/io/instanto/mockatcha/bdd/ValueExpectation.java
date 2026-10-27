/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

import io.instanto.mockatcha.Mockatcha;
import io.instanto.mockatcha.spi.MockAccess;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Fluent Jasmine-style expectation on any value or mock.
 *
 * <pre>{@code
 * expect(result.hours()).toBe(38);
 * expect(result.reviewRequired()).toBeFalse();
 * expect(result.notes()).toContain("timesheet");
 * expect(timesheets).toHaveNoInteractions();
 * }</pre>
 */
public class ValueExpectation<T> {

  private static final String OTHER_REQUIRED = "other";

  private final T actual;
  private final boolean inverted;

  ValueExpectation(T actual, boolean inverted) {
    this.actual = actual;
    this.inverted = inverted;
  }

  /** Inverts the expectation. */
  public ValueExpectation<T> not() {
    return new ValueExpectation<>(actual, !inverted);
  }

  /** The actual value wrapped by this expectation. */
  public T getActual() {
    return actual;
  }

  /** Expects the value to be equal to {@code expected} via {@link Objects#equals}. */
  public ValueExpectation<T> toBe(Object expected) {
    boolean equals = Objects.equals(actual, expected);
    if (!inverted && !equals) {
      throw new AssertionError("Expected: <" + expected + "> but was: <" + actual + ">");
    }
    if (inverted && equals) {
      throw new AssertionError("Expected value not to be: <" + expected + ">");
    }
    return this;
  }

  /** Expects deep equality via {@link Objects#deepEquals}. */
  public ValueExpectation<T> toEqual(Object expected) {
    boolean equals = Objects.deepEquals(actual, expected);
    if (!inverted && !equals) {
      throw new AssertionError(
          "Expected deep equality: <" + expected + "> but was: <" + actual + ">");
    }
    if (inverted && equals) {
      throw new AssertionError("Expected value not to deep-equal: <" + expected + ">");
    }
    return this;
  }

  /** Expects the value to be {@code null}. */
  public ValueExpectation<T> toBeNull() {
    boolean isNull = actual == null;
    if (!inverted && !isNull) {
      throw new AssertionError("Expected null but was: <" + actual + ">");
    }
    if (inverted && isNull) {
      throw new AssertionError("Expected non-null value but was: null");
    }
    return this;
  }

  /** Expects the value to not be {@code null}. */
  public ValueExpectation<T> toBeNotNull() {
    return not().toBeNull();
  }

  /** Expects the value to be {@code Boolean.TRUE}. */
  public ValueExpectation<T> toBeTrue() {
    boolean isTrue = Boolean.TRUE.equals(actual);
    if (!inverted && !isTrue) {
      throw new AssertionError("Expected true but was: <" + actual + ">");
    }
    if (inverted && isTrue) {
      throw new AssertionError("Expected value not to be true");
    }
    return this;
  }

  /** Expects the value to be {@code Boolean.FALSE}. */
  public ValueExpectation<T> toBeFalse() {
    boolean isFalse = Boolean.FALSE.equals(actual);
    if (!inverted && !isFalse) {
      throw new AssertionError("Expected false but was: <" + actual + ">");
    }
    if (inverted && isFalse) {
      throw new AssertionError("Expected value not to be false");
    }
    return this;
  }

  /**
   * Expects a truthy value (non-null, not Boolean.FALSE, non-zero number, non-empty
   * text/collection).
   */
  public ValueExpectation<T> toBeTruthy() {
    boolean truthy = isTruthy(actual);
    if (!inverted && !truthy) {
      throw new AssertionError("Expected truthy value but was: <" + actual + ">");
    }
    if (inverted && truthy) {
      throw new AssertionError("Expected falsy value but was: <" + actual + ">");
    }
    return this;
  }

  /** Expects a falsy value (null, Boolean.FALSE, zero, or empty text/collection). */
  public ValueExpectation<T> toBeFalsy() {
    return not().toBeTruthy();
  }

  /** Expects the value to be an instance of {@code expectedType}. */
  public ValueExpectation<T> toBeInstanceOf(Class<?> expectedType) {
    Objects.requireNonNull(expectedType, "expectedType");
    boolean instance = actual != null && expectedType.isInstance(actual);
    if (!inverted && !instance) {
      String actualType = actual == null ? "null" : actual.getClass().getName();
      throw new AssertionError(
          "Expected instance of <" + expectedType.getName() + "> but was: <" + actualType + ">");
    }
    if (inverted && instance) {
      throw new AssertionError(
          "Expected value not to be instance of <" + expectedType.getName() + ">");
    }
    return this;
  }

  /** Expects a string to contain a substring, or a collection/array/map to contain an item/key. */
  public ValueExpectation<T> toContain(Object item) {
    boolean contained = containsItem(actual, item);
    if (!inverted && !contained) {
      throw new AssertionError("Expected <" + actual + "> to contain <" + item + ">");
    }
    if (inverted && contained) {
      throw new AssertionError("Expected <" + actual + "> not to contain <" + item + ">");
    }
    return this;
  }

  /** Expects a string, array, collection, or map to have the given size/length. */
  public ValueExpectation<T> toHaveSize(int expectedSize) {
    if (expectedSize < 0) {
      throw new IllegalArgumentException("Expected size must not be negative: " + expectedSize);
    }
    int actualSize = sizeOf(actual);
    if (actualSize < 0) {
      throw new IllegalArgumentException(
          "Cannot determine size of <"
              + (actual == null ? "null" : actual.getClass().getName())
              + ">");
    }
    if (!inverted && actualSize != expectedSize) {
      throw new AssertionError(
          "Expected size: <"
              + expectedSize
              + "> but was: <"
              + actualSize
              + "> for <"
              + actual
              + ">");
    }
    if (inverted && actualSize == expectedSize) {
      throw new AssertionError(
          "Expected size not to be: <" + expectedSize + "> for <" + actual + ">");
    }
    return this;
  }

  /**
   * Expects a string, array, collection, or map to have the given length (synonym for toHaveSize).
   */
  public ValueExpectation<T> toHaveLength(int expectedLength) {
    return toHaveSize(expectedLength);
  }

  /** Expects a string, array, collection, or map to be empty (size or length of 0). */
  public ValueExpectation<T> toBeEmpty() {
    return toHaveSize(0);
  }

  /** Expects a string to match a regular expression. */
  public ValueExpectation<T> toMatch(String regex) {
    Objects.requireNonNull(regex, "regex");
    return toMatch(Pattern.compile(regex));
  }

  /** Expects a string to match a compiled regular expression {@link Pattern}. */
  public ValueExpectation<T> toMatch(Pattern pattern) {
    Objects.requireNonNull(pattern, "pattern");
    if (!(actual instanceof CharSequence text)) {
      throw new IllegalArgumentException(
          "toMatch() requires a CharSequence, but got: "
              + (actual == null ? "null" : actual.getClass().getName()));
    }
    boolean matches = pattern.matcher(text).find();
    if (!inverted && !matches) {
      throw new AssertionError(
          "Expected \"" + text + "\" to match pattern /" + pattern.pattern() + "/");
    }
    if (inverted && matches) {
      throw new AssertionError(
          "Expected \"" + text + "\" not to match pattern /" + pattern.pattern() + "/");
    }
    return this;
  }

  /** Expects a comparable value to be strictly greater than {@code other}. */
  @SuppressWarnings("unchecked")
  public ValueExpectation<T> toBeGreaterThan(Comparable<? super T> other) {
    Objects.requireNonNull(other, OTHER_REQUIRED);
    requireComparable(actual);
    int comparison = other.compareTo(actual);
    boolean condition = comparison < 0;
    if (!inverted && !condition) {
      throw new AssertionError("Expected <" + actual + "> to be greater than <" + other + ">");
    }
    if (inverted && condition) {
      throw new AssertionError("Expected <" + actual + "> not to be greater than <" + other + ">");
    }
    return this;
  }

  /** Expects a comparable value to be strictly less than {@code other}. */
  @SuppressWarnings("unchecked")
  public ValueExpectation<T> toBeLessThan(Comparable<? super T> other) {
    Objects.requireNonNull(other, OTHER_REQUIRED);
    requireComparable(actual);
    int comparison = other.compareTo(actual);
    boolean condition = comparison > 0;
    if (!inverted && !condition) {
      throw new AssertionError("Expected <" + actual + "> to be less than <" + other + ">");
    }
    if (inverted && condition) {
      throw new AssertionError("Expected <" + actual + "> not to be less than <" + other + ">");
    }
    return this;
  }

  /** Expects a comparable value to be greater than or equal to {@code other}. */
  @SuppressWarnings("unchecked")
  public ValueExpectation<T> toBeGreaterThanOrEqual(Comparable<? super T> other) {
    Objects.requireNonNull(other, OTHER_REQUIRED);
    requireComparable(actual);
    int comparison = other.compareTo(actual);
    boolean condition = comparison <= 0;
    if (!inverted && !condition) {
      throw new AssertionError(
          "Expected <" + actual + "> to be greater than or equal to <" + other + ">");
    }
    if (inverted && condition) {
      throw new AssertionError(
          "Expected <" + actual + "> not to be greater than or equal to <" + other + ">");
    }
    return this;
  }

  /** Expects a comparable value to be less than or equal to {@code other}. */
  @SuppressWarnings("unchecked")
  public ValueExpectation<T> toBeLessThanOrEqual(Comparable<? super T> other) {
    Objects.requireNonNull(other, OTHER_REQUIRED);
    requireComparable(actual);
    int comparison = other.compareTo(actual);
    boolean condition = comparison >= 0;
    if (!inverted && !condition) {
      throw new AssertionError(
          "Expected <" + actual + "> to be less than or equal to <" + other + ">");
    }
    if (inverted && condition) {
      throw new AssertionError(
          "Expected <" + actual + "> not to be less than or equal to <" + other + ">");
    }
    return this;
  }

  /**
   * Expects this mock or spy to have recorded zero invocations.
   *
   * @throws IllegalArgumentException if this value is not a Mockatcha mock or spy
   */
  public ValueExpectation<T> toHaveNoInteractions() {
    requireMock("toHaveNoInteractions");
    List<?> calls = MockAccess.invocations(actual);
    boolean noInteractions = calls.isEmpty();
    if (!inverted && !noInteractions) {
      throw new AssertionError(
          "Expected no interactions on "
              + MockAccess.description(actual)
              + ", but recorded "
              + calls.size()
              + " call(s): "
              + calls);
    }
    if (inverted && noInteractions) {
      throw new AssertionError(
          "Expected at least one interaction on "
              + MockAccess.description(actual)
              + ", but none was recorded");
    }
    return this;
  }

  /**
   * Expects every invocation on this mock to have been verified already.
   *
   * @throws IllegalArgumentException if this value is not a Mockatcha mock or spy
   */
  public ValueExpectation<T> toHaveNoMoreInteractions() {
    requireMock("toHaveNoMoreInteractions");
    if (!inverted) {
      Mockatcha.verifyNoMoreInteractions(actual);
    }
    return this;
  }

  private void requireMock(String operation) {
    if (!MockAccess.isMock(actual)) {
      throw new IllegalArgumentException(
          operation
              + "() requires an object created by Mockatcha.mock or Mockatcha.spy, but got: "
              + (actual == null ? "null" : actual.getClass().getName()));
    }
  }

  private static void requireComparable(Object value) {
    if (!(value instanceof Comparable)) {
      throw new IllegalArgumentException(
          "Comparison requires a Comparable value, but got: "
              + (value == null ? "null" : value.getClass().getName()));
    }
  }

  private static boolean isTruthy(Object value) {
    if (value == null || Boolean.FALSE.equals(value)) {
      return false;
    }
    if (value instanceof Number number) {
      return number.doubleValue() != 0.0;
    }
    if (value instanceof CharSequence text) {
      return !text.isEmpty();
    }
    if (value instanceof Collection<?> col) {
      return !col.isEmpty();
    }
    if (value instanceof Map<?, ?> map) {
      return !map.isEmpty();
    }
    if (value.getClass().isArray()) {
      return Array.getLength(value) > 0;
    }
    return true;
  }

  private static int sizeOf(Object value) {
    if (value instanceof CharSequence text) {
      return text.length();
    }
    if (value instanceof Collection<?> col) {
      return col.size();
    }
    if (value instanceof Map<?, ?> map) {
      return map.size();
    }
    if (value != null && value.getClass().isArray()) {
      return Array.getLength(value);
    }
    return -1;
  }

  private static boolean containsItem(Object container, Object item) {
    if (container == null) {
      return false;
    }
    if (container instanceof CharSequence text) {
      return item != null && text.toString().contains(item.toString());
    }
    if (container instanceof Collection<?> col) {
      return col.contains(item);
    }
    if (container instanceof Map<?, ?> map) {
      return map.containsKey(item);
    }
    if (container.getClass().isArray()) {
      int len = Array.getLength(container);
      for (int i = 0; i < len; i++) {
        if (Objects.deepEquals(Array.get(container, i), item)) {
          return true;
        }
      }
      return false;
    }
    throw new IllegalArgumentException(
        "Cannot check toContain() on type: " + container.getClass().getName());
  }
}

/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

import io.instanto.mockatcha.ArgumentMatcher;
import io.instanto.mockatcha.Invocation;
import io.instanto.mockatcha.spi.MockAccess;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Fluent Jasmine-style expectation on a Mockatcha {@link Spy} or method on a mock.
 *
 * <pre>{@code
 * expect(pricing, "currency").toHaveBeenCalled();
 * expect(pricing, "total").toHaveBeenCalledWith("A-17");
 * expect(pricing, "total").toHaveBeenCalledTimes(2);
 * expect(pricing, "currency").not().toHaveBeenCalled();
 * }</pre>
 */
public final class SpyExpectation {

  private final Object mock;
  private final String methodName;
  private final Spy spy;
  private final boolean inverted;

  SpyExpectation(Object mock, String methodName, boolean inverted) {
    this.mock = Objects.requireNonNull(mock, "mock");
    this.methodName = Objects.requireNonNull(methodName, "methodName");
    this.spy = null;
    this.inverted = inverted;
  }

  SpyExpectation(Spy spy, boolean inverted) {
    this.mock = null;
    this.methodName = null;
    this.spy = Objects.requireNonNull(spy, "spy");
    this.inverted = inverted;
  }

  /** Inverts the expectation. */
  public SpyExpectation not() {
    return spy != null
        ? new SpyExpectation(spy, !inverted)
        : new SpyExpectation(mock, methodName, !inverted);
  }

  /** Expects the method to have been called at least once. */
  public SpyExpectation toHaveBeenCalled() {
    List<Invocation> calls = invocations();
    boolean called = !calls.isEmpty();
    if (!inverted && !called) {
      throw new AssertionError(
          "Expected " + description() + " to have been called, but it was not called");
    }
    if (inverted && called) {
      throw new AssertionError(
          "Expected "
              + description()
              + " not to have been called, but it was called "
              + calls.size()
              + " time(s)");
    }
    return this;
  }

  /** Expects the method to have been called exactly this many times. */
  public SpyExpectation toHaveBeenCalledTimes(int expectedCount) {
    if (expectedCount < 0) {
      throw new IllegalArgumentException("Expected count must not be negative: " + expectedCount);
    }
    List<Invocation> calls = invocations();
    int actualCount = calls.size();
    if (!inverted && actualCount != expectedCount) {
      throw new AssertionError(
          "Expected "
              + description()
              + " to have been called "
              + expectedCount
              + " time(s), but was called "
              + actualCount
              + " time(s)");
    }
    if (inverted && actualCount == expectedCount) {
      throw new AssertionError(
          "Expected " + description() + " not to have been called " + expectedCount + " time(s)");
    }
    return this;
  }

  /**
   * Expects at least one call to have been made with matching arguments.
   *
   * <p>Arguments can be literal values (compared via deep equality) or {@link ArgumentMatcher}s.
   */
  public SpyExpectation toHaveBeenCalledWith(Object... expectedArgs) {
    List<Invocation> calls = invocations();
    boolean matched = false;
    for (Invocation call : calls) {
      if (matchesArgs(call, expectedArgs)) {
        matched = true;
        break;
      }
    }
    if (!inverted && !matched) {
      throw new AssertionError(
          "Expected "
              + description()
              + " to have been called with "
              + formatArgs(expectedArgs)
              + ",\nbut recorded calls were:"
              + formatCalls(calls));
    }
    if (inverted && matched) {
      throw new AssertionError(
          "Expected "
              + description()
              + " not to have been called with "
              + formatArgs(expectedArgs)
              + ", but a matching call was found");
    }
    return this;
  }

  /** Expects the most recent call to have been made with matching arguments. */
  public SpyExpectation toHaveBeenLastCalledWith(Object... expectedArgs) {
    List<Invocation> calls = invocations();
    if (calls.isEmpty()) {
      if (!inverted) {
        throw new AssertionError(
            "Expected "
                + description()
                + " to have been called with "
                + formatArgs(expectedArgs)
                + ", but it was never called");
      }
      return this;
    }
    Invocation last = calls.get(calls.size() - 1);
    boolean matched = matchesArgs(last, expectedArgs);
    if (!inverted && !matched) {
      throw new AssertionError(
          "Expected most recent call of "
              + description()
              + " to be with "
              + formatArgs(expectedArgs)
              + ", but was with "
              + last.arguments());
    }
    if (inverted && matched) {
      throw new AssertionError(
          "Expected most recent call of "
              + description()
              + " not to be with "
              + formatArgs(expectedArgs));
    }
    return this;
  }

  /**
   * Expects the nth call (1-based index) to have been made with matching arguments.
   *
   * @param nth 1-based call index (1 = first call)
   * @param expectedArgs expected arguments
   */
  public SpyExpectation toHaveBeenNthCalledWith(int nth, Object... expectedArgs) {
    if (nth < 1) {
      throw new IllegalArgumentException("Call index must be 1 or greater: " + nth);
    }
    List<Invocation> calls = invocations();
    if (nth > calls.size()) {
      if (!inverted) {
        throw new AssertionError(
            "Wanted call "
                + nth
                + " of "
                + description()
                + ", but only "
                + calls.size()
                + " call(s) were recorded");
      }
      return this;
    }
    Invocation target = calls.get(nth - 1);
    boolean matched = matchesArgs(target, expectedArgs);
    if (!inverted && !matched) {
      throw new AssertionError(
          "Expected call "
              + nth
              + " of "
              + description()
              + " to be with "
              + formatArgs(expectedArgs)
              + ", but was with "
              + target.arguments());
    }
    if (inverted && matched) {
      throw new AssertionError(
          "Expected call "
              + nth
              + " of "
              + description()
              + " not to be with "
              + formatArgs(expectedArgs));
    }
    return this;
  }

  private List<Invocation> invocations() {
    if (spy != null) {
      return spy.calls().all();
    }
    return MockAccess.invocations(mock, methodName);
  }

  private String description() {
    if (spy != null) {
      return spy.toString();
    }
    return methodName + "() on " + MockAccess.description(mock);
  }

  static boolean matchesArgs(Invocation invocation, Object[] expectedArgs) {
    if (invocation.argumentCount() != expectedArgs.length) {
      return false;
    }
    for (int i = 0; i < expectedArgs.length; i++) {
      Object expected = expectedArgs[i];
      Object actual = invocation.argument(i);
      if (expected instanceof ArgumentMatcher<?> matcher) {
        @SuppressWarnings("unchecked")
        ArgumentMatcher<Object> objMatcher = (ArgumentMatcher<Object>) matcher;
        if (!objMatcher.matches(actual)) {
          return false;
        }
      } else if (!Objects.deepEquals(expected, actual)) {
        return false;
      }
    }
    return true;
  }

  private static String formatArgs(Object[] args) {
    if (args == null || args.length == 0) {
      return "[]";
    }
    return Arrays.toString(args);
  }

  private static String formatCalls(List<Invocation> calls) {
    if (calls.isEmpty()) {
      return "\n  (no calls recorded)";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < calls.size(); i++) {
      sb.append("\n  ").append(i + 1).append(". ").append(calls.get(i).arguments());
    }
    return sb.toString();
  }
}

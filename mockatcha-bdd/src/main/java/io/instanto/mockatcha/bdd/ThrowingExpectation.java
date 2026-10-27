/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

import java.util.Objects;

/**
 * Fluent expectation on an action that may throw an exception.
 *
 * <pre>{@code
 * expect(() -> service.submit(null)).toThrow(IllegalArgumentException.class);
 * expect(() -> service.submit("A-17")).not().toThrow();
 * }</pre>
 */
public final class ThrowingExpectation {

  private final ThrowingRunnable action;
  private final boolean inverted;

  ThrowingExpectation(ThrowingRunnable action, boolean inverted) {
    this.action = Objects.requireNonNull(action, "action");
    this.inverted = inverted;
  }

  /** Inverts the expectation. */
  public ThrowingExpectation not() {
    return new ThrowingExpectation(action, !inverted);
  }

  /**
   * Expects the action to throw an exception.
   *
   * @return the thrown exception, or {@code null} if inverted and nothing was thrown
   */
  public Throwable toThrow() {
    Throwable thrown = execute();
    if (!inverted && thrown == null) {
      throw new AssertionError("Expected action to throw an exception, but it completed normally");
    }
    if (inverted && thrown != null) {
      throw new AssertionError(
          "Expected action not to throw an exception, but it threw "
              + thrown.getClass().getName()
              + ": "
              + thrown.getMessage(),
          thrown);
    }
    return thrown;
  }

  /**
   * Expects the action to throw an exception of the specified type.
   *
   * @param expectedType the expected exception class
   * @return the thrown exception cast to the expected type
   */
  @SuppressWarnings("unchecked")
  public <E extends Throwable> E toThrow(Class<E> expectedType) {
    Objects.requireNonNull(expectedType, "expectedType");
    Throwable thrown = execute();
    if (!inverted) {
      if (thrown == null) {
        throw new AssertionError(
            "Expected action to throw " + expectedType.getName() + ", but it completed normally");
      }
      if (!expectedType.isInstance(thrown)) {
        throw new AssertionError(
            "Expected action to throw "
                + expectedType.getName()
                + ", but it threw "
                + thrown.getClass().getName()
                + ": "
                + thrown.getMessage(),
            thrown);
      }
      return (E) thrown;
    }
    if (thrown != null && expectedType.isInstance(thrown)) {
      throw new AssertionError(
          "Expected action not to throw " + expectedType.getName() + ", but it did", thrown);
    }
    return null;
  }

  /**
   * Expects the action to throw an exception whose message contains the given substring.
   *
   * @param messageSubstring the substring expected in the exception message
   * @return the thrown exception
   */
  public Throwable toThrow(String messageSubstring) {
    Objects.requireNonNull(messageSubstring, "messageSubstring");
    Throwable thrown = toThrow();
    if (!inverted) {
      if (thrown == null) {
        throw new AssertionError(
            "Expected action to throw an exception, but it completed normally");
      }
      String msg = thrown.getMessage();
      if (msg == null || !msg.contains(messageSubstring)) {
        throw new AssertionError(
            "Expected exception message to contain \""
                + messageSubstring
                + "\", but was: \""
                + msg
                + "\"",
            thrown);
      }
    } else if (thrown != null) {
      String msg = thrown.getMessage();
      if (msg != null && msg.contains(messageSubstring)) {
        throw new AssertionError(
            "Expected exception message not to contain \""
                + messageSubstring
                + "\", but was: \""
                + msg
                + "\"",
            thrown);
      }
    }
    return thrown;
  }

  /**
   * Expects the action to throw an exception of the specified type with a message containing the
   * given substring.
   */
  public <E extends Throwable> E toThrow(Class<E> expectedType, String messageSubstring) {
    E thrown = toThrow(expectedType);
    if (!inverted) {
      if (thrown == null) {
        throw new AssertionError(
            "Expected action to throw " + expectedType.getName() + ", but it completed normally");
      }
      String msg = thrown.getMessage();
      if (msg == null || !msg.contains(messageSubstring)) {
        throw new AssertionError(
            "Expected "
                + expectedType.getSimpleName()
                + " message to contain \""
                + messageSubstring
                + "\", but was: \""
                + msg
                + "\"",
            thrown);
      }
    }
    return thrown;
  }

  private Throwable execute() {
    try {
      action.run();
      return null;
    } catch (Throwable t) {
      return t;
    }
  }
}

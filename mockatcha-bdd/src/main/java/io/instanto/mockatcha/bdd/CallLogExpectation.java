/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

import java.util.Objects;

/**
 * Fluent expectation on a {@link CallLog}.
 *
 * <pre>{@code
 * expect(calls(pricing, "currency")).toHaveCount(2);
 * expect(calls(pricing, "unknown")).toBeEmpty();
 * }</pre>
 */
public final class CallLogExpectation {

  private final CallLog calls;
  private final boolean inverted;

  CallLogExpectation(CallLog calls, boolean inverted) {
    this.calls = Objects.requireNonNull(calls, "calls");
    this.inverted = inverted;
  }

  /** Inverts the expectation. */
  public CallLogExpectation not() {
    return new CallLogExpectation(calls, !inverted);
  }

  /** Expects the call log to have recorded exactly this many matching calls. */
  public CallLogExpectation toHaveCount(int expectedCount) {
    if (expectedCount < 0) {
      throw new IllegalArgumentException("Expected count must not be negative: " + expectedCount);
    }
    int actualCount = calls.count();
    if (!inverted && actualCount != expectedCount) {
      throw new AssertionError(
          "Expected " + calls + " to have count " + expectedCount + ", but was " + actualCount);
    }
    if (inverted && actualCount == expectedCount) {
      throw new AssertionError("Expected " + calls + " not to have count " + expectedCount);
    }
    return this;
  }

  /** Expects no calls to have been recorded. */
  public CallLogExpectation toBeEmpty() {
    return toHaveCount(0);
  }
}

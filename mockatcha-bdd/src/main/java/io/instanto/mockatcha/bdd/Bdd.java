/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

import io.instanto.mockatcha.spi.MockAccess;
import java.util.Objects;

/** Method-name stubbing and call inspection for Mockatcha tests. */
public final class Bdd {

  private Bdd() {}

  /**
   * Selects one method of an existing mock or spy by name.
   *
   * <p>Covers every overload of that name unless {@link Spy#withArgs} narrows it. The object must
   * already be a Mockatcha mock or spy.
   *
   * <pre>{@code
   * PricingService pricing = spy(PricingService.class, real);
   * spyOn(pricing, "currency").andReturn("EUR");
   * assertEquals(1, spyOn(pricing, "currency").calls().count());
   * }</pre>
   */
  public static Spy spyOn(Object mock, String methodName) {
    requireMock(mock, "spyOn");
    return new Spy(mock, Objects.requireNonNull(methodName, "methodName"), null);
  }

  /** Reads every call recorded on a mock or spy. */
  public static CallLog calls(Object mock) {
    requireMock(mock, "calls");
    return new CallLog(mock, null, MockAccess.invocations(mock), MockAccess.description(mock));
  }

  /** Reads the calls recorded for one method name. */
  public static CallLog calls(Object mock, String methodName) {
    requireMock(mock, "calls");
    Objects.requireNonNull(methodName, "methodName");
    return new CallLog(mock, methodName, MockAccess.invocations(mock, methodName), methodName);
  }

  /** Begins a fluent expectation on any value or mock. */
  public static <T> ValueExpectation<T> expect(T actual) {
    return new ValueExpectation<>(actual, false);
  }

  /** Begins a fluent expectation on a call log. */
  public static CallLogExpectation expect(CallLog calls) {
    return new CallLogExpectation(calls, false);
  }

  /** Begins a fluent expectation on a method spy. */
  public static SpyExpectation expect(Spy spy) {
    return new SpyExpectation(spy, false);
  }

  /** Begins a fluent expectation on one method of a mock. */
  public static SpyExpectation expect(Object mock, String methodName) {
    requireMock(mock, "expect");
    return new SpyExpectation(mock, methodName, false);
  }

  /** Begins a fluent expectation on an action that may throw an exception. */
  public static ThrowingExpectation expect(ThrowingRunnable action) {
    return new ThrowingExpectation(action, false);
  }

  /** Starts a Given-When-Then BDD scenario flow with a title. */
  public static Scenario scenario(String title) {
    return Scenario.scenario(title);
  }

  private static void requireMock(Object mock, String operation) {
    if (!MockAccess.isMock(mock)) {
      throw new IllegalArgumentException(
          operation
              + "() needs an object created by Mockatcha.mock or Mockatcha.spy, because TeaVM"
              + " cannot replace a method on an object that already exists");
    }
  }
}

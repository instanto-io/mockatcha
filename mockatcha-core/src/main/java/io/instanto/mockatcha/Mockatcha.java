/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static org.teavm.metaprogramming.Metaprogramming.currentLocation;

import io.instanto.mockatcha.internal.CountingMode;
import io.instanto.mockatcha.internal.InOrderImpl;
import io.instanto.mockatcha.internal.LenientStubberImpl;
import io.instanto.mockatcha.internal.MockRuntime;
import io.instanto.mockatcha.internal.StubberImpl;
import io.instanto.mockatcha.jvm.JvmMocks;
import io.instanto.mockatcha.teavm.TeaVmMocks;
import org.teavm.interop.PlatformMarker;

/** Entry point for mocking on the JVM and in TeaVM, following Mockito's API. */
public final class Mockatcha {

  private Mockatcha() {}

  // TeaVM replaces this call with true and removes the JVM branches that follow it before it
  // analyses dependencies, so the JVM mock factory never reaches a TeaVM build.
  @PlatformMarker
  private static boolean onTeaVm() {
    return false;
  }

  /**
   * Generates a mock implementation of an interface or an ordinary class.
   *
   * <p>The class must be a compile-time constant. A mocked class must be non-final and have a
   * no-argument constructor, which is run. Static, private, and final methods keep their real
   * behaviour.
   */
  public static <T> T mock(Class<T> type) {
    if (onTeaVm()) {
      return TeaVmMocks.mock(type);
    }
    return JvmMocks.mock(type);
  }

  /** Generates a mock whose name is used in diagnostics. */
  public static <T> T mock(Class<T> type, String name) {
    if (onTeaVm()) {
      return TeaVmMocks.mock(type, name);
    }
    return JvmMocks.mock(type, name);
  }

  /**
   * Generates a spy that records calls and hands unstubbed ones to {@code delegate}.
   *
   * <p>The spy is a separate object from {@code delegate}, so a real method calling another method
   * on itself reaches the delegate rather than the spy.
   */
  public static <T> T spy(Class<T> type, T delegate) {
    if (onTeaVm()) {
      return TeaVmMocks.spy(type, delegate);
    }
    return JvmMocks.spy(type, delegate);
  }

  /** Generates a named spy whose name is used in diagnostics. */
  public static <T> T spy(Class<T> type, T delegate, String name) {
    if (onTeaVm()) {
      return TeaVmMocks.spy(type, delegate, name);
    }
    return JvmMocks.spy(type, delegate, name);
  }

  /**
   * Opens a framework-neutral lifecycle that validates and releases its mocks.
   *
   * <p>Intended for custom or non-JUnit harnesses. JUnit tests should normally use
   * {@link MockatchaRule}. Makes unused-stub checks on close without enabling fail-fast strict
   * stubbing.
   */
  public static MockatchaSession session() {
    return session(false);
  }

  /** Opens a lifecycle, optionally failing immediately when a call narrowly misses a stub. */
  public static MockatchaSession session(boolean strictStubs) {
    return new MockatchaSession(strictStubs);
  }

  /**
   * Releases the mocks this thread created outside a session, and any part-finished stubbing or
   * verification, without checking them.
   *
   * <p>Tests that create mocks without {@link MockatchaRule} or a {@link MockatchaSession} leave
   * them for whatever checks next, which on a JVM is the following test class, since one process
   * runs the whole suite. Call this after such a test so what it left is not reported against
   * another. A session's own mocks are untouched.
   */
  public static void releaseMocks() {
    MockRuntime.releaseUnchecked();
  }

  /** Begins configuration of the most recently evaluated mock invocation. */
  public static <T> OngoingStubbing<T> when(T ignored) {
    return MockRuntime.when();
  }

  /** Arranges a result without evaluating the call. */
  public static Stubber doReturn(Object value) {
    return new StubberImpl().doReturn(value);
  }

  /** Arranges a failure without evaluating the call. */
  public static Stubber doThrow(Throwable throwable) {
    return new StubberImpl().doThrow(throwable);
  }

  /** Arranges a calculated result without evaluating the call. */
  public static Stubber doAnswer(Answer<?> answer) {
    return new StubberImpl().doAnswer(answer);
  }

  /** Arranges a call to do nothing, suppressing a spy's real method. */
  public static Stubber doNothing() {
    return new StubberImpl().doNothing();
  }

  /** Verifies that the following mock invocation occurred exactly once. */
  public static <T> T verify(T mock) {
    return verify(mock, times(1));
  }

  /** Verifies the following mock invocation using the supplied count rule. */
  public static <T> T verify(T mock, VerificationMode mode) {
    MockRuntime.beginVerification(mock, mode);
    return mock;
  }

  /** Expects exactly this many matching calls. */
  public static VerificationMode times(int expectedCount) {
    return CountingMode.exact(expectedCount);
  }

  /** Expects no matching call. */
  public static VerificationMode never() {
    return CountingMode.exact(0);
  }

  /** Expects at least this many matching calls, and does not care how many more. */
  public static VerificationMode atLeast(int minimumCount) {
    return CountingMode.atLeast(minimumCount);
  }

  /** Expects the call to have happened, without fixing how often. */
  public static VerificationMode atLeastOnce() {
    return CountingMode.atLeast(1);
  }

  /** Expects no more than this many matching calls, including none at all. */
  public static VerificationMode atMost(int maximumCount) {
    return CountingMode.atMost(maximumCount);
  }

  /** Expects this call once, and nothing else on the same mock. */
  public static VerificationMode only() {
    return CountingMode.only();
  }

  /**
   * Verifies calls in the order they happened, across these mocks.
   *
   * <pre>{@code
   * InOrder order = inOrder(repository, notifications);
   * order.verify(repository).save(report);
   * order.verify(notifications).send("saved");
   * }</pre>
   */
  public static InOrder inOrder(Object... mocks) {
    return new InOrderImpl(mocks);
  }

  /**
   * Expects every call on these mocks to have been verified already.
   *
   * <p>Use after the verifications a test cares about, to say that nothing else happened.
   */
  public static void verifyNoMoreInteractions(Object... mocks) {
    MockRuntime.verifyNoMoreInteractions(mocks);
  }

  /** Expects these mocks not to have been called at all. */
  public static void verifyNoInteractions(Object... mocks) {
    MockRuntime.verifyNoInteractions(mocks);
  }

  /**
   * Arranges a stub that {@link #validateStubbing} will not insist on.
   *
   * <pre>{@code
   * lenient().when(clock.now()).thenReturn(FIXED_TIME);
   * }</pre>
   */
  public static LenientStubber lenient() {
    return new LenientStubberImpl();
  }

  /**
   * Fails a call that found no arranged answer while the same method has one that did not match.
   *
   * <p>Off by default. {@link MockatchaRule}, {@link StrictTest}, and a strict
   * {@link MockatchaSession} turn it on for their test lifecycle.
   */
  public static void strictStubs(boolean strict) {
    MockRuntime.strictStubs(strict);
  }

  /**
   * Fails when a stub was arranged and no call ever matched it.
   *
   * <p>With no arguments, checks every mock created since the last such check, which scopes it to
   * one test when called from an {@code @After} method. Naming mocks explicitly checks only those.
   *
   * <p>The failure lists the calls that were made to the same method, which is usually where the
   * mismatch shows itself.
   */
  public static void validateStubbing(Object... mocks) {
    MockRuntime.validateStubbing(mocks);
  }

  /**
   * Fails when an argument matcher was created but never used by a call on a mock.
   *
   * <p>Useful in an {@code @After} method, where it reports the mistake against the test that
   * made it rather than the next one to run.
   */
  public static void validateUsage() {
    MockRuntime.validateUsage();
  }

  /** Returns read-only details about a mock or spy. */
  public static MockingDetails mockingDetails(Object mock) {
    return new MockingDetails(mock);
  }

  /** Forgets recorded calls while preserving stubs. */
  public static void clearInvocations(Object mock) {
    MockRuntime.clearInvocations(mock);
  }

  /** Forgets recorded calls and stubs. */
  public static void reset(Object mock) {
    MockRuntime.reset(mock);
  }
}

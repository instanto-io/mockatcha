/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.Answer;
import io.instanto.mockatcha.ArgumentMatcher;
import io.instanto.mockatcha.Invocation;
import io.instanto.mockatcha.OngoingStubbing;
import io.instanto.mockatcha.VerificationContext;
import io.instanto.mockatcha.VerificationMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Runtime called by TeaVM-generated mock implementations. */
public final class MockRuntime {

  /** Returned by dispatch when nothing is stubbed, which a stubbed null must not look like. */
  public static final Object REAL_CALL = new Object();

  private static final Map<Object, MockState> STATES = new IdentityHashMap<>();
  private static final List<RegisteredMatcher> MATCHERS = new ArrayList<>();
  private static final List<Object> UNCHECKED_MOCKS = new ArrayList<>();
  private static LastInvocation lastInvocation;
  private static VerificationRequest verification;
  private static StubbingRequest stubbing;
  private static boolean nextStubIsLenient;
  private static boolean strictStubs;
  private static SessionContext session;

  private MockRuntime() {}

  public static MockState createState(String description) {
    requireNoPendingMatchers("Creating a mock");
    return new MockState(description);
  }

  /** Validates a user-supplied mock name while retaining the generated type in the description. */
  public static String requireMockName(String name, String kind) {
    Objects.requireNonNull(name, "name");
    if (name.trim().isEmpty()) {
      throw new IllegalArgumentException("A mock name must not be blank");
    }
    return name + " (" + kind + ")";
  }

  /** Answers {@code toString} on a generated mock or spy. */
  public static String describe(MockState state) {
    return state.description();
  }

  static String descriptionOf(Object mock) {
    return requireState(mock).description();
  }

  /**
   * Reports whether a generated method should hand this call to the real object.
   *
   * <p>A mock has no delegate, so an unstubbed call on one is where strict stubbing looks for a
   * stub that was meant for it.
   */
  public static boolean callsReal(
      MockState state, String method, Object[] arguments, Object dispatchResult, Object delegate) {
    if (dispatchResult != REAL_CALL) {
      return false;
    }
    if (delegate != null) {
      return true;
    }
    requireNoNearMissStub(state, method, arguments);
    return false;
  }

  /** Turns on failing at the call when a stub for the same method was arranged but did not match. */
  public static void strictStubs(boolean strict) {
    strictStubs = strict;
  }

  /**
   * Fails a call that found no stub while the same method has one that did not match.
   *
   * <p>Almost always the stub and the call disagree about the arguments, and saying so at the call
   * points at the mistake rather than at whatever the empty value later broke.
   */
  private static void requireNoNearMissStub(MockState state, String method, Object[] arguments) {
    if (!strictStubs) {
      return;
    }
    int parenthesis = method.indexOf('(');
    String methodName = parenthesis < 0 ? method : method.substring(0, parenthesis);
    List<Stub> nearMisses = state.stubsOf(methodName);
    if (nearMisses.isEmpty()) {
      return;
    }

    StringBuilder message =
        new StringBuilder(state.description())
            .append(" was called with ")
            .append(new Invocation(state.mock(), method, arguments))
            .append(", which no arranged answer matched.")
            .append("\nThis method does have arranged answers for:");
    for (Stub stub : nearMisses) {
      message.append("\n  ").append(stub.pattern());
    }
    message.append(
        "\nEither the call or the stub has the wrong arguments."
            + " Use lenient() where a stub is deliberately narrow.");
    throw new AssertionError(message.toString());
  }

  /** Converts a dispatch result for a method that returns an object. */
  public static Object toObject(Object result) {
    return result == REAL_CALL ? null : result;
  }

  public static void register(Object mock, MockState state) {
    Objects.requireNonNull(mock, "mock");
    Objects.requireNonNull(state, "state");
    state.attach(mock);
    STATES.put(mock, state);
    if (session == null) {
      UNCHECKED_MOCKS.add(mock);
    } else {
      session.mocks.add(mock);
    }
  }

  /** Starts a non-nestable lifecycle, adopting fixture mocks created before test setup ran. */
  public static Object openSession(boolean strict) {
    if (session != null) {
      throw new IllegalStateException("Mockatcha sessions cannot be nested");
    }
    Object token = new Object();
    SessionContext opened = new SessionContext(token, strictStubs);
    opened.mocks.addAll(UNCHECKED_MOCKS);
    UNCHECKED_MOCKS.clear();
    session = opened;
    strictStubs = strict;
    return token;
  }

  /** Validates and releases every mock owned by a lifecycle, even when validation fails. */
  public static void closeSession(Object token) {
    SessionContext closing = session;
    if (closing == null || closing.token != token) {
      throw new IllegalStateException("This Mockatcha session is not active");
    }
    session = null;
    strictStubs = closing.previousStrictStubs;

    Throwable failure = null;
    try {
      validateStubbing(closing.mocks.toArray());
    } catch (Throwable thrown) {
      failure = thrown;
    }
    try {
      validateUsage();
    } catch (Throwable thrown) {
      failure = combine(failure, thrown);
    } finally {
      for (Object mock : closing.mocks) {
        MockState state = STATES.remove(mock);
        if (state != null) {
          state.release();
        }
      }
    }

    if (failure != null) {
      sneakyThrow(failure);
    }
  }

  /** Rejects a spy created without a delegate. */
  public static <T> T requireDelegate(T delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("spy() requires a real object to delegate to");
    }
    return delegate;
  }

  @SuppressWarnings("unchecked")
  public static void registerMatcher(ArgumentMatcher<?> matcher, String description) {
    MATCHERS.add(
        new RegisteredMatcher((ArgumentMatcher<Object>) matcher, description));
  }

  /** Registers a matcher whose side effect is committed only after the whole invocation matches. */
  @SuppressWarnings("unchecked")
  public static void registerMatcher(
      ArgumentMatcher<?> matcher, String description, Consumer<Object> onMatch) {
    MATCHERS.add(
        new RegisteredMatcher(
            (ArgumentMatcher<Object>) matcher,
            description,
            Objects.requireNonNull(onMatch, "onMatch")));
  }

  public static Object invokeObject(MockState state, String method, Object[] arguments) {
    Object result = dispatch(state, method, arguments);
    if (result == REAL_CALL) {
      requireNoNearMissStub(state, method, arguments);
      return null;
    }
    return result;
  }

  /** Dispatches a call on a generated spy, returning {@link #REAL_CALL} when nothing is stubbed. */
  public static Object dispatchSpy(MockState state, String method, Object[] arguments) {
    return dispatch(state, method, arguments);
  }


  public static boolean toBoolean(Object result) {
    return result != null && result != REAL_CALL && (Boolean) result;
  }

  public static byte toByte(Object result) {
    return isEmpty(result) ? 0 : ((Number) result).byteValue();
  }

  public static short toShort(Object result) {
    return isEmpty(result) ? 0 : ((Number) result).shortValue();
  }

  public static char toChar(Object result) {
    return isEmpty(result) ? '\0' : (Character) result;
  }

  public static int toInt(Object result) {
    return isEmpty(result) ? 0 : ((Number) result).intValue();
  }

  public static long toLong(Object result) {
    return isEmpty(result) ? 0L : ((Number) result).longValue();
  }

  public static float toFloat(Object result) {
    return isEmpty(result) ? 0F : ((Number) result).floatValue();
  }

  public static double toDouble(Object result) {
    return isEmpty(result) ? 0D : ((Number) result).doubleValue();
  }

  /** Answers {@code equals} on a generated mock or spy. */
  public static boolean sameInstance(Object mock, Object other) {
    return mock == other;
  }

  /** Answers {@code hashCode} on a generated mock or spy. */
  public static int identityHashCode(Object mock) {
    return System.identityHashCode(mock);
  }

  public static <T> OngoingStubbing<T> when() {
    requireNoPendingMatchers("when()");
    if (lastInvocation == null) {
      throw new IllegalStateException("when() requires a mock invocation");
    }
    LastInvocation captured = lastInvocation;
    lastInvocation = null;
    captured.state.remove(captured.invocation);
    return new OngoingStubbingImpl<>(newStub(captured.state, captured.pattern));
  }

  /** Marks the next stub as one no test needs to use. */
  public static void nextStubIsLenient() {
    nextStubIsLenient = true;
  }

  private static Stub newStub(MockState state, InvocationPattern pattern) {
    Stub stub = state.addStub(pattern);
    if (nextStubIsLenient) {
      nextStubIsLenient = false;
      stub.makeLenient();
    }
    return stub;
  }

  public static void beginVerification(Object mock, VerificationMode mode) {
    MockState state = requireState(mock);
    requireNoPendingMatchers("verify()");
    verification = new VerificationRequest(state, Objects.requireNonNull(mode, "mode"), null);
    lastInvocation = null;
  }

  /** Verifies the call that follows against an order rather than against a total count. */
  public static void beginOrderedVerification(
      Object mock, VerificationMode mode, InOrderImpl order) {
    MockState state = requireState(mock);
    requireNoPendingMatchers("verify()");
    verification =
        new VerificationRequest(state, Objects.requireNonNull(mode, "mode"), order);
    lastInvocation = null;
  }

  /** Arranges answers for the call that follows, without letting that call run. */
  public static void beginStubbing(Object mock, List<Answer<?>> answers) {
    MockState state = requireState(mock);
    requireNoPendingMatchers("when(mock)");
    stubbing = new StubbingRequest(state, new ArrayList<>(answers));
    lastInvocation = null;
  }

  public static boolean isMock(Object mock) {
    return mock != null && STATES.containsKey(mock);
  }

  public static List<Invocation> invocations(Object mock) {
    return requireState(mock).invocations();
  }

  public static void clearInvocations(Object mock) {
    requireState(mock).clearInvocations();
    lastInvocation = null;
  }

  /** Forgets the calls recorded for one method name, keeping the rest. */
  public static void clearInvocations(Object mock, String methodName) {
    requireState(mock).clearInvocations(Objects.requireNonNull(methodName, "methodName"));
    lastInvocation = null;
  }

  public static void reset(Object mock) {
    requireState(mock).reset();
    lastInvocation = null;
    verification = null;
    stubbing = null;
    MATCHERS.clear();
  }

  @SuppressWarnings("unchecked")
  public static <T, E extends Throwable> T sneakyThrow(Throwable throwable) throws E {
    throw (E) throwable;
  }

  private static Object dispatch(MockState state, String method, Object[] arguments) {
    state.requireActive();
    InvocationPattern pattern = consumePattern(method, arguments);

    if (stubbing != null) {
      applyStubbing(state, pattern);
      return null;
    }
    if (verification != null) {
      verify(state, pattern);
      return null;
    }

    Invocation invocation = new Invocation(state.mock(), method, arguments);
    state.record(invocation);
    lastInvocation = new LastInvocation(state, invocation, pattern);

    Stub stub = state.findStub(method, arguments);
    return stub == null ? REAL_CALL : stub.answer(invocation);
  }

  private static boolean isEmpty(Object result) {
    return result == null || result == REAL_CALL;
  }

  private static void applyStubbing(MockState state, InvocationPattern pattern) {
    StubbingRequest request = stubbing;
    stubbing = null;
    if (request.state != state) {
      throw new IllegalStateException(
          "The call after when(mock) must be made on the same mock");
    }
    Stub stub = newStub(state, pattern);
    for (Answer<?> answer : request.answers) {
      stub.add(answer);
    }
  }

  private static void verify(MockState state, InvocationPattern pattern) {
    VerificationRequest request = verification;
    verification = null;
    if (request.state != state) {
      throw new IllegalStateException("verify() must be followed by a call on the same mock");
    }
    if (request.order != null) {
      request.order.check(state.mock(), pattern, request.mode);
      return;
    }
    List<Invocation> matched = state.matching(pattern);
    request.mode.verify(
        new VerificationContext(
            matched.size(),
            state.invocationCount(),
            state.description() + ": " + pattern,
            state.invocations()));
    capture(pattern, matched);
    state.markVerified(matched);
  }

  static void capture(InvocationPattern pattern, List<Invocation> invocations) {
    for (Invocation invocation : invocations) {
      pattern.capture(invocation.arguments().toArray());
    }
  }

  /**
   * Rejects matchers that were registered but never consumed by a call on a mock.
   *
   * <p>A matcher passed to something that is not a mock would otherwise sit in the queue and be
   * applied to the next mock call instead.
   */
  private static void requireNoPendingMatchers(String operation) {
    if (MATCHERS.isEmpty()) {
      return;
    }
    List<RegisteredMatcher> pending = new ArrayList<>(MATCHERS);
    MATCHERS.clear();
    throw new IllegalStateException(
        operation + " found " + pending.size() + " argument matcher(s) left over: " + pending
            + ". A matcher belongs inside a call on a mock, as in verify(mock).save(any()).");
  }

  /** Records that a verification accounted for these calls. */
  public static void markVerified(Object mock, List<Invocation> matched) {
    requireState(mock).markVerified(matched);
  }

  /** Fails when any call on these mocks has not been accounted for by a verification. */
  public static void verifyNoMoreInteractions(Object... mocks) {
    for (Object mock : mocks) {
      List<Invocation> remaining = requireState(mock).unverified();
      if (!remaining.isEmpty()) {
        throw new AssertionError(
            stateDescription(mock) + ": wanted no further calls, but observed "
                + remaining.get(0) + "."
                + Failures.listOf(requireState(mock).invocations()));
      }
    }
  }

  /** Fails when anything at all was called on these mocks. */
  public static void verifyNoInteractions(Object... mocks) {
    for (Object mock : mocks) {
      List<Invocation> recorded = requireState(mock).invocations();
      if (!recorded.isEmpty()) {
        throw new AssertionError(
            stateDescription(mock) + ": wanted no calls at all, but observed "
                + recorded.get(0) + "."
                + Failures.listOf(recorded));
      }
    }
  }

  /** Fails when a matcher was registered but never consumed. */
  public static void validateUsage() {
    List<String> unfinished = new ArrayList<>();
    if (!MATCHERS.isEmpty()) {
      unfinished.add(MATCHERS.size() + " argument matcher(s) left over: " + MATCHERS);
    }
    if (verification != null) {
      unfinished.add("verify(mock) was not followed by a method call");
    }
    if (stubbing != null) {
      unfinished.add("when(mock) was not followed by a method call");
    }
    if (nextStubIsLenient) {
      unfinished.add("lenient() was not followed by when(mock.method())");
    }

    MATCHERS.clear();
    verification = null;
    stubbing = null;
    nextStubIsLenient = false;
    lastInvocation = null;

    if (!unfinished.isEmpty()) {
      throw new IllegalStateException(
          "validateUsage() found unfinished Mockatcha operations: " + unfinished
              + ". Complete each operation in the test where it begins.");
    }
  }

  /**
   * Fails when a stub was arranged on these mocks and no call ever matched it.
   *
   * <p>With no mocks, checks every mock created since the last such check.
   */
  public static void validateStubbing(Object... mocks) {
    List<Object> subjects;
    if (mocks.length == 0) {
      // Drained before the check, so that a failure here does not carry into the next test.
      subjects = new ArrayList<>(UNCHECKED_MOCKS);
      UNCHECKED_MOCKS.clear();
    } else {
      subjects = Arrays.asList(mocks);
    }

    StringBuilder report = new StringBuilder();
    for (Object mock : subjects) {
      MockState state = STATES.get(mock);
      if (state == null) {
        throw new IllegalArgumentException("Object is not a Mockatcha mock");
      }
      List<Stub> unusedStubs = state.unusedStubs();
      if (!unusedStubs.isEmpty()) {
        report.append("\n").append(state.description()).append(":");
      }
      for (Stub unused : unusedStubs) {
        report.append("\n  ").append(unused.pattern());
        List<Invocation> nearby = state.invocationsOf(unused.pattern().methodName());
        if (nearby.isEmpty()) {
          report.append("\n    that method was never called");
        } else {
          report.append("\n    that method was called with:");
          for (Invocation invocation : nearby) {
            report.append("\n      ").append(invocation);
          }
        }
      }
    }

    if (report.length() > 0) {
      throw new AssertionError(
          "These stubs were arranged but never used:" + report
              + "\nRemove them, correct their arguments, or arrange them with lenient().");
    }
  }

  /** Removes the most recently registered matcher, so a combining matcher can wrap it. */
  public static RegisteredMatcher takeLastMatcher() {
    if (MATCHERS.isEmpty()) {
      throw new IllegalStateException(
          "A combining matcher needs matchers to combine, as in and(gt(2), lt(9))");
    }
    return MATCHERS.remove(MATCHERS.size() - 1);
  }

  private static InvocationPattern consumePattern(String method, Object[] arguments) {
    List<RegisteredMatcher> argumentMatchers;
    if (MATCHERS.isEmpty()) {
      argumentMatchers = new ArrayList<>(arguments.length);
      for (Object argument : arguments) {
        argumentMatchers.add(
            new RegisteredMatcher(
                actual -> Objects.deepEquals(argument, actual), String.valueOf(argument)));
      }
    } else {
      if (MATCHERS.size() != arguments.length) {
        int matcherCount = MATCHERS.size();
        MATCHERS.clear();
        throw new IllegalStateException(
            "When matchers are used, every argument must use one: received "
                + matcherCount
                + " matcher(s) for "
                + arguments.length
                + " argument(s)");
      }
      argumentMatchers = new ArrayList<>(MATCHERS);
      MATCHERS.clear();
    }
    return InvocationPattern.of(method, argumentMatchers);
  }

  /**
   * Arranges answers for a method chosen by name rather than by evaluating a call.
   *
   * <p>Null arguments match every call of that name.
   */
  public static void arrangeByName(
      Object mock, String methodName, Object[] arguments, List<Answer<?>> answers) {
    MockState state = requireState(mock);
    Objects.requireNonNull(methodName, "methodName");
    Stub stub =
        newStub(
            state,
            arguments == null
                ? InvocationPattern.ofName(methodName)
                : InvocationPattern.ofName(methodName, exactMatchers(arguments)));
    for (Answer<?> answer : answers) {
      stub.add(answer);
    }
  }

  /** Removes every stub arranged for a method name, so the mock answers as if never stubbed. */
  public static void removeStubsByName(Object mock, String methodName) {
    requireState(mock).removeStubs(Objects.requireNonNull(methodName, "methodName"));
  }

  private static List<RegisteredMatcher> exactMatchers(Object[] arguments) {
    List<RegisteredMatcher> matchers = new ArrayList<>(arguments.length);
    for (Object argument : arguments) {
      matchers.add(
          new RegisteredMatcher(
              actual -> Objects.deepEquals(argument, actual), String.valueOf(argument)));
    }
    return matchers;
  }

  private static MockState requireState(Object mock) {
    MockState state = STATES.get(mock);
    if (state == null) {
      throw new IllegalArgumentException("Object is not a Mockatcha mock");
    }
    return state;
  }

  private static String stateDescription(Object mock) {
    return requireState(mock).description();
  }

  private static Throwable combine(Throwable primary, Throwable additional) {
    if (primary == null) {
      return additional;
    }
    primary.addSuppressed(additional);
    return primary;
  }

  private static final class SessionContext {
    private final Object token;
    private final boolean previousStrictStubs;
    private final List<Object> mocks = new ArrayList<>();

    private SessionContext(Object token, boolean previousStrictStubs) {
      this.token = token;
      this.previousStrictStubs = previousStrictStubs;
    }
  }

  private static final class LastInvocation {
    private final MockState state;
    private final Invocation invocation;
    private final InvocationPattern pattern;

    private LastInvocation(
        MockState state, Invocation invocation, InvocationPattern pattern) {
      this.state = state;
      this.invocation = invocation;
      this.pattern = pattern;
    }
  }

  private static final class VerificationRequest {
    private final MockState state;
    private final VerificationMode mode;
    private final InOrderImpl order;

    private VerificationRequest(MockState state, VerificationMode mode, InOrderImpl order) {
      this.state = state;
      this.mode = mode;
      this.order = order;
    }
  }

  private static final class StubbingRequest {
    private final MockState state;
    private final List<Answer<?>> answers;

    private StubbingRequest(MockState state, List<Answer<?>> answers) {
      this.state = state;
      this.answers = answers;
    }
  }
}

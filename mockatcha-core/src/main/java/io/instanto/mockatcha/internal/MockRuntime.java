package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.Answer;
import io.instanto.mockatcha.ArgumentMatcher;
import io.instanto.mockatcha.Invocation;
import io.instanto.mockatcha.OngoingStubbing;
import io.instanto.mockatcha.VerificationContext;
import io.instanto.mockatcha.VerificationMode;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Runtime called by TeaVM-generated mock implementations. */
public final class MockRuntime {

  /** Returned by dispatch when nothing is stubbed, which a stubbed null must not look like. */
  public static final Object REAL_CALL = new Object();

  private static final Map<Object, MockState> STATES = new IdentityHashMap<>();
  private static final List<RegisteredMatcher> MATCHERS = new ArrayList<>();
  private static LastInvocation lastInvocation;
  private static VerificationRequest verification;
  private static StubbingRequest stubbing;

  private MockRuntime() {}

  public static MockState createState(String description) {
    requireNoPendingMatchers("Creating a mock");
    return new MockState(description);
  }

  /** Answers {@code toString} on a generated mock or spy. */
  public static String describe(MockState state) {
    return state.description();
  }

  /** Reports whether a generated method should hand this call to the real object. */
  public static boolean callsReal(Object dispatchResult, Object delegate) {
    return dispatchResult == REAL_CALL && delegate != null;
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

  public static Object invokeObject(MockState state, String method, Object[] arguments) {
    Object result = dispatch(state, method, arguments);
    return result == REAL_CALL ? null : result;
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
    return new OngoingStubbingImpl<>(captured.state.addStub(captured.pattern));
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
    Stub stub = state.addStub(pattern);
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
    state.markVerified(matched);
    request.mode.verify(
        new VerificationContext(
            matched.size(), state.invocationCount(), pattern.toString(), state.invocations()));
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
            "Wanted no further calls, but observed " + remaining.get(0) + "."
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
            "Wanted no calls at all, but observed " + recorded.get(0) + "."
                + Failures.listOf(recorded));
      }
    }
  }

  /** Fails when a matcher was registered but never consumed. */
  public static void validateUsage() {
    requireNoPendingMatchers("validateUsage()");
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
        state.addStub(
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

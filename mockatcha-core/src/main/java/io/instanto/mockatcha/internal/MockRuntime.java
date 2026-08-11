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

  /**
   * Returned by {@link #dispatchSpy} when a spy call has no stub and the real object should answer
   * it. A dedicated instance keeps "no stub" distinct from a stub configured with a null result.
   */
  public static final Object REAL_CALL = new Object();

  private static final Map<Object, MockState> STATES = new IdentityHashMap<>();
  private static final List<RegisteredMatcher> MATCHERS = new ArrayList<>();
  private static LastInvocation lastInvocation;
  private static VerificationRequest verification;
  private static StubbingRequest stubbing;

  private MockRuntime() {}

  public static MockState createState(String description) {
    return new MockState(description);
  }

  /** Answers {@code toString} on a generated mock or spy without running the mocked class's own. */
  public static String describe(MockState state) {
    return state.description();
  }

  /**
   * Reports whether a generated method should hand this call to the real object.
   *
   * <p>A mock has no delegate, so it always answers from the runtime.
   */
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

  /** Rejects a spy created without a real object to delegate to. */
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

  /**
   * Dispatches a call on a generated spy.
   *
   * <p>Returns {@link #REAL_CALL} when the generated method should hand the call to the real
   * object, and the configured result otherwise. Verification and {@code doReturn}-style stubbing
   * never reach the real object.
   */
  public static Object dispatchSpy(MockState state, String method, Object[] arguments) {
    return dispatch(state, method, arguments);
  }

  /*
   * Dispatch returns a boxed result, so a generated primitive method converts it here rather than
   * repeating the boxing and default-value rules in generated code.
   */

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

  /** Answers {@code equals} on a generated mock or spy, which is never the object it stands in for. */
  public static boolean sameInstance(Object mock, Object other) {
    return mock == other;
  }

  /** Answers {@code hashCode} on a generated mock or spy without consulting the mocked class. */
  public static int identityHashCode(Object mock) {
    return System.identityHashCode(mock);
  }

  public static <T> OngoingStubbing<T> when() {
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
    verification = new VerificationRequest(state, Objects.requireNonNull(mode, "mode"));
    lastInvocation = null;
    MATCHERS.clear();
  }

  /**
   * Arranges answers for the call that follows, without letting that call run.
   *
   * <p>This is how {@code doReturn(...).when(spy).method()} avoids the real method that plain
   * {@code when(spy.method())} would evaluate during setup.
   */
  public static void beginStubbing(Object mock, List<Answer<?>> answers) {
    MockState state = requireState(mock);
    stubbing = new StubbingRequest(state, new ArrayList<>(answers));
    lastInvocation = null;
    MATCHERS.clear();
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
    request.mode.verify(
        new VerificationContext(
            state.count(pattern), state.invocationCount(), pattern.toString()));
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
   * <p>Passing null arguments matches every call of that name. This is the addressing scheme
   * Jasmine-shaped APIs use; the everyday Mockito-shaped path is {@link #beginStubbing}.
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

    private VerificationRequest(MockState state, VerificationMode mode) {
      this.state = state;
      this.mode = mode;
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

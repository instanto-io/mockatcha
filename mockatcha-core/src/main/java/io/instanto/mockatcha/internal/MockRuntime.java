package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.ArgumentMatcher;
import io.instanto.mockatcha.Invocation;
import io.instanto.mockatcha.OngoingStubbing;
import io.instanto.mockatcha.VerificationMode;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Runtime called by TeaVM-generated mock implementations. */
public final class MockRuntime {

  private static final Map<Object, MockState> STATES = new IdentityHashMap<>();
  private static final List<RegisteredMatcher> MATCHERS = new ArrayList<>();
  private static LastInvocation lastInvocation;
  private static VerificationRequest verification;

  private MockRuntime() {}

  public static MockState createState() {
    return new MockState();
  }

  public static void register(Object mock, MockState state) {
    Objects.requireNonNull(mock, "mock");
    Objects.requireNonNull(state, "state");
    state.attach(mock);
    STATES.put(mock, state);
  }

  @SuppressWarnings("unchecked")
  public static void registerMatcher(ArgumentMatcher<?> matcher, String description) {
    MATCHERS.add(
        new RegisteredMatcher((ArgumentMatcher<Object>) matcher, description));
  }

  public static Object invokeObject(MockState state, String method, Object[] arguments) {
    InvocationPattern pattern = consumePattern(method, arguments);
    if (verification != null) {
      verify(state, pattern);
      return null;
    }

    Invocation invocation = new Invocation(state.mock(), method, arguments);
    state.record(invocation);
    lastInvocation = new LastInvocation(state, invocation, pattern);

    Stub stub = state.findStub(method, arguments);
    return stub == null ? null : stub.answer(invocation);
  }

  public static void invokeVoid(MockState state, String method, Object[] arguments) {
    invokeObject(state, method, arguments);
  }

  public static boolean invokeBoolean(MockState state, String method, Object[] arguments) {
    Object result = invokeObject(state, method, arguments);
    return result != null && (Boolean) result;
  }

  public static byte invokeByte(MockState state, String method, Object[] arguments) {
    Object result = invokeObject(state, method, arguments);
    return result == null ? 0 : ((Number) result).byteValue();
  }

  public static short invokeShort(MockState state, String method, Object[] arguments) {
    Object result = invokeObject(state, method, arguments);
    return result == null ? 0 : ((Number) result).shortValue();
  }

  public static char invokeChar(MockState state, String method, Object[] arguments) {
    Object result = invokeObject(state, method, arguments);
    return result == null ? '\0' : (Character) result;
  }

  public static int invokeInt(MockState state, String method, Object[] arguments) {
    Object result = invokeObject(state, method, arguments);
    return result == null ? 0 : ((Number) result).intValue();
  }

  public static long invokeLong(MockState state, String method, Object[] arguments) {
    Object result = invokeObject(state, method, arguments);
    return result == null ? 0L : ((Number) result).longValue();
  }

  public static float invokeFloat(MockState state, String method, Object[] arguments) {
    Object result = invokeObject(state, method, arguments);
    return result == null ? 0F : ((Number) result).floatValue();
  }

  public static double invokeDouble(MockState state, String method, Object[] arguments) {
    Object result = invokeObject(state, method, arguments);
    return result == null ? 0D : ((Number) result).doubleValue();
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
    MATCHERS.clear();
  }

  @SuppressWarnings("unchecked")
  public static <T, E extends Throwable> T sneakyThrow(Throwable throwable) throws E {
    throw (E) throwable;
  }

  private static void verify(MockState state, InvocationPattern pattern) {
    VerificationRequest request = verification;
    verification = null;
    if (request.state != state) {
      throw new IllegalStateException("verify() must be followed by a call on the same mock");
    }
    request.mode.verify(state.count(pattern), pattern.toString());
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
    return new InvocationPattern(method, argumentMatchers);
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
}

package io.instanto.mockatcha;

import static org.teavm.metaprogramming.Metaprogramming.caller;
import static org.teavm.metaprogramming.Metaprogramming.currentLocation;
import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.environment;
import static org.teavm.metaprogramming.Metaprogramming.exit;
import static org.teavm.metaprogramming.Metaprogramming.handle;
import static org.teavm.metaprogramming.Metaprogramming.proxy;

import io.instanto.mockatcha.internal.CountingMode;
import io.instanto.mockatcha.internal.InOrderImpl;
import io.instanto.mockatcha.internal.LenientStubberImpl;
import io.instanto.mockatcha.internal.MockRuntime;
import io.instanto.mockatcha.internal.MockState;
import io.instanto.mockatcha.internal.StubberImpl;
import io.instanto.mockatcha.internal.compile.MethodIdentity;
import io.instanto.mockatcha.internal.compile.SubclassGenerator;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.metaprogramming.ClassHandle;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.MethodCaller;
import org.teavm.metaprogramming.Value;

/** Entry point for mocking in TeaVM tests, following Mockito's API. */
@CompileTime
public final class Mockatcha {

  private Mockatcha() {}

  /**
   * Generates a mock implementation of an interface or an ordinary class.
   *
   * <p>The class must be a compile-time constant. A mocked class must be non-final and have a
   * no-argument constructor, which is run. Static, private, and final methods keep their real
   * behaviour.
   */
  @Meta
  public static native <T> T mock(Class<T> type);

  /**
   * Generates a spy that records calls and hands unstubbed ones to {@code delegate}.
   *
   * <p>The spy is a separate object from {@code delegate}, so a real method calling another method
   * on itself reaches the delegate rather than the spy.
   */
  @Meta
  public static native <T> T spy(Class<T> type, T delegate);

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

  /** Expects these mocks to have been called at all. */
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
   * <p>Off by default. {@link StrictTest} turns it on for the tests that extend it.
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

  public static MockingDetails mockingDetails(Object mock) {
    return new MockingDetails(mock);
  }

  public static void clearInvocations(Object mock) {
    MockRuntime.clearInvocations(mock);
  }

  public static void reset(Object mock) {
    MockRuntime.reset(mock);
  }

  private static <T> void mock(IntrospectClass<T> type) {
    generate(type, null);
  }

  private static <T> void spy(IntrospectClass<T> type, Value<T> delegate) {
    emit(() -> MockRuntime.requireDelegate(delegate.get()));
    generate(type, delegate);
  }

  /** Emits the mock or spy for one call site. */
  private static <T> void generate(IntrospectClass<T> type, Value<T> delegate) {
    String description =
        (delegate == null ? "Mockatcha mock of " : "Mockatcha spy of ") + type.name();
    Value<MockState> state = emit(() -> MockRuntime.createState(description));

    Value<T> generated =
        type.isInterface()
            ? proxyInterface(type, state, delegate)
            : subclass(type, state, delegate);
    if (generated == null) {
      exit(() -> null);
      return;
    }

    emit(() -> MockRuntime.register(generated.get(), state.get()));
    exit(() -> generated.get());
  }

  private static <T> Value<T> proxyInterface(
      IntrospectClass<T> type, Value<MockState> state, Value<T> delegate) {
    return proxy(
        type,
        (instance, method, arguments) -> {
          Value<Object[]> recorded = collectArguments(arguments);
          Value<Object> result =
              delegate == null
                  ? emitMockDispatch(state, method, recorded)
                  : emitSpyDispatch(state, delegate, method, recorded);
          emitReturn(method, result);
        });
  }

  /** Instantiates the generated subclass, or reports why the class is unsupported and returns null. */
  private static <T> Value<T> subclass(
      IntrospectClass<T> type, Value<MockState> state, Value<T> delegate) {
    String rejection = SubclassGenerator.rejectionReason(type);
    if (rejection != null) {
      environment()
          .diagnostics()
          .error(currentLocation(), "Mockatcha cannot mock this type: " + rejection);
      return null;
    }

    MethodCaller constructor =
        caller(SubclassGenerator.subclassOf(environment(), type).constructor());
    ClassHandle<T> mockedType = handle(type);
    Value<Object> instance =
        delegate == null
            ? emit(() -> constructor.construct(state.get(), null))
            : emit(() -> constructor.construct(state.get(), delegate.get()));
    return emit(() -> mockedType.cast(instance.get()));
  }

  private static Value<Object[]> collectArguments(Value<Object>[] arguments) {
    int argumentCount = arguments.length;
    Value<Object[]> collected = emit(() -> new Object[argumentCount]);
    for (int index = 0; index < argumentCount; index++) {
      int argumentIndex = index;
      Value<Object> argument = arguments[index];
      emit(() -> collected.get()[argumentIndex] = argument.get());
    }
    return collected;
  }

  private static Value<Object> emitMockDispatch(
      Value<MockState> state, IntrospectMethod method, Value<Object[]> arguments) {
    String methodId = MethodIdentity.of(method);
    return emit(() -> MockRuntime.invokeObject(state.get(), methodId, arguments.get()));
  }

  // The real call is emitted inline rather than passed as a callback, because TeaVM resolves the
  // target method while it inlines this fragment.
  private static <T> Value<Object> emitSpyDispatch(
      Value<MockState> state,
      Value<T> delegate,
      IntrospectMethod method,
      Value<Object[]> arguments) {
    String methodId = MethodIdentity.of(method);
    MethodCaller realCall = caller(method);
    return emit(
        () -> {
          Object result = MockRuntime.dispatchSpy(state.get(), methodId, arguments.get());
          return result == MockRuntime.REAL_CALL
              ? realCall.call(delegate.get(), arguments.get())
              : result;
        });
  }

  /** Converts the boxed dispatch result to the declared return type and returns it. */
  private static void emitReturn(IntrospectMethod method, Value<Object> result) {
    switch (method.returnType().name()) {
      case "void":
        exit();
        break;
      case "boolean":
        exit(() -> MockRuntime.toBoolean(result.get()));
        break;
      case "byte":
        exit(() -> MockRuntime.toByte(result.get()));
        break;
      case "short":
        exit(() -> MockRuntime.toShort(result.get()));
        break;
      case "char":
        exit(() -> MockRuntime.toChar(result.get()));
        break;
      case "int":
        exit(() -> MockRuntime.toInt(result.get()));
        break;
      case "long":
        exit(() -> MockRuntime.toLong(result.get()));
        break;
      case "float":
        exit(() -> MockRuntime.toFloat(result.get()));
        break;
      case "double":
        exit(() -> MockRuntime.toDouble(result.get()));
        break;
      default:
        ClassHandle<?> resultType = handle(method.returnType());
        exit(() -> resultType.cast(result.get()));
        break;
    }
  }
}

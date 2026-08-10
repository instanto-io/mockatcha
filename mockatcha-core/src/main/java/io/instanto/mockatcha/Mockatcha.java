package io.instanto.mockatcha;

import static org.teavm.metaprogramming.Metaprogramming.caller;
import static org.teavm.metaprogramming.Metaprogramming.currentLocation;
import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.environment;
import static org.teavm.metaprogramming.Metaprogramming.exit;
import static org.teavm.metaprogramming.Metaprogramming.handle;
import static org.teavm.metaprogramming.Metaprogramming.proxy;

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

/** Entry point for TeaVM-native, Mockito-shaped mocking. */
@CompileTime
public final class Mockatcha {

  private Mockatcha() {}

  /**
   * Generates a mock implementation of an interface or an ordinary class.
   *
   * <p>The class argument must be visible as a constant to TeaVM at the call site.
   *
   * <p>A mocked class must be non-final and have a no-argument constructor, which Mockatcha runs.
   * Static, private, and final methods keep their real behaviour because they cannot be overridden.
   */
  @Meta
  public static native <T> T mock(Class<T> type);

  /**
   * Generates a spy that records calls and hands unstubbed ones to {@code delegate}.
   *
   * <p>The spy is a separate object from {@code delegate}, so {@code ==} distinguishes them, and a
   * real method calling another method on itself reaches the delegate rather than the spy.
   *
   * <p>The type is a separate argument because TeaVM must know the concrete class while it
   * compiles the call.
   */
  @Meta
  public static native <T> T spy(Class<T> type, T delegate);

  /** Begins configuration of the most recently evaluated mock invocation. */
  public static <T> OngoingStubbing<T> when(T ignored) {
    return MockRuntime.when();
  }

  /** Arranges a result without evaluating the call, which matters for spies. */
  public static Stubber doReturn(Object value) {
    return new StubberImpl().doReturn(value);
  }

  /** Arranges a failure without evaluating the call, which matters for spies. */
  public static Stubber doThrow(Throwable throwable) {
    return new StubberImpl().doThrow(throwable);
  }

  /** Arranges a calculated result without evaluating the call, which matters for spies. */
  public static Stubber doAnswer(Answer<?> answer) {
    return new StubberImpl().doAnswer(answer);
  }

  /** Arranges a call to do nothing, which suppresses a spy's real method. */
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

  public static VerificationMode times(int expectedCount) {
    if (expectedCount < 0) {
      throw new IllegalArgumentException("expectedCount must not be negative");
    }
    return (actualCount, invocation) -> {
      if (actualCount != expectedCount) {
        throw new AssertionError(
            "Wanted "
                + expectedCount
                + " invocation(s) of "
                + invocation
                + " but observed "
                + actualCount);
      }
    };
  }

  public static VerificationMode never() {
    return times(0);
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

  /**
   * Emits the mock or spy for one call site.
   *
   * <p>An interface goes to TeaVM's proxy generator, which already produces a class implementing
   * every abstract method. An ordinary class needs a generated subclass instead, because the proxy
   * generator supplies bodies only for methods it finds abstract.
   */
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

  /**
   * Instantiates the generated subclass for a mocked class, or reports why the class is out of
   * bounds and returns null.
   */
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

  /**
   * Emits the runtime decision and, when nothing is stubbed, the call to the real object.
   *
   * <p>The real call is emitted inline rather than passed as a callback because TeaVM resolves the
   * target method while it inlines this fragment. Verification and arranged answers return before
   * the branch, so neither reaches the real object.
   */
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

  /** Converts the boxed dispatch result to the method's declared return type and returns it. */
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

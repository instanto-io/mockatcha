package io.instanto.mockatcha;

import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.exit;
import static org.teavm.metaprogramming.Metaprogramming.proxy;
import static org.teavm.metaprogramming.Metaprogramming.unsupportedCase;

import io.instanto.mockatcha.internal.MockRuntime;
import io.instanto.mockatcha.internal.MockState;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.ReflectClass;
import org.teavm.metaprogramming.Value;

/** Entry point for TeaVM-native, Mockito-shaped interface mocking. */
@CompileTime
public final class Mockatcha {

  private Mockatcha() {}

  /**
   * Generates a mock implementation for an interface.
   *
   * <p>The class argument must be visible as a constant to TeaVM at the call site.
   */
  @Meta
  public static native <T> T mock(Class<T> type);

  /** Begins configuration of the most recently evaluated mock invocation. */
  public static <T> OngoingStubbing<T> when(T ignored) {
    return MockRuntime.when();
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

  @SuppressWarnings({"deprecation", "unchecked"})
  private static <T> void mock(ReflectClass<T> type) {
    if (!type.isInterface()) {
      unsupportedCase();
      return;
    }

    Value<MockState> state = emit(MockRuntime::createState);
    Value<T> generated =
        proxy(
            type,
            (instance, method, arguments) -> {
              String methodId = methodId(method.getName(), method.getParameterTypes());
              int argumentCount = arguments.length;
              Value<Object[]> invocationArguments =
                  emit(() -> new Object[argumentCount]);
              for (int index = 0; index < argumentCount; index++) {
                int argumentIndex = index;
                Value<Object> argument = arguments[index];
                emit(
                    () ->
                        invocationArguments.get()[argumentIndex] =
                            argument.get());
              }

              String returnType = method.getReturnType().getName();
              switch (returnType) {
                case "void":
                  emit(
                      () ->
                          MockRuntime.invokeVoid(
                              state.get(), methodId, invocationArguments.get()));
                  exit();
                  break;
                case "boolean":
                  exit(
                      () ->
                          MockRuntime.invokeBoolean(
                              state.get(), methodId, invocationArguments.get()));
                  break;
                case "byte":
                  exit(
                      () ->
                          MockRuntime.invokeByte(
                              state.get(), methodId, invocationArguments.get()));
                  break;
                case "short":
                  exit(
                      () ->
                          MockRuntime.invokeShort(
                              state.get(), methodId, invocationArguments.get()));
                  break;
                case "char":
                  exit(
                      () ->
                          MockRuntime.invokeChar(
                              state.get(), methodId, invocationArguments.get()));
                  break;
                case "int":
                  exit(
                      () ->
                          MockRuntime.invokeInt(
                              state.get(), methodId, invocationArguments.get()));
                  break;
                case "long":
                  exit(
                      () ->
                          MockRuntime.invokeLong(
                              state.get(), methodId, invocationArguments.get()));
                  break;
                case "float":
                  exit(
                      () ->
                          MockRuntime.invokeFloat(
                              state.get(), methodId, invocationArguments.get()));
                  break;
                case "double":
                  exit(
                      () ->
                          MockRuntime.invokeDouble(
                              state.get(), methodId, invocationArguments.get()));
                  break;
                default:
                  ReflectClass<?> resultType = method.getReturnType();
                  exit(
                      () ->
                          resultType.cast(
                              MockRuntime.invokeObject(
                                  state.get(), methodId, invocationArguments.get())));
                  break;
              }
            });

    emit(() -> MockRuntime.register(generated.get(), state.get()));
    exit(() -> generated.get());
  }

  @SuppressWarnings("deprecation")
  private static String methodId(String name, ReflectClass<?>[] parameterTypes) {
    StringBuilder identifier = new StringBuilder(name).append('(');
    for (int index = 0; index < parameterTypes.length; index++) {
      if (index > 0) {
        identifier.append(',');
      }
      identifier.append(parameterTypes[index].getName());
    }
    return identifier.append(')').toString();
  }
}

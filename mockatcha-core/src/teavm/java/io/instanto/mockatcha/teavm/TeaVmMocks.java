/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.teavm;

import static org.teavm.metaprogramming.Metaprogramming.caller;
import static org.teavm.metaprogramming.Metaprogramming.currentLocation;
import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.environment;
import static org.teavm.metaprogramming.Metaprogramming.exit;
import static org.teavm.metaprogramming.Metaprogramming.handle;
import static org.teavm.metaprogramming.Metaprogramming.proxy;

import io.instanto.mockatcha.internal.MockRuntime;
import io.instanto.mockatcha.internal.MockState;
import org.teavm.extension.introspect.IntrospectClass;
import org.teavm.extension.introspect.IntrospectMethod;
import org.teavm.metaprogramming.ClassHandle;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.MethodCaller;
import org.teavm.metaprogramming.Value;

/**
 * Generates mocks and spies while TeaVM compiles, one implementation per mocked type.
 *
 * <p>Reached only through {@link io.instanto.mockatcha.Mockatcha}, which routes here under TeaVM.
 * Each native method is replaced during compilation by the code its same-named companion emits.
 */
@CompileTime
public final class TeaVmMocks {

  private TeaVmMocks() {}

  @Meta
  public static native <T> T mock(Class<T> type);

  @Meta
  public static native <T> T mock(Class<T> type, String name);

  @Meta
  public static native <T> T spy(Class<T> type, T delegate);

  @Meta
  public static native <T> T spy(Class<T> type, T delegate, String name);

  private static <T> void mock(IntrospectClass<T> type) {
    generate(type, null, defaultDescription(type, false));
  }

  private static <T> void mock(IntrospectClass<T> type, Value<String> name) {
    generate(type, null, namedDescription(type, name, false));
  }

  private static <T> void spy(IntrospectClass<T> type, Value<T> delegate) {
    emit(() -> MockRuntime.requireDelegate(delegate.get()));
    generate(type, delegate, defaultDescription(type, true));
  }

  private static <T> void spy(
      IntrospectClass<T> type, Value<T> delegate, Value<String> name) {
    emit(() -> MockRuntime.requireDelegate(delegate.get()));
    generate(type, delegate, namedDescription(type, name, true));
  }

  /** Emits the mock or spy for one call site. */
  private static <T> void generate(
      IntrospectClass<T> type, Value<T> delegate, Value<String> description) {
    Value<MockState> state = emit(() -> MockRuntime.createState(description.get()));

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

  private static Value<String> defaultDescription(IntrospectClass<?> type, boolean spy) {
    String description = (spy ? "Mockatcha spy of " : "Mockatcha mock of ") + type.name();
    return emit(() -> description);
  }

  private static Value<String> namedDescription(
      IntrospectClass<?> type, Value<String> name, boolean spy) {
    String kind = (spy ? "Mockatcha spy of " : "Mockatcha mock of ") + type.name();
    return emit(() -> MockRuntime.requireMockName(name.get(), kind));
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

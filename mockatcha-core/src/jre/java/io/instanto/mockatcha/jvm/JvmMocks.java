/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.jvm;

import io.instanto.mockatcha.internal.MockRuntime;
import io.instanto.mockatcha.internal.MockState;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates mocks and spies on the JVM, where TeaVM's compile-time generation is not available.
 *
 * <p>An interface is mocked with a {@link Proxy}; a class with a subclass generated on the spot.
 * Either way the calls reach the same {@link MockRuntime} that a TeaVM-generated mock calls, so
 * stubbing, verification and diagnostics behave alike on both platforms.
 */
public final class JvmMocks {
  private JvmMocks() {}

  public static <T> T mock(Class<T> type) {
    return create(type, null, defaultDescription(type, false));
  }

  public static <T> T mock(Class<T> type, String name) {
    return create(type, null, namedDescription(type, name, false));
  }

  public static <T> T spy(Class<T> type, T delegate) {
    MockRuntime.requireDelegate(delegate);
    return create(type, delegate, defaultDescription(type, true));
  }

  public static <T> T spy(Class<T> type, T delegate, String name) {
    MockRuntime.requireDelegate(delegate);
    return create(type, delegate, namedDescription(type, name, true));
  }

  private static <T> T create(Class<T> type, T delegate, String description) {
    MockState state = MockRuntime.createState(description);
    T mock = type.isInterface() ? proxy(type, state, delegate) : subclass(type, state, delegate);
    MockRuntime.register(mock, state);
    return mock;
  }

  private static <T> T proxy(Class<T> type, MockState state, T delegate) {
    return type.cast(
        Proxy.newProxyInstance(loaderFor(type), new Class<?>[] {type}, new Handler(state, delegate)));
  }

  private static <T> T subclass(Class<T> type, MockState state, T delegate) {
    String rejection = JvmSubclasses.rejectionReason(type);
    if (rejection != null) {
      throw new UnsupportedOperationException("Mockatcha cannot mock this type: " + rejection);
    }
    return JvmSubclasses.instantiate(type, state, delegate);
  }

  private static String defaultDescription(Class<?> type, boolean spy) {
    return kind(Objects.requireNonNull(type, "type"), spy);
  }

  private static String namedDescription(Class<?> type, String name, boolean spy) {
    return MockRuntime.requireMockName(name, kind(Objects.requireNonNull(type, "type"), spy));
  }

  private static String kind(Class<?> type, boolean spy) {
    return (spy ? "Mockatcha spy of " : "Mockatcha mock of ") + type.getName();
  }

  // An interface loaded by the bootstrap loader is visible from any loader, including ours.
  private static ClassLoader loaderFor(Class<?> type) {
    ClassLoader loader = type.getClassLoader();
    return loader != null ? loader : JvmMocks.class.getClassLoader();
  }

  /** Names a method as the TeaVM generator does, such as {@code total(java.lang.String,int)}. */
  static String identity(Method method) {
    StringBuilder identifier = new StringBuilder(method.getName()).append('(');
    Class<?>[] parameters = method.getParameterTypes();
    for (int index = 0; index < parameters.length; index++) {
      if (index > 0) {
        identifier.append(',');
      }
      identifier.append(parameters[index].getName());
    }
    return identifier.append(')').toString();
  }

  private static final class Handler implements InvocationHandler {
    private static final Object[] NO_ARGUMENTS = new Object[0];
    private static final Map<Method, String> IDENTITIES = new ConcurrentHashMap<>();

    private final MockState state;
    private final Object delegate;

    Handler(MockState state, Object delegate) {
      this.state = state;
      this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // Proxy passes equals, hashCode and toString with Object as the declaring class, even when
      // the interface redeclares them. Answer them as a generated class mock does.
      if (method.getDeclaringClass() == Object.class) {
        switch (method.getName()) {
          case "equals":
            return MockRuntime.sameInstance(proxy, args[0]);
          case "hashCode":
            return MockRuntime.identityHashCode(proxy);
          default:
            return MockRuntime.describe(state);
        }
      }
      Object[] arguments = args == null ? NO_ARGUMENTS : args;
      String methodId = IDENTITIES.computeIfAbsent(method, JvmMocks::identity);
      Object result;
      if (delegate == null) {
        result = MockRuntime.invokeObject(state, methodId, arguments);
      } else {
        result = MockRuntime.dispatchSpy(state, methodId, arguments);
        if (result == MockRuntime.REAL_CALL) {
          result = callReal(method, arguments);
        }
      }
      return toReturnType(method.getReturnType(), result);
    }

    private Object callReal(Method method, Object[] arguments) throws Throwable {
      // A test's own interface is often not public; its methods then need opening to be called.
      if (!method.canAccess(delegate)) {
        method.trySetAccessible();
      }
      try {
        return method.invoke(delegate, arguments);
      } catch (InvocationTargetException failure) {
        throw failure.getCause();
      }
    }

    private static Object toReturnType(Class<?> returnType, Object result) {
      if (returnType == void.class) {
        return null;
      }
      if (!returnType.isPrimitive()) {
        return MockRuntime.toObject(result);
      }
      if (returnType == boolean.class) {
        return MockRuntime.toBoolean(result);
      }
      if (returnType == byte.class) {
        return MockRuntime.toByte(result);
      }
      if (returnType == short.class) {
        return MockRuntime.toShort(result);
      }
      if (returnType == char.class) {
        return MockRuntime.toChar(result);
      }
      if (returnType == int.class) {
        return MockRuntime.toInt(result);
      }
      if (returnType == long.class) {
        return MockRuntime.toLong(result);
      }
      if (returnType == float.class) {
        return MockRuntime.toFloat(result);
      }
      return MockRuntime.toDouble(result);
    }
  }
}

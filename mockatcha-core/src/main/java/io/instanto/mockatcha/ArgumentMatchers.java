package io.instanto.mockatcha;

import io.instanto.mockatcha.internal.MockRuntime;
import java.util.Objects;

/** Mockito-shaped argument matchers for stubbing and verification. */
public final class ArgumentMatchers {

  private ArgumentMatchers() {}

  public static <T> T any() {
    MockRuntime.registerMatcher(argument -> true, "any()");
    return null;
  }

  public static String anyString() {
    MockRuntime.registerMatcher(argument -> argument instanceof String, "anyString()");
    return null;
  }

  public static int anyInt() {
    MockRuntime.registerMatcher(argument -> argument instanceof Integer, "anyInt()");
    return 0;
  }

  public static <T> T eq(T expected) {
    MockRuntime.registerMatcher(argument -> Objects.equals(expected, argument), "eq(" + expected + ")");
    return expected;
  }
}

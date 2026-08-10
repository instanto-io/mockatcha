package io.instanto.mockatcha;

import io.instanto.mockatcha.internal.MockRuntime;
import java.util.Objects;

/**
 * Mockito-shaped argument matchers for stubbing and verification.
 *
 * <p>If one argument of a call uses a matcher, every argument of that call must use one. The rule
 * keeps the intended call unambiguous.
 */
public final class ArgumentMatchers {

  private ArgumentMatchers() {}

  public static <T> T any() {
    return argThat(argument -> true, "any()");
  }

  public static boolean anyBoolean() {
    argThat(argument -> argument instanceof Boolean, "anyBoolean()");
    return false;
  }

  public static byte anyByte() {
    argThat(argument -> argument instanceof Byte, "anyByte()");
    return 0;
  }

  public static short anyShort() {
    argThat(argument -> argument instanceof Short, "anyShort()");
    return 0;
  }

  public static char anyChar() {
    argThat(argument -> argument instanceof Character, "anyChar()");
    return '\0';
  }

  public static int anyInt() {
    argThat(argument -> argument instanceof Integer, "anyInt()");
    return 0;
  }

  public static long anyLong() {
    argThat(argument -> argument instanceof Long, "anyLong()");
    return 0L;
  }

  public static float anyFloat() {
    argThat(argument -> argument instanceof Float, "anyFloat()");
    return 0F;
  }

  public static double anyDouble() {
    argThat(argument -> argument instanceof Double, "anyDouble()");
    return 0D;
  }

  public static String anyString() {
    return argThat(argument -> argument instanceof String, "anyString()");
  }

  public static <T> T isNull() {
    return argThat(Objects::isNull, "isNull()");
  }

  public static <T> T isNotNull() {
    return argThat(Objects::nonNull, "isNotNull()");
  }

  public static <T> T eq(T expected) {
    argThat(argument -> Objects.equals(expected, argument), "eq(" + expected + ")");
    return expected;
  }

  /** Matches an argument with a condition of your own, which is reported as {@code argThat(...)}. */
  public static <T> T argThat(ArgumentMatcher<T> matcher) {
    return argThat(matcher, "argThat(...)");
  }

  /**
   * Registers a matcher with the description that failure messages should show.
   *
   * <p>Libraries layered on Mockatcha use this to add matchers of their own without reaching into
   * the runtime.
   */
  public static <T> T argThat(ArgumentMatcher<T> matcher, String description) {
    MockRuntime.registerMatcher(
        Objects.requireNonNull(matcher, "matcher"),
        Objects.requireNonNull(description, "description"));
    return null;
  }
}

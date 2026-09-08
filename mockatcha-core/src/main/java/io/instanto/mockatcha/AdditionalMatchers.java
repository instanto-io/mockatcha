/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import io.instanto.mockatcha.internal.MockRuntime;
import io.instanto.mockatcha.internal.RegisteredMatcher;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Matchers that compare and combine, alongside the ones in {@link ArgumentMatchers}.
 *
 * <pre>{@code
 * verify(store).save(and(gt(10), lt(100)));
 * verify(audit).log(not(eq("ignored")));
 * verify(reader).read(aryEq(new byte[] {1, 2, 3}));
 * }</pre>
 *
 * <p>Comparisons come in {@code int}, {@code long}, {@code double}, and {@link Comparable} forms.
 * A {@code float}, {@code short}, or {@code byte} argument is served by the {@code int} or
 * {@code double} form, which Java widens at the call site.
 */
public final class AdditionalMatchers {

  private AdditionalMatchers() {}

  /** Matches when both matchers match. */
  public static <T> T and(T first, T second) {
    return combine(true);
  }

  /** Matches when either matcher matches. */
  public static <T> T or(T first, T second) {
    return combine(false);
  }

  /** Matches when the matcher does not. */
  public static <T> T not(T matcher) {
    return invert();
  }

  /*
   * A combining matcher stands in for the argument it describes, so each primitive type needs
   * an overload that returns something the call site can unbox.
   */

  public static boolean and(boolean first, boolean second) {
    combine(true);
    return false;
  }

  public static boolean or(boolean first, boolean second) {
    combine(false);
    return false;
  }

  public static boolean not(boolean matcher) {
    invert();
    return false;
  }

  public static byte and(byte first, byte second) {
    combine(true);
    return (byte) 0;
  }

  public static byte or(byte first, byte second) {
    combine(false);
    return (byte) 0;
  }

  public static byte not(byte matcher) {
    invert();
    return (byte) 0;
  }

  public static char and(char first, char second) {
    combine(true);
    return '\0';
  }

  public static char or(char first, char second) {
    combine(false);
    return '\0';
  }

  public static char not(char matcher) {
    invert();
    return '\0';
  }

  public static short and(short first, short second) {
    combine(true);
    return (short) 0;
  }

  public static short or(short first, short second) {
    combine(false);
    return (short) 0;
  }

  public static short not(short matcher) {
    invert();
    return (short) 0;
  }

  public static int and(int first, int second) {
    combine(true);
    return 0;
  }

  public static int or(int first, int second) {
    combine(false);
    return 0;
  }

  public static int not(int matcher) {
    invert();
    return 0;
  }

  public static long and(long first, long second) {
    combine(true);
    return 0L;
  }

  public static long or(long first, long second) {
    combine(false);
    return 0L;
  }

  public static long not(long matcher) {
    invert();
    return 0L;
  }

  public static float and(float first, float second) {
    combine(true);
    return 0F;
  }

  public static float or(float first, float second) {
    combine(false);
    return 0F;
  }

  public static float not(float matcher) {
    invert();
    return 0F;
  }

  public static double and(double first, double second) {
    combine(true);
    return 0D;
  }

  public static double or(double first, double second) {
    combine(false);
    return 0D;
  }

  public static double not(double matcher) {
    invert();
    return 0D;
  }

  public static int gt(int value) {
    compareIntegral(value, ">");
    return 0;
  }

  public static long gt(long value) {
    compareIntegral(value, ">");
    return 0L;
  }

  public static double gt(double value) {
    compare(value, ">");
    return 0D;
  }

  public static int geq(int value) {
    compareIntegral(value, ">=");
    return 0;
  }

  public static long geq(long value) {
    compareIntegral(value, ">=");
    return 0L;
  }

  public static double geq(double value) {
    compare(value, ">=");
    return 0D;
  }

  public static int lt(int value) {
    compareIntegral(value, "<");
    return 0;
  }

  public static long lt(long value) {
    compareIntegral(value, "<");
    return 0L;
  }

  public static double lt(double value) {
    compare(value, "<");
    return 0D;
  }

  public static int leq(int value) {
    compareIntegral(value, "<=");
    return 0;
  }

  public static long leq(long value) {
    compareIntegral(value, "<=");
    return 0L;
  }

  public static double leq(double value) {
    compare(value, "<=");
    return 0D;
  }

  /** Matches a value greater than this one, by its own ordering. */
  public static <T extends Comparable<T>> T gt(T value) {
    return compareTo(value, ">");
  }

  /** Matches a value greater than or equal to this one, by its own ordering. */
  public static <T extends Comparable<T>> T geq(T value) {
    return compareTo(value, ">=");
  }

  /** Matches a value less than this one, by its own ordering. */
  public static <T extends Comparable<T>> T lt(T value) {
    return compareTo(value, "<");
  }

  /** Matches a value less than or equal to this one, by its own ordering. */
  public static <T extends Comparable<T>> T leq(T value) {
    return compareTo(value, "<=");
  }

  /** Matches a value that compares equal to this one, which {@code equals} may not. */
  public static <T extends Comparable<T>> T cmpEq(T value) {
    return compareTo(value, "==");
  }

  /** Matches a string containing this regular expression. */
  public static String find(String regularExpression) {
    Objects.requireNonNull(regularExpression, "regularExpression");
    Pattern pattern = Pattern.compile(regularExpression);
    return ArgumentMatchers.argThat(
        argument ->
            argument instanceof String
                && pattern.matcher((String) argument).find(),
        "find(\"" + regularExpression + "\")");
  }

  /** Matches an array with these contents, in this order. */
  public static <T> T[] aryEq(T[] value) {
    return arrayEqual(value);
  }

  public static boolean[] aryEq(boolean[] value) {
    return arrayEqual(value);
  }

  public static byte[] aryEq(byte[] value) {
    return arrayEqual(value);
  }

  public static char[] aryEq(char[] value) {
    return arrayEqual(value);
  }

  public static short[] aryEq(short[] value) {
    return arrayEqual(value);
  }

  public static int[] aryEq(int[] value) {
    return arrayEqual(value);
  }

  public static long[] aryEq(long[] value) {
    return arrayEqual(value);
  }

  public static float[] aryEq(float[] value) {
    return arrayEqual(value);
  }

  public static double[] aryEq(double[] value) {
    return arrayEqual(value);
  }

  private static <T> T combine(boolean requireBoth) {
    RegisteredMatcher right = MockRuntime.takeLastMatcher();
    RegisteredMatcher left = MockRuntime.takeLastMatcher();
    String name = requireBoth ? "and" : "or";
    ArgumentMatcher<Object> combined =
        argument ->
            requireBoth
                ? left.matches(argument) && right.matches(argument)
                : left.matches(argument) || right.matches(argument);
    MockRuntime.registerMatcher(
        combined,
        name + "(" + left + ", " + right + ")",
        argument -> {
          if (left.matches(argument)) {
            left.capture(argument);
          }
          if (right.matches(argument)) {
            right.capture(argument);
          }
        });
    return null;
  }

  private static <T> T invert() {
    RegisteredMatcher inner = MockRuntime.takeLastMatcher();
    return ArgumentMatchers.argThat(argument -> !inner.matches(argument), "not(" + inner + ")");
  }

  private static <A> A arrayEqual(A value) {
    ArgumentMatchers.argThat(
        argument -> Objects.deepEquals(value, argument), "aryEq(" + describe(value) + ")");
    return null;
  }

  private static void compare(double bound, String operator) {
    ArgumentMatchers.argThat(
        argument -> argument instanceof Number && holds(operator, compareNumbers(argument, bound)),
        operator + " " + bound);
  }

  private static void compareIntegral(long bound, String operator) {
    ArgumentMatchers.argThat(
        argument -> argument instanceof Number && holds(operator, compareNumber(argument, bound)),
        operator + " " + bound);
  }

  private static int compareNumber(Object argument, long bound) {
    if (argument instanceof Byte
        || argument instanceof Short
        || argument instanceof Integer
        || argument instanceof Long) {
      return Long.compare(((Number) argument).longValue(), bound);
    }
    return Double.compare(((Number) argument).doubleValue(), (double) bound);
  }

  private static <T extends Comparable<T>> T compareTo(T bound, String operator) {
    Objects.requireNonNull(bound, "value");
    ArgumentMatchers.argThat(
        argument -> {
          if (!bound.getClass().isInstance(argument)) {
            return false;
          }
          @SuppressWarnings("unchecked")
          T other = (T) argument;
          return holds(operator, other.compareTo(bound));
        },
        operator + " " + bound);
    return null;
  }

  private static int compareNumbers(Object argument, double bound) {
    return Double.compare(((Number) argument).doubleValue(), bound);
  }

  private static boolean holds(String operator, int comparison) {
    switch (operator) {
      case ">":
        return comparison > 0;
      case ">=":
        return comparison >= 0;
      case "<":
        return comparison < 0;
      case "<=":
        return comparison <= 0;
      default:
        return comparison == 0;
    }
  }

  private static String describe(Object array) {
    if (array instanceof Object[]) {
      return java.util.Arrays.deepToString((Object[]) array);
    }
    return String.valueOf(array);
  }
}

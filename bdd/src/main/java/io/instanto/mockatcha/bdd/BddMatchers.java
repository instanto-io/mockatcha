package io.instanto.mockatcha.bdd;

import static io.instanto.mockatcha.ArgumentMatchers.argThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Argument matchers that describe the shape of a value rather than its exact identity.
 *
 * <p>These follow the same rule as {@code ArgumentMatchers}: if one argument of a call uses a
 * matcher, every argument must use one. To match an object by its properties, see
 * {@link ObjectMatchers}.
 */
public final class BddMatchers {

  private BddMatchers() {}

  /** Matches a string that contains a substring. */
  public static String stringContaining(String substring) {
    Objects.requireNonNull(substring, "substring");
    return argThat(
        argument -> argument instanceof String && ((String) argument).contains(substring),
        "stringContaining(\"" + substring + "\")");
  }

  /** Matches a string with a regular expression, anywhere within it. */
  public static String stringMatching(String regularExpression) {
    Pattern pattern = Pattern.compile(Objects.requireNonNull(regularExpression, "regex"));
    return argThat(
        argument -> argument instanceof String && pattern.matcher((String) argument).find(),
        "stringMatching(\"" + regularExpression + "\")");
  }

  /** Matches an array or collection containing all of these items, in any order. Extra items are allowed. */
  public static <T> T containing(Object... items) {
    Object[] wanted = items.clone();
    return argThat(
        argument -> containsAll(argument, wanted),
        "containing(" + Arrays.toString(wanted) + ")");
  }

  /** Matches an array or collection holding exactly these items, in any order. */
  public static <T> T containingExactly(Object... items) {
    Object[] wanted = items.clone();
    return argThat(
        argument -> sizeOf(argument) == wanted.length && containsAll(argument, wanted),
        "containingExactly(" + Arrays.toString(wanted) + ")");
  }

  /** Matches a value of this type. */
  public static <T> T instanceOf(Class<?> type) {
    Objects.requireNonNull(type, "type");
    return argThat(type::isInstance, "instanceOf(" + type.getName() + ")");
  }

  /** Matches a map that contains this entry, whatever else it holds. */
  public static <T> T mapContaining(Object key, Object value) {
    return argThat(
        argument ->
            argument instanceof Map
                && ((Map<?, ?>) argument).containsKey(key)
                && Objects.deepEquals(((Map<?, ?>) argument).get(key), value),
        "mapContaining(" + key + ", " + value + ")");
  }

  /** Matches an array or collection of exactly this size. */
  public static <T> T ofSize(int size) {
    return argThat(argument -> sizeOf(argument) == size, "ofSize(" + size + ")");
  }

  private static boolean containsAll(Object argument, Object[] wanted) {
    Collection<?> actual = asCollection(argument);
    if (actual == null) {
      return false;
    }
    for (Object item : wanted) {
      if (!containsItem(actual, item)) {
        return false;
      }
    }
    return true;
  }

  private static boolean containsItem(Collection<?> actual, Object item) {
    for (Object candidate : actual) {
      if (Objects.deepEquals(candidate, item)) {
        return true;
      }
    }
    return false;
  }

  private static int sizeOf(Object argument) {
    Collection<?> actual = asCollection(argument);
    return actual == null ? -1 : actual.size();
  }

  private static Collection<?> asCollection(Object argument) {
    if (argument instanceof Collection) {
      return (Collection<?>) argument;
    }
    if (argument instanceof Object[]) {
      return Arrays.asList((Object[]) argument);
    }
    return null;
  }
}

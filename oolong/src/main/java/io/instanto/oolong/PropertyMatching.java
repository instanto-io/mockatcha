package io.instanto.oolong;

import io.instanto.mockatcha.ArgumentMatchers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The runtime half of {@link ObjectMatchers}. */
public final class PropertyMatching {

  /** Returned by a generated reader for a property it does not know. */
  public static final Object UNKNOWN = new Object();

  private PropertyMatching() {}

  /** Registers a matcher for one property. Called from generated code. */
  public static <T> Object matchOne(PropertyReader<T> reader, String property, Object expected) {
    Map<String, Object> single = new LinkedHashMap<>();
    single.put(Objects.requireNonNull(property, "property"), expected);
    return matchAll(reader, single);
  }

  /** Registers a matcher for several properties at once. Called from generated code. */
  public static <T> Object matchAll(PropertyReader<T> reader, Map<String, Object> properties) {
    Objects.requireNonNull(properties, "properties");
    if (properties.isEmpty()) {
      throw new IllegalArgumentException("objectContaining needs at least one property");
    }
    Map<String, Object> wanted = new LinkedHashMap<>(properties);
    for (String property : wanted.keySet()) {
      requireKnown(reader, property);
    }
    return ArgumentMatchers.argThat(
        argument -> matches(reader, wanted, argument), describe(wanted));
  }

  private static <T> boolean matches(
      PropertyReader<T> reader, Map<String, Object> wanted, Object argument) {
    if (argument == null || !reader.canRead(argument)) {
      return false;
    }
    @SuppressWarnings("unchecked")
    T target = (T) argument;
    for (Map.Entry<String, Object> property : wanted.entrySet()) {
      if (!Objects.deepEquals(property.getValue(), reader.read(target, property.getKey()))) {
        return false;
      }
    }
    return true;
  }

  /** Fails when the matcher is built, so a misspelled property does not silently match nothing. */
  private static void requireKnown(PropertyReader<?> reader, String property) {
    List<String> names = Arrays.asList(reader.names());
    if (!names.contains(property)) {
      List<String> sorted = new ArrayList<>(names);
      sorted.sort(String::compareTo);
      throw new IllegalArgumentException(
          "No property named \"" + property + "\". Readable properties are " + sorted + ".");
    }
  }

  private static String describe(Map<String, Object> wanted) {
    StringBuilder description = new StringBuilder("objectContaining(");
    boolean first = true;
    for (Map.Entry<String, Object> property : wanted.entrySet()) {
      if (!first) {
        description.append(", ");
      }
      first = false;
      description.append(property.getKey()).append('=').append(property.getValue());
    }
    return description.append(')').toString();
  }
}

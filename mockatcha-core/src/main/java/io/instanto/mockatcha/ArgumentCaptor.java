package io.instanto.mockatcha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps the arguments a call was made with, so a test can assert on them afterwards.
 *
 * <pre>{@code
 * ArgumentCaptor<Timesheet> saved = ArgumentCaptor.forClass(Timesheet.class);
 *
 * verify(repository).save(saved.capture());
 *
 * assertEquals("A-17", saved.getValue().employeeId());
 * }</pre>
 *
 * <p>{@link #capture()} is a matcher, so the usual rule applies: if one argument of a call uses a
 * matcher, every argument must use one.
 */
public final class ArgumentCaptor<T> {

  private final List<T> captured = new ArrayList<>();
  private final Class<?> type;

  private ArgumentCaptor(Class<?> type) {
    this.type = type;
  }

  /**
   * Creates a captor for a type.
   *
   * <p>Required when capturing an argument of a primitive parameter.
   */
  public static <T> ArgumentCaptor<T> forClass(Class<T> type) {
    return new ArgumentCaptor<>(type);
  }

  /** Creates a captor whose type comes from the variable it is assigned to. */
  public static <T> ArgumentCaptor<T> captor() {
    return new ArgumentCaptor<>(null);
  }

  /**
   * Matches any argument and keeps it. Write this where the argument would go.
   *
   * <p>Values accumulate across verifications.
   */
  @SuppressWarnings("unchecked")
  public T capture() {
    ArgumentMatchers.argThat(
        argument -> {
          captured.add((T) argument);
          return true;
        },
        "capture()");
    return (T) emptyValueFor(type);
  }

  /** The most recently captured argument. */
  public T getValue() {
    if (captured.isEmpty()) {
      throw new IllegalStateException(
          "No argument was captured. Check that capture() was passed to a verified or stubbed"
              + " call, and that the call actually happened.");
    }
    return captured.get(captured.size() - 1);
  }

  /** Every captured argument, oldest first. */
  public List<T> getAllValues() {
    return Collections.unmodifiableList(new ArrayList<>(captured));
  }

  /** Forgets everything captured so far, so one captor can serve two verifications. */
  public void clear() {
    captured.clear();
  }

  // capture() stands in for an argument, so a primitive parameter needs something that unboxes.
  // The value is never used; the matcher accepts whatever the call really passed.
  private static Object emptyValueFor(Class<?> type) {
    if (type == null) {
      return null;
    }
    if (type == Boolean.class || type == boolean.class) {
      return Boolean.FALSE;
    }
    if (type == Byte.class || type == byte.class) {
      return (byte) 0;
    }
    if (type == Short.class || type == short.class) {
      return (short) 0;
    }
    if (type == Character.class || type == char.class) {
      return '\0';
    }
    if (type == Integer.class || type == int.class) {
      return 0;
    }
    if (type == Long.class || type == long.class) {
      return 0L;
    }
    if (type == Float.class || type == float.class) {
      return 0F;
    }
    if (type == Double.class || type == double.class) {
      return 0D;
    }
    return null;
  }

  @Override
  public String toString() {
    return "ArgumentCaptor" + captured;
  }
}

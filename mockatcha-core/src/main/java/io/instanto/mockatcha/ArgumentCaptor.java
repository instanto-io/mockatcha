package io.instanto.mockatcha;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps the arguments a call was actually made with, so a test can assert on them afterwards.
 *
 * <p>Matchers answer "was it called with something like this". A captor answers "what was it called
 * with", which is the question to ask when the argument is built inside the code under test and the
 * test could not have predicted it.
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
 * matcher, every argument must use one. Capturing during verification is the intended use;
 * capturing while stubbing works but describes the call less clearly than {@code eq} or {@code any}.
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
   * <p>The class is what makes the captured values typed, and it also lets {@link #capture()}
   * return a usable value for a primitive parameter rather than a null that would fail to unbox.
   */
  public static <T> ArgumentCaptor<T> forClass(Class<T> type) {
    return new ArgumentCaptor<>(type);
  }

  /**
   * Creates a captor whose type comes from the variable it is assigned to.
   *
   * <p>Convenient for reference types. Use {@link #forClass} when capturing an argument of a
   * primitive parameter.
   */
  public static <T> ArgumentCaptor<T> captor() {
    return new ArgumentCaptor<>(null);
  }

  /**
   * Matches any argument and keeps it. Write this where the argument would go.
   *
   * <p>Values accumulate, so verifying twice with one captor keeps both sets of arguments.
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

  /**
   * The last captured argument.
   *
   * <p>When a call was verified more than once, this is the most recent one, which matches
   * Mockito's behaviour.
   */
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

  /*
   * capture() stands in for an argument, so for a primitive parameter it must return something
   * that unboxes. The value is never used: the matcher accepts whatever the call really passed.
   */
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

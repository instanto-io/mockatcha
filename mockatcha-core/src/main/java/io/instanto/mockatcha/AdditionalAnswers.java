package io.instanto.mockatcha;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Answers built from the call being answered, for use with {@code thenAnswer} and
 * {@code doAnswer}.
 *
 * <pre>{@code
 * when(store.save(any())).thenAnswer(returnsFirstArg());
 * when(clock.next()).thenAnswer(returnsElementsOf(List.of(1, 2, 3)));
 * when(names.of(anyString())).thenAnswer(answer((String id) -> id.toUpperCase()));
 * }</pre>
 *
 * <p>A spy created with {@code spy(Class, T)} covers what Mockito's {@code delegatesTo} does.
 */
public final class AdditionalAnswers {

  private AdditionalAnswers() {}

  /** Returns the call's first argument. */
  public static <T> Answer<T> returnsFirstArg() {
    return returnsArgAt(0);
  }

  /** Returns the call's second argument. */
  public static <T> Answer<T> returnsSecondArg() {
    return returnsArgAt(1);
  }

  /** Returns the call's last argument. */
  public static <T> Answer<T> returnsLastArg() {
    return invocation -> argumentAt(invocation, invocation.argumentCount() - 1);
  }

  /** Returns the argument at this position, counting from zero. */
  public static <T> Answer<T> returnsArgAt(int index) {
    if (index < 0) {
      throw new IllegalArgumentException("An argument index must not be negative: " + index);
    }
    return invocation -> argumentAt(invocation, index);
  }

  /**
   * Returns each element in turn, repeating the last once they run out.
   *
   * <p>The same progression as chaining {@code thenReturn}, for a sequence a test already has.
   */
  public static <T> Answer<T> returnsElementsOf(Collection<?> elements) {
    Objects.requireNonNull(elements, "elements");
    if (elements.isEmpty()) {
      throw new IllegalArgumentException("returnsElementsOf needs at least one element");
    }
    List<?> remaining = new ArrayList<>(elements);
    int[] position = {0};
    return invocation -> {
      int index = Math.min(position[0], remaining.size() - 1);
      position[0] = index + 1;
      @SuppressWarnings("unchecked")
      T value = (T) remaining.get(index);
      return value;
    };
  }

  /** Computes a result from the call's single argument, without casting it in the test. */
  public static <T, A> Answer<T> answer(Answer1<T, A> answer) {
    Objects.requireNonNull(answer, "answer");
    return invocation -> {
      @SuppressWarnings("unchecked")
      A argument = (A) invocation.argument(0);
      return answer.answer(argument);
    };
  }

  /** Computes a result from the call's first two arguments, without casting them in the test. */
  public static <T, A, B> Answer<T> answer(Answer2<T, A, B> answer) {
    Objects.requireNonNull(answer, "answer");
    return invocation -> {
      @SuppressWarnings("unchecked")
      A first = (A) invocation.argument(0);
      @SuppressWarnings("unchecked")
      B second = (B) invocation.argument(1);
      return answer.answer(first, second);
    };
  }

  private static <T> T argumentAt(Invocation invocation, int index) {
    if (index < 0 || index >= invocation.argumentCount()) {
      throw new IllegalArgumentException(
          "Wanted argument " + index + " of " + invocation + " but it has "
              + invocation.argumentCount());
    }
    @SuppressWarnings("unchecked")
    T argument = (T) invocation.argument(index);
    return argument;
  }
}

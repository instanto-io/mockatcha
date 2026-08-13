package io.instanto.mockatcha.bdd;

import io.instanto.mockatcha.Answer;
import io.instanto.mockatcha.Invocation;
import io.instanto.mockatcha.spi.MockAccess;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A handle on one method of a mock or spy, addressed by name.
 *
 * <p>Obtained from {@link Bdd#spyOn(Object, String)}.
 */
public final class Spy {

  private final Object mock;
  private final String methodName;
  private final Object[] arguments;
  private final String description;

  Spy(Object mock, String methodName, Object[] arguments) {
    this.mock = mock;
    this.methodName = methodName;
    this.arguments = arguments;
    this.description =
        arguments == null
            ? methodName + "(any arguments)"
            : methodName + Arrays.toString(arguments);
  }

  /** Narrows this handle to calls made with these exact arguments. */
  public Spy withArgs(Object... arguments) {
    return new Spy(mock, methodName, Objects.requireNonNull(arguments, "arguments"));
  }

  /** Answers every matching call with one value. */
  public Spy andReturn(Object value) {
    return arrange(List.of(invocation -> value));
  }

  /** Answers matching calls with each value in turn, repeating the last one. */
  public Spy andReturnValues(Object... values) {
    List<Answer<?>> answers = new ArrayList<>(values.length);
    for (Object value : values) {
      answers.add(invocation -> value);
    }
    return arrange(answers);
  }

  /** Fails every matching call. */
  public Spy andThrow(Throwable throwable) {
    Objects.requireNonNull(throwable, "throwable");
    return arrange(
        List.of(
            invocation -> {
              throw throwable;
            }));
  }

  /** Calculates a result for each matching call from its arguments. */
  public Spy andCallFake(Answer<?> answer) {
    return arrange(List.of(Objects.requireNonNull(answer, "answer")));
  }

  /** Answers matching calls with Java's empty value. */
  public Spy andStub() {
    return arrange(List.of(invocation -> null));
  }

  /**
   * Removes any arranged behaviour for this method name.
   *
   * <p>On a spy the real object answers again; on a mock the call returns Java's empty value.
   */
  public Spy andCallThrough() {
    MockAccess.clearStubs(mock, methodName);
    return this;
  }

  /** The calls recorded for this method, narrowed by {@link #withArgs} when it was used. */
  public CallLog calls() {
    List<Invocation> matching = new ArrayList<>();
    for (Invocation invocation : MockAccess.invocations(mock, methodName)) {
      if (arguments == null || matchesArguments(invocation)) {
        matching.add(invocation);
      }
    }
    return new CallLog(mock, methodName, matching, description);
  }

  private boolean matchesArguments(Invocation invocation) {
    if (invocation.argumentCount() != arguments.length) {
      return false;
    }
    for (int index = 0; index < arguments.length; index++) {
      if (!Objects.deepEquals(arguments[index], invocation.argument(index))) {
        return false;
      }
    }
    return true;
  }

  private Spy arrange(List<Answer<?>> answers) {
    MockAccess.arrange(mock, methodName, arguments, answers);
    return this;
  }

  @Override
  public String toString() {
    return "Spy on " + description;
  }
}

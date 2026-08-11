package io.instanto.mockatcha;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable record of one method call made on a mock. */
public final class Invocation {

  private static int sequenceGenerator;

  private final Object mock;
  private final String method;
  private final Object[] arguments;
  private final int sequence = sequenceGenerator++;

  public Invocation(Object mock, String method, Object[] arguments) {
    this.mock = Objects.requireNonNull(mock, "mock");
    this.method = Objects.requireNonNull(method, "method");
    this.arguments = Objects.requireNonNull(arguments, "arguments").clone();
  }

  public Object mock() {
    return mock;
  }

  /** Where this call falls in the order shared by every mock. */
  public int sequence() {
    return sequence;
  }

  /** Returns a stable method identifier including parameter types. */
  public String method() {
    return method;
  }

  public String methodName() {
    int parameters = method.indexOf('(');
    return parameters < 0 ? method : method.substring(0, parameters);
  }

  public List<Object> arguments() {
    return Collections.unmodifiableList(Arrays.asList(arguments.clone()));
  }

  public Object argument(int index) {
    return arguments[index];
  }

  public int argumentCount() {
    return arguments.length;
  }

  @Override
  public String toString() {
    return method + arguments();
  }
}

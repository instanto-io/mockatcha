package io.instanto.mockatcha.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class InvocationPattern {

  private final String method;
  private final List<RegisteredMatcher> arguments;

  InvocationPattern(String method, List<RegisteredMatcher> arguments) {
    this.method = Objects.requireNonNull(method, "method");
    this.arguments = Collections.unmodifiableList(new ArrayList<>(arguments));
  }

  boolean matches(String method, Object[] arguments) {
    if (!this.method.equals(method) || this.arguments.size() != arguments.length) {
      return false;
    }
    for (int index = 0; index < arguments.length; index++) {
      if (!this.arguments.get(index).matches(arguments[index])) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return method + arguments;
  }
}

/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** The calls a stub answers, or the calls a verification counts. */
final class InvocationPattern {

  private final String method;
  private final boolean byName;
  private final List<RegisteredMatcher> arguments;

  private InvocationPattern(String method, boolean byName, List<RegisteredMatcher> arguments) {
    this.method = Objects.requireNonNull(method, "method");
    this.byName = byName;
    this.arguments =
        arguments == null ? null : Collections.unmodifiableList(new ArrayList<>(arguments));
  }

  /** Matches one method identifier, with a matcher for each argument. */
  static InvocationPattern of(String method, List<RegisteredMatcher> arguments) {
    return new InvocationPattern(method, false, Objects.requireNonNull(arguments, "arguments"));
  }

  /** Matches every overload of a method name, whatever arguments it is called with. */
  static InvocationPattern ofName(String methodName) {
    return new InvocationPattern(methodName, true, null);
  }

  /** Matches every overload of a method name that is called with these exact arguments. */
  static InvocationPattern ofName(String methodName, List<RegisteredMatcher> arguments) {
    return new InvocationPattern(methodName, true, Objects.requireNonNull(arguments, "arguments"));
  }

  /** The method name this pattern addresses. */
  String methodName() {
    int parenthesis = method.indexOf('(');
    return parenthesis < 0 ? method : method.substring(0, parenthesis);
  }

  boolean matches(String method, Object[] arguments) {
    if (!matchesMethod(method)) {
      return false;
    }
    if (this.arguments == null) {
      return true;
    }
    if (this.arguments.size() != arguments.length) {
      return false;
    }
    for (int index = 0; index < arguments.length; index++) {
      if (!this.arguments.get(index).matches(arguments[index])) {
        return false;
      }
    }
    return true;
  }

  /** Commits side effects such as argument capture after the whole invocation has matched. */
  void capture(Object[] arguments) {
    if (this.arguments == null) {
      return;
    }
    for (int index = 0; index < arguments.length; index++) {
      this.arguments.get(index).capture(arguments[index]);
    }
  }

  private boolean matchesMethod(String method) {
    if (!byName) {
      return this.method.equals(method);
    }
    // An identifier is name(parameterTypes); a Java method name cannot contain '('.
    return method.length() > this.method.length()
        && method.startsWith(this.method)
        && method.charAt(this.method.length()) == '(';
  }

  @Override
  public String toString() {
    if (arguments == null) {
      return method + "(any arguments)";
    }
    return method + arguments;
  }
}

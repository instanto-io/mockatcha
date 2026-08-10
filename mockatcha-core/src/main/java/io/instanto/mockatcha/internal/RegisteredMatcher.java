package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.ArgumentMatcher;

final class RegisteredMatcher {

  private final ArgumentMatcher<Object> matcher;
  private final String description;

  RegisteredMatcher(ArgumentMatcher<Object> matcher, String description) {
    this.matcher = matcher;
    this.description = description;
  }

  boolean matches(Object argument) {
    return matcher.matches(argument);
  }

  @Override
  public String toString() {
    return description;
  }
}

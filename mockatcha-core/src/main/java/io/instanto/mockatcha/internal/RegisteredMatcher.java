package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.ArgumentMatcher;

/** An argument matcher together with the text a failure message should show for it. */
public final class RegisteredMatcher {

  private final ArgumentMatcher<Object> matcher;
  private final String description;

  RegisteredMatcher(ArgumentMatcher<Object> matcher, String description) {
    this.matcher = matcher;
    this.description = description;
  }

  public boolean matches(Object argument) {
    return matcher.matches(argument);
  }

  @Override
  public String toString() {
    return description;
  }
}

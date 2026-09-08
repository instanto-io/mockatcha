/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.ArgumentMatcher;
import java.util.function.Consumer;

/** An argument matcher together with the text a failure message should show for it. */
public final class RegisteredMatcher {

  private final ArgumentMatcher<Object> matcher;
  private final String description;
  private final Consumer<Object> onMatch;

  RegisteredMatcher(ArgumentMatcher<Object> matcher, String description) {
    this(matcher, description, argument -> {});
  }

  RegisteredMatcher(
      ArgumentMatcher<Object> matcher, String description, Consumer<Object> onMatch) {
    this.matcher = matcher;
    this.description = description;
    this.onMatch = onMatch;
  }

  public boolean matches(Object argument) {
    return matcher.matches(argument);
  }

  public void capture(Object argument) {
    onMatch.accept(argument);
  }

  @Override
  public String toString() {
    return description;
  }
}

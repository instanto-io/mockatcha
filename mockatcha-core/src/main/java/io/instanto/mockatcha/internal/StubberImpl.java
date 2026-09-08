/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.Answer;
import io.instanto.mockatcha.Stubber;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Collects answers until the call they describe is made. */
public final class StubberImpl implements Stubber {

  private final List<Answer<?>> answers = new ArrayList<>();

  public StubberImpl() {}

  @Override
  public Stubber doReturn(Object value) {
    answers.add(invocation -> value);
    return this;
  }

  @Override
  public Stubber doThrow(Throwable throwable) {
    Objects.requireNonNull(throwable, "throwable");
    answers.add(
        invocation -> {
          throw throwable;
        });
    return this;
  }

  @Override
  public Stubber doAnswer(Answer<?> answer) {
    answers.add(Objects.requireNonNull(answer, "answer"));
    return this;
  }

  @Override
  public Stubber doNothing() {
    answers.add(invocation -> null);
    return this;
  }

  @Override
  public <T> T when(T mock) {
    if (answers.isEmpty()) {
      throw new IllegalStateException("when(mock) requires at least one arranged answer");
    }
    MockRuntime.beginStubbing(mock, answers);
    return mock;
  }
}

package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.Answer;
import io.instanto.mockatcha.OngoingStubbing;
import java.util.Objects;

final class OngoingStubbingImpl<T> implements OngoingStubbing<T> {

  private final Stub stub;

  OngoingStubbingImpl(Stub stub) {
    this.stub = stub;
  }

  @Override
  public OngoingStubbing<T> thenReturn(T value) {
    stub.add(invocation -> value);
    return this;
  }

  @Override
  public OngoingStubbing<T> thenThrow(Throwable throwable) {
    Objects.requireNonNull(throwable, "throwable");
    stub.add(
        invocation -> {
          throw throwable;
        });
    return this;
  }

  @Override
  public OngoingStubbing<T> thenAnswer(Answer<? extends T> answer) {
    stub.add(Objects.requireNonNull(answer, "answer"));
    return this;
  }
}

package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.Answer;
import io.instanto.mockatcha.BDDOngoingStubbing;
import io.instanto.mockatcha.OngoingStubbing;

/** The given-first spelling of {@link OngoingStubbing}. */
public final class BDDOngoingStubbingImpl<T> implements BDDOngoingStubbing<T> {

  private final OngoingStubbing<T> stubbing;

  public BDDOngoingStubbingImpl(OngoingStubbing<T> stubbing) {
    this.stubbing = stubbing;
  }

  @Override
  public BDDOngoingStubbing<T> willReturn(T value) {
    stubbing.thenReturn(value);
    return this;
  }

  @Override
  public BDDOngoingStubbing<T> willThrow(Throwable throwable) {
    stubbing.thenThrow(throwable);
    return this;
  }

  @Override
  public BDDOngoingStubbing<T> willAnswer(Answer<? extends T> answer) {
    stubbing.thenAnswer(answer);
    return this;
  }
}

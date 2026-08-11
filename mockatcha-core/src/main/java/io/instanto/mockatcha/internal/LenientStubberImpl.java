package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.Answer;
import io.instanto.mockatcha.LenientStubber;
import io.instanto.mockatcha.OngoingStubbing;
import io.instanto.mockatcha.Stubber;

/** Marks whichever stub is arranged next as lenient. */
public final class LenientStubberImpl implements LenientStubber {

  public LenientStubberImpl() {
    MockRuntime.nextStubIsLenient();
  }

  @Override
  public <T> OngoingStubbing<T> when(T ignored) {
    return MockRuntime.when();
  }

  @Override
  public Stubber doReturn(Object value) {
    return new StubberImpl().doReturn(value);
  }

  @Override
  public Stubber doThrow(Throwable throwable) {
    return new StubberImpl().doThrow(throwable);
  }

  @Override
  public Stubber doAnswer(Answer<?> answer) {
    return new StubberImpl().doAnswer(answer);
  }

  @Override
  public Stubber doNothing() {
    return new StubberImpl().doNothing();
  }
}

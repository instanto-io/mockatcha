package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.Answer;
import io.instanto.mockatcha.BDDStubber;
import io.instanto.mockatcha.Stubber;

/** The given-first spelling of {@link Stubber}. */
public final class BDDStubberImpl implements BDDStubber {

  private final Stubber stubber = new StubberImpl();

  public BDDStubberImpl() {}

  @Override
  public BDDStubber willReturn(Object value) {
    stubber.doReturn(value);
    return this;
  }

  @Override
  public BDDStubber willThrow(Throwable throwable) {
    stubber.doThrow(throwable);
    return this;
  }

  @Override
  public BDDStubber willAnswer(Answer<?> answer) {
    stubber.doAnswer(answer);
    return this;
  }

  @Override
  public BDDStubber willDoNothing() {
    stubber.doNothing();
    return this;
  }

  @Override
  public <T> T given(T mock) {
    return stubber.when(mock);
  }
}

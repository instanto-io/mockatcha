package io.instanto.mockatcha;

/**
 * Arranges answers before the call they describe is written.
 *
 * <p>The given-first spelling of {@link Stubber}.
 */
public interface BDDStubber {

  BDDStubber willReturn(Object value);

  BDDStubber willThrow(Throwable throwable);

  BDDStubber willAnswer(Answer<?> answer);

  BDDStubber willDoNothing();

  /** Returns the mock so the call being configured can be written next. */
  <T> T given(T mock);
}

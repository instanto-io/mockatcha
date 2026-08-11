package io.instanto.mockatcha;

/**
 * Arranges answers before the call they describe is written.
 *
 * <p>Unlike {@code when(spy.total())}, a spy's real method never runs during setup.
 */
public interface Stubber {

  Stubber doReturn(Object value);

  Stubber doThrow(Throwable throwable);

  Stubber doAnswer(Answer<?> answer);

  /** Arranges the call to do nothing. */
  Stubber doNothing();

  /** Returns the mock so the call being configured can be written next. */
  <T> T when(T mock);
}

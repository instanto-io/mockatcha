package io.instanto.mockatcha;

/**
 * Arranges answers before the call they describe is written.
 *
 * <p>{@code when(spy.total())} has to evaluate {@code spy.total()} to know which call is being
 * configured, which runs the real method once. {@code doReturn(...).when(spy).total()} arranges the
 * answer first and never lets the real method run, which matters when it is slow, destructive, or
 * unavailable in a browser.
 */
public interface Stubber {

  Stubber doReturn(Object value);

  Stubber doThrow(Throwable throwable);

  Stubber doAnswer(Answer<?> answer);

  /** Arranges a void method, or an ignored result, to do nothing. */
  Stubber doNothing();

  /** Returns the mock so the call being configured can be written next. */
  <T> T when(T mock);
}

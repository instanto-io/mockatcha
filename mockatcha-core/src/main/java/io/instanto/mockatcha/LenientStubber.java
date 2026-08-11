package io.instanto.mockatcha;

/**
 * Arranges a stub that {@link Mockatcha#validateStubbing} will not insist on.
 *
 * <p>For shared setup, where a stub is there for the tests that need it.
 */
public interface LenientStubber {

  <T> OngoingStubbing<T> when(T ignored);

  Stubber doReturn(Object value);

  Stubber doThrow(Throwable throwable);

  Stubber doAnswer(Answer<?> answer);

  Stubber doNothing();
}

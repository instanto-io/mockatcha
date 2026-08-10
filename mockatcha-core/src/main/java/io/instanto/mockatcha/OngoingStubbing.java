package io.instanto.mockatcha;

/** Configures the result of the invocation captured by {@link Mockatcha#when(Object)}. */
public interface OngoingStubbing<T> {

  OngoingStubbing<T> thenReturn(T value);

  OngoingStubbing<T> thenThrow(Throwable throwable);

  OngoingStubbing<T> thenAnswer(Answer<? extends T> answer);
}

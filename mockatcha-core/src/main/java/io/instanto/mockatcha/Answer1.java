package io.instanto.mockatcha;

/** Computes a result from a call's single argument. */
@FunctionalInterface
public interface Answer1<T, A> {
  T answer(A argument) throws Throwable;
}

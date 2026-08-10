package io.instanto.mockatcha;

/** Computes a stubbed result from the invocation being answered. */
@FunctionalInterface
public interface Answer<T> {
  T answer(Invocation invocation) throws Throwable;
}

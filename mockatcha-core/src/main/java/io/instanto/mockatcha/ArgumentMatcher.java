package io.instanto.mockatcha;

/** Tests whether one invocation argument satisfies a stub or verification. */
@FunctionalInterface
public interface ArgumentMatcher<T> {
  boolean matches(T argument);
}

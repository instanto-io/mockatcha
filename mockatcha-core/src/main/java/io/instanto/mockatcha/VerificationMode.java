package io.instanto.mockatcha;

/**
 * Describes how many matching invocations verification expects.
 *
 * <p>A mode that needs more than the count overrides {@link #verify(VerificationContext)}.
 */
public interface VerificationMode {

  /** Checks the number of calls that matched, and throws an {@link AssertionError} if it is wrong. */
  void verify(int actualCount, String invocationDescription);

  /** Checks a verified call with everything the runtime knows about it. */
  default void verify(VerificationContext context) {
    verify(context.matchingCount(), context.description());
  }
}

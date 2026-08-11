package io.instanto.mockatcha;

/**
 * Verifies that calls happened in a given order, across one mock or several.
 *
 * <p>Each {@code verify} consumes the calls it matches, so the next one looks only at what came
 * after. Calls in between are allowed.
 *
 * <pre>{@code
 * InOrder order = inOrder(repository, notifications);
 *
 * order.verify(repository).save(report);
 * order.verify(notifications).send("saved");
 * }</pre>
 *
 * <p>Works with {@code times}, {@code never}, {@code atLeast}, and {@code atLeastOnce}.
 */
public interface InOrder {

  /** Verifies that the following call happened once, after everything already verified. */
  <T> T verify(T mock);

  /** Verifies the following call with a counting rule, after everything already verified. */
  <T> T verify(T mock, VerificationMode mode);

  /** Expects nothing else to have happened on these mocks after the last verified call. */
  void verifyNoMoreInteractions();
}

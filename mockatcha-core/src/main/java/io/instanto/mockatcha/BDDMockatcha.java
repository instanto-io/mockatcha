package io.instanto.mockatcha;

import io.instanto.mockatcha.internal.BDDOngoingStubbingImpl;
import io.instanto.mockatcha.internal.BDDStubberImpl;
import io.instanto.mockatcha.internal.MockRuntime;

/**
 * The same mocking, spelled the way a given-when-then test reads.
 *
 * <pre>{@code
 * // given
 * given(repository.findDraft("A-17")).willReturn(new Timesheet("A-17", 38));
 *
 * // when
 * Submission result = service.submit("A-17");
 *
 * // then
 * then(repository).should().markSubmitted("A-17");
 * then(notifications).shouldHaveNoInteractions();
 * }</pre>
 *
 * <p>Every method here has an equivalent in {@link Mockatcha}, and the two can be mixed.
 */
public final class BDDMockatcha {

  private BDDMockatcha() {}

  /** Begins configuration of the call just evaluated, as {@code when} does. */
  public static <T> BDDOngoingStubbing<T> given(T ignored) {
    return new BDDOngoingStubbingImpl<>(MockRuntime.when());
  }

  /** Arranges a result without evaluating the call, as {@code doReturn} does. */
  public static BDDStubber willReturn(Object value) {
    return new BDDStubberImpl().willReturn(value);
  }

  /** Arranges a failure without evaluating the call, as {@code doThrow} does. */
  public static BDDStubber willThrow(Throwable throwable) {
    return new BDDStubberImpl().willThrow(throwable);
  }

  /** Arranges a calculated result without evaluating the call, as {@code doAnswer} does. */
  public static BDDStubber willAnswer(Answer<?> answer) {
    return new BDDStubberImpl().willAnswer(answer);
  }

  /** Arranges a call to do nothing, as {@code doNothing} does. */
  public static BDDStubber willDoNothing() {
    return new BDDStubberImpl().willDoNothing();
  }

  /** Begins the checks on one mock. */
  public static <T> Then<T> then(T mock) {
    return new Then<>(mock);
  }

  /** The checks a given-when-then test makes on one mock. */
  public static final class Then<T> {

    private final T mock;

    private Then(T mock) {
      this.mock = mock;
    }

    /** Expects the following call to have happened once. */
    public T should() {
      return Mockatcha.verify(mock);
    }

    /** Expects the following call to have happened as often as the rule says. */
    public T should(VerificationMode mode) {
      return Mockatcha.verify(mock, mode);
    }

    /** Expects the following call to come next in this order. */
    public T should(InOrder order) {
      return order.verify(mock);
    }

    /** Expects the following call to come next in this order, as often as the rule says. */
    public T should(InOrder order, VerificationMode mode) {
      return order.verify(mock, mode);
    }

    /** Expects this mock to have been called at all. */
    public void shouldHaveNoInteractions() {
      Mockatcha.verifyNoInteractions(mock);
    }

    /** Expects every call on this mock to have been verified already. */
    public void shouldHaveNoMoreInteractions() {
      Mockatcha.verifyNoMoreInteractions(mock);
    }
  }
}

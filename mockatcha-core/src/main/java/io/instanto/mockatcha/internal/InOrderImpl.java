package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.InOrder;
import io.instanto.mockatcha.Invocation;
import io.instanto.mockatcha.VerificationContext;
import io.instanto.mockatcha.VerificationMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Ordered verification over a fixed set of mocks. */
public final class InOrderImpl implements InOrder {

  private final List<Object> mocks;
  private int cursor = -1;

  public InOrderImpl(Object... mocks) {
    if (mocks.length == 0) {
      throw new IllegalArgumentException("inOrder() needs at least one mock");
    }
    this.mocks = new ArrayList<>(mocks.length);
    for (Object mock : mocks) {
      if (!MockRuntime.isMock(mock)) {
        throw new IllegalArgumentException("inOrder() needs objects created by Mockatcha");
      }
      this.mocks.add(mock);
    }
  }

  @Override
  public <T> T verify(T mock) {
    return verify(mock, CountingMode.exact(1));
  }

  @Override
  public <T> T verify(T mock, VerificationMode mode) {
    requireKnown(mock);
    MockRuntime.beginOrderedVerification(mock, mode, this);
    return mock;
  }

  @Override
  public void verifyNoMoreInteractions() {
    List<Invocation> remaining = unverified();
    if (!remaining.isEmpty()) {
      throw new AssertionError(
          "Wanted no further calls in this order, but observed " + remaining.get(0));
    }
  }

  /**
   * Counts the matching calls that follow everything already verified, then consumes them.
   *
   * <p>An exact count first looks at the run of matching calls that starts next, which lets
   * {@code times(1)} succeed when a later call matches too. If that run is not the size wanted,
   * every matching call after the cursor is considered instead.
   */
  void check(Object mock, InvocationPattern pattern, VerificationMode mode) {
    List<Invocation> remaining = unverified();
    List<Invocation> matched = matchesIn(remaining, mock, pattern);

    if (mode instanceof CountingMode) {
      CountingMode counting = (CountingMode) mode;
      rejectUnorderable(counting);
      if (counting.kind() == CountingMode.Kind.EXACT) {
        List<Invocation> run = firstRunIn(remaining, mock, pattern);
        if (run.size() == counting.count()) {
          matched = run;
        }
      }
    }

    mode.verify(new VerificationContext(matched.size(), remaining.size(), pattern.toString()));

    if (!matched.isEmpty()) {
      cursor = matched.get(matched.size() - 1).sequence();
    }
  }

  private static void rejectUnorderable(CountingMode mode) {
    if (mode.kind() == CountingMode.Kind.AT_MOST || mode.kind() == CountingMode.Kind.ONLY) {
      throw new IllegalArgumentException(
          "atMost() and only() cannot be used with inOrder(); use times, never, atLeast, or"
              + " atLeastOnce");
    }
  }

  private List<Invocation> unverified() {
    List<Invocation> remaining = new ArrayList<>();
    for (Object mock : mocks) {
      for (Invocation invocation : MockRuntime.invocations(mock)) {
        if (invocation.sequence() > cursor) {
          remaining.add(invocation);
        }
      }
    }
    remaining.sort(Comparator.comparingInt(Invocation::sequence));
    return remaining;
  }

  private static List<Invocation> matchesIn(
      List<Invocation> invocations, Object mock, InvocationPattern pattern) {
    List<Invocation> matched = new ArrayList<>();
    for (Invocation invocation : invocations) {
      if (matches(invocation, mock, pattern)) {
        matched.add(invocation);
      }
    }
    return matched;
  }

  private static List<Invocation> firstRunIn(
      List<Invocation> invocations, Object mock, InvocationPattern pattern) {
    List<Invocation> run = new ArrayList<>();
    for (Invocation invocation : invocations) {
      if (matches(invocation, mock, pattern)) {
        run.add(invocation);
      } else if (!run.isEmpty()) {
        break;
      }
    }
    return run;
  }

  private static boolean matches(
      Invocation invocation, Object mock, InvocationPattern pattern) {
    return invocation.mock() == mock
        && pattern.matches(invocation.method(), invocation.arguments().toArray());
  }

  private void requireKnown(Object mock) {
    for (Object known : mocks) {
      if (known == mock) {
        return;
      }
    }
    throw new IllegalArgumentException("This mock was not passed to inOrder()");
  }
}

package io.instanto.mockatcha.internal;

import io.instanto.mockatcha.VerificationContext;
import io.instanto.mockatcha.VerificationMode;

/**
 * A verification mode expressed as a comparison against a count.
 *
 * <p>Ordered verification reads the kind and count back, because how many calls it consumes
 * depends on how many were wanted.
 */
public final class CountingMode implements VerificationMode {

  public enum Kind {
    EXACT,
    AT_LEAST,
    AT_MOST,
    ONLY
  }

  private final Kind kind;
  private final int count;

  private CountingMode(Kind kind, int count) {
    if (count < 0) {
      throw new IllegalArgumentException("An invocation count must not be negative: " + count);
    }
    this.kind = kind;
    this.count = count;
  }

  public static CountingMode exact(int count) {
    return new CountingMode(Kind.EXACT, count);
  }

  public static CountingMode atLeast(int count) {
    return new CountingMode(Kind.AT_LEAST, count);
  }

  public static CountingMode atMost(int count) {
    return new CountingMode(Kind.AT_MOST, count);
  }

  public static CountingMode only() {
    return new CountingMode(Kind.ONLY, 1);
  }

  public Kind kind() {
    return kind;
  }

  public int count() {
    return count;
  }

  @Override
  public void verify(int actualCount, String invocationDescription) {
    verify(new VerificationContext(actualCount, actualCount, invocationDescription));
  }

  @Override
  public void verify(VerificationContext context) {
    int actual = context.matchingCount();
    switch (kind) {
      case EXACT:
      case ONLY:
        if (actual != count) {
          throw new AssertionError(
              "Wanted " + count + " invocation(s) of " + context.description() + " but observed "
                  + actual);
        }
        break;
      case AT_LEAST:
        if (actual < count) {
          throw new AssertionError(
              "Wanted at least " + count + " invocation(s) of " + context.description()
                  + " but observed " + actual);
        }
        break;
      case AT_MOST:
        if (actual > count) {
          throw new AssertionError(
              "Wanted at most " + count + " invocation(s) of " + context.description()
                  + " but observed " + actual);
        }
        break;
      default:
        break;
    }

    if (kind == Kind.ONLY && context.totalCount() != 1) {
      throw new AssertionError(
          "Wanted " + context.description() + " to be the only call on this mock, but "
              + (context.totalCount() - 1) + " other call(s) were recorded");
    }
  }
}

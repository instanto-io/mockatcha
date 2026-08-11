package io.instanto.oolong;

import io.instanto.mockatcha.spi.MockAccess;
import java.util.Objects;

/**
 * A second way of writing Mockatcha tests, taken from Jasmine.
 *
 * <p>Works on the same objects as {@code Mockatcha}, in the same test.
 */
public final class Oolong {

  private static final Clock CLOCK = new Clock();

  private Oolong() {}

  /**
   * Arranges and inspects one method of a mock or spy, chosen by name.
   *
   * <p>Covers every overload of that name unless {@link Spy#withArgs} narrows it. The object must
   * already be a Mockatcha mock or spy.
   *
   * <pre>{@code
   * PricingService pricing = spy(PricingService.class, real);
   * spyOn(pricing, "currency").andReturn("EUR");
   * assertEquals(1, spyOn(pricing, "currency").calls().count());
   * }</pre>
   */
  public static Spy spyOn(Object mock, String methodName) {
    requireMock(mock, "spyOn");
    return new Spy(mock, Objects.requireNonNull(methodName, "methodName"), null);
  }

  /** Reads every call recorded on a mock or spy. */
  public static CallLog calls(Object mock) {
    requireMock(mock, "calls");
    return new CallLog(mock, null, MockAccess.invocations(mock), String.valueOf(mock));
  }

  /** Reads the calls recorded for one method name. */
  public static CallLog calls(Object mock, String methodName) {
    requireMock(mock, "calls");
    Objects.requireNonNull(methodName, "methodName");
    return new CallLog(mock, methodName, MockAccess.invocations(mock, methodName), methodName);
  }

  /** The fake clock, which does nothing until it is installed. There is one per test run. */
  public static Clock clock() {
    return CLOCK;
  }

  private static void requireMock(Object mock, String operation) {
    if (!MockAccess.isMock(mock)) {
      throw new IllegalArgumentException(
          operation
              + "() needs an object created by Mockatcha.mock or Mockatcha.spy, because TeaVM"
              + " cannot replace a method on an object that already exists");
    }
  }
}

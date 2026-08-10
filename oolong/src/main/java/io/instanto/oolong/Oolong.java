package io.instanto.oolong;

import io.instanto.mockatcha.spi.MockAccess;
import java.util.Objects;

/**
 * A Jasmine-shaped vocabulary for tests built on Mockatcha.
 *
 * <p>Mockatcha keeps Mockito's shape, which most Java developers already read fluently. Jasmine
 * solves a few problems Mockito does not have to: it names the method being configured instead of
 * calling it, it reads a spy's call record fluently, and it takes control of the clock. Those three
 * ideas are worth having in a browser test, so they live here rather than being bolted onto the
 * Mockito-shaped API.
 *
 * <p>The two vocabularies work on the same objects. A mock created with {@code Mockatcha.mock} can
 * be configured with {@code spyOn}, verified with {@code Mockatcha.verify}, and inspected with
 * {@code calls} in the same test.
 *
 * <p>One thing did not survive the port. Jasmine's {@code spyOn(object, 'method')} replaces a
 * method on an object that already exists, which JavaScript allows and TeaVM does not. Here the
 * object must already be a Mockatcha mock or spy, and {@code spyOn} arranges its behaviour by name.
 */
public final class Oolong {

  private static final Clock CLOCK = new Clock();

  private Oolong() {}

  /**
   * Arranges and inspects one method of a mock or spy, chosen by name.
   *
   * <p>Every overload of that name is covered unless {@link Spy#withArgs} narrows it.
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
    return new CallLog(MockAccess.invocations(mock), String.valueOf(mock));
  }

  /** Reads the calls recorded for one method name. */
  public static CallLog calls(Object mock, String methodName) {
    requireMock(mock, "calls");
    Objects.requireNonNull(methodName, "methodName");
    return new CallLog(MockAccess.invocations(mock, methodName), methodName);
  }

  /**
   * The fake clock, which does nothing until it is installed.
   *
   * <p>There is one clock per test run, because the browser has one set of timer functions to
   * replace.
   */
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

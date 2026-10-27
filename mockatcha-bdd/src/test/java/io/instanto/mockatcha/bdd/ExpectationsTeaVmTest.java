/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.spy;
import static io.instanto.mockatcha.bdd.Bdd.calls;
import static io.instanto.mockatcha.bdd.Bdd.expect;
import static io.instanto.mockatcha.bdd.Bdd.spyOn;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.instanto.mockatcha.Mockatcha;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ExpectationsTeaVmTest {

  @After
  public void releaseMockatchaState() {
    Mockatcha.releaseMocks();
  }

  @Test
  public void assertsValueEqualityAndInequality() {
    expect(42).toBe(42);
    expect("hello").toBe("hello");
    expect("hello").not().toBe("world");

    ValueExpectation<Integer> exp = expect(42);
    AssertionError failure = assertThrows(AssertionError.class, () -> exp.toBe(99));
    assertTrue(failure.getMessage().contains("Expected: <99> but was: <42>"));
  }

  @Test
  public void assertsDeepEquality() {
    expect(new int[] {1, 2}).toEqual(new int[] {1, 2});
    expect(List.of("a", "b")).toEqual(List.of("a", "b"));
    expect(new int[] {1, 2}).not().toEqual(new int[] {1, 3});
  }

  @Test
  public void assertsNullAndNonNull() {
    expect((Object) null).toBeNull();
    expect("present").toBeNotNull();
    expect("present").not().toBeNull();
    expect((Object) null).not().toBeNotNull();

    ValueExpectation<String> notNullExp = expect("foo");
    assertThrows(AssertionError.class, notNullExp::toBeNull);
    ValueExpectation<Object> nullExp = expect((Object) null);
    assertThrows(AssertionError.class, nullExp::toBeNotNull);
  }

  @Test
  public void assertsBooleanValues() {
    expect(true).toBeTrue();
    expect(false).toBeFalse();
    expect(true).not().toBeFalse();
    expect(false).not().toBeTrue();

    ValueExpectation<Boolean> falseExp = expect(false);
    assertThrows(AssertionError.class, falseExp::toBeTrue);
  }

  @Test
  public void assertsTruthyAndFalsy() {
    expect("non-empty").toBeTruthy();
    expect(10).toBeTruthy();
    expect(List.of("item")).toBeTruthy();

    expect((Object) null).toBeFalsy();
    expect("").toBeFalsy();
    expect(0).toBeFalsy();
    expect(List.of()).toBeFalsy();
    expect(false).toBeFalsy();
  }

  @Test
  public void assertsInstanceOf() {
    expect("text").toBeInstanceOf(String.class);
    expect(42).toBeInstanceOf(Number.class);
    expect("text").not().toBeInstanceOf(Integer.class);

    ValueExpectation<Integer> intExp = expect(42);
    assertThrows(AssertionError.class, () -> intExp.toBeInstanceOf(String.class));
  }

  @Test
  public void assertsContainmentOnStringsCollectionsMapsAndArrays() {
    expect("hello world").toContain("world");
    expect("hello world").not().toContain("bye");

    expect(List.of("alpha", "beta")).toContain("beta");
    expect(List.of("alpha", "beta")).not().toContain("gamma");

    expect(Map.of("k", "v")).toContain("k");
    expect(Map.of("k", "v")).not().toContain("other");

    expect(new String[] {"x", "y"}).toContain("y");
    expect(new String[] {"x", "y"}).not().toContain("z");
  }

  @Test
  public void assertsSizeAndLength() {
    expect("hello").toHaveLength(5);
    expect(List.of(1, 2, 3)).toHaveSize(3);
    expect(Map.of("a", 1)).toHaveSize(1);
    expect(new int[] {9, 8}).toHaveLength(2);

    expect(List.of(1)).not().toHaveSize(2);
  }

  @Test
  public void assertsRegexMatching() {
    expect("employee-A-17").toMatch("^employee-[A-Z]-\\d+$");
    expect("invalid").not().toMatch("^employee-\\d+$");

    ValueExpectation<String> invalidExp = expect("invalid");
    assertThrows(AssertionError.class, () -> invalidExp.toMatch("^employee-\\d+$"));
  }

  @Test
  public void assertsComparisons() {
    expect(10).toBeGreaterThan(5);
    expect(5).toBeLessThan(10);
    expect(10).toBeGreaterThanOrEqual(10);
    expect(10).toBeGreaterThanOrEqual(9);
    expect(5).toBeLessThanOrEqual(5);

    expect(5).not().toBeGreaterThan(10);
  }

  @Test
  public void assertsNoInteractionsOnMock() {
    Greeter mock = mock(Greeter.class);

    expect(mock).toHaveNoInteractions();

    mock.hello("Ada");

    ValueExpectation<Greeter> mockExp = expect(mock);
    AssertionError failure = assertThrows(AssertionError.class, mockExp::toHaveNoInteractions);
    assertTrue(failure.getMessage().contains("Expected no interactions"));

    expect(mock).not().toHaveNoInteractions();
  }

  @Test
  public void assertsExceptionsWithThrowingExpectation() {
    expect(
            () -> {
              throw new IllegalArgumentException("invalid id: A-0");
            })
        .toThrow(IllegalArgumentException.class);

    expect(
            () -> {
              throw new IllegalArgumentException("invalid id: A-0");
            })
        .toThrow("invalid id");

    expect(
            () -> {
              throw new IllegalArgumentException("invalid id: A-0");
            })
        .toThrow(IllegalArgumentException.class, "A-0");

    expect(
            () -> {
              // no exception
            })
        .not()
        .toThrow();

    ThrowingExpectation noThrowExp = expect(() -> {});
    assertThrows(AssertionError.class, noThrowExp::toThrow);

    ThrowingRunnable throwingAction =
        () -> {
          throw new IllegalStateException("error");
        };
    ThrowingExpectation invertedExp = expect(throwingAction).not();
    assertThrows(AssertionError.class, invertedExp::toThrow);
  }

  @Test
  public void assertsMethodCallsOnSpyAndMock() {
    Greeter real = new RealGreeter();
    Greeter greeter = spy(Greeter.class, real);
    spyOn(greeter, "hello").andReturn("Hi");

    expect(greeter, "hello").not().toHaveBeenCalled();

    greeter.hello("Ada");
    greeter.hello("Bob");

    expect(greeter, "hello").toHaveBeenCalled();
    expect(greeter, "hello").toHaveBeenCalledTimes(2);
    expect(greeter, "hello").toHaveBeenCalledWith("Ada");
    expect(greeter, "hello").toHaveBeenCalledWith("Bob");
    expect(greeter, "hello").not().toHaveBeenCalledWith("Charlie");

    expect(greeter, "hello").toHaveBeenLastCalledWith("Bob");
    expect(greeter, "hello").toHaveBeenNthCalledWith(1, "Ada");
    expect(greeter, "hello").toHaveBeenNthCalledWith(2, "Bob");
  }

  @Test
  public void assertsOnSpyInstanceDirectly() {
    Greeter greeter = mock(Greeter.class);
    Spy spy = spyOn(greeter, "hello").andReturn("Hi");

    expect(spy).not().toHaveBeenCalled();

    greeter.hello("Ada");

    expect(spy).toHaveBeenCalled();
    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy).toHaveBeenCalledWith("Ada");
  }

  @Test
  public void assertsOnCallLog() {
    Greeter greeter = mock(Greeter.class);

    expect(calls(greeter, "hello")).toBeEmpty();
    expect(calls(greeter)).toBeEmpty();

    greeter.hello("Ada");

    expect(calls(greeter, "hello")).toHaveCount(1);
    expect(calls(greeter, "hello")).not().toBeEmpty();
  }

  public interface Greeter {
    String hello(String name);
  }

  public static class RealGreeter implements Greeter {
    @Override
    public String hello(String name) {
      return "Hello " + name;
    }
  }
}

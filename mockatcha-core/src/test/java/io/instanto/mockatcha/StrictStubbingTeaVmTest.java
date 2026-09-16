/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.ArgumentMatchers.anyString;
import static io.instanto.mockatcha.Mockatcha.doReturn;
import static io.instanto.mockatcha.Mockatcha.lenient;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.reset;
import static io.instanto.mockatcha.Mockatcha.spy;
import static io.instanto.mockatcha.Mockatcha.validateStubbing;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/** Reporting stubs that no call ever matched. */
@RunWith(TeaVMTestRunner.class)
public class StrictStubbingTeaVmTest extends MockatchaTestLifecycle {

  @After
  public void forgetMocksFromThisTest() {
    // Each test checks the mocks it names, so drain the rest before the next test runs.
    try {
      validateStubbing();
    } catch (AssertionError alreadyAsserted) {
      // The test itself decided whether an unused stub was the point.
    }
  }

  @Test
  public void acceptsAStubThatWasUsed() {
    Store store = mock(Store.class);
    when(store.find("A-17")).thenReturn("timesheet");

    store.find("A-17");

    validateStubbing(store);
  }

  @Test
  public void reportsAStubThatWasNeverUsed() {
    Store store = mock(Store.class);
    when(store.find("A-17")).thenReturn("timesheet");

    AssertionError failure =
        assertThrows(AssertionError.class, () -> validateStubbing(store));

    assertTrue(failure.getMessage(), failure.getMessage().contains("never used"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("find"));
  }

  @Test
  public void showsTheCallsThatWereMadeInstead() {
    Store store = mock(Store.class);
    when(store.find("A-17")).thenReturn("timesheet");

    store.find("A-18");

    AssertionError failure =
        assertThrows(AssertionError.class, () -> validateStubbing(store));

    assertTrue(failure.getMessage(), failure.getMessage().contains("called with"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("A-18"));
  }

  @Test
  public void saysWhenTheMethodWasNeverCalledAtAll() {
    Store store = mock(Store.class);
    when(store.find("A-17")).thenReturn("timesheet");

    store.audit("something else");

    AssertionError failure =
        assertThrows(AssertionError.class, () -> validateStubbing(store));

    assertTrue(failure.getMessage(), failure.getMessage().contains("never called"));
  }

  @Test
  public void acceptsAStubMarkedLenient() {
    Store store = mock(Store.class);
    lenient().when(store.find("A-17")).thenReturn("timesheet");

    validateStubbing(store);
  }

  @Test
  public void acceptsALenientArrangedAnswer() {
    Store store = mock(Store.class);
    lenient().doReturn("timesheet").when(store).find("A-17");

    validateStubbing(store);
  }

  @Test
  public void leniencyAppliesToOneStubOnly() {
    Store store = mock(Store.class);
    lenient().when(store.find("A-17")).thenReturn("timesheet");
    when(store.find("A-18")).thenReturn("another");

    assertThrows(AssertionError.class, () -> validateStubbing(store));
  }

  @Test
  public void countsAnArrangedAnswerAsAStub() {
    Store store = mock(Store.class);
    doReturn("timesheet").when(store).find("A-17");

    assertThrows(AssertionError.class, () -> validateStubbing(store));
  }

  @Test
  public void countsAStubOnASpy() {
    RealStore real = new RealStore();
    RealStore store = spy(RealStore.class, real);
    doReturn("stubbed").when(store).find("A-17");

    assertThrows(AssertionError.class, () -> validateStubbing(store));
  }

  @Test
  public void aMatcherStubCountsAsUsedByAnyMatchingCall() {
    Store store = mock(Store.class);
    when(store.find(anyString())).thenReturn("timesheet");

    assertEquals("timesheet", store.find("anything"));

    validateStubbing(store);
  }

  @Test
  public void resettingAMockRemovesItsStubs() {
    Store store = mock(Store.class);
    when(store.find("A-17")).thenReturn("timesheet");

    reset(store);

    validateStubbing(store);
  }

  @Test
  public void checksEveryMockMadeSinceTheLastCheckWhenGivenNone() {
    Store first = mock(Store.class);
    when(first.find("A-17")).thenReturn("timesheet");
    first.find("A-17");
    validateStubbing();

    Store second = mock(Store.class);
    when(second.find("A-18")).thenReturn("another");

    assertThrows(AssertionError.class, Mockatcha::validateStubbing);
  }

  @Test
  public void strictModeFailsACallThatMissesItsStub() {
    Mockatcha.strictStubs(true);
    try {
      Store store = mock(Store.class);
      when(store.find("A-17")).thenReturn("timesheet");

      AssertionError failure = assertThrows(AssertionError.class, () -> store.find("A-18"));

      assertTrue(failure.getMessage(), failure.getMessage().contains("A-18"));
      assertTrue(failure.getMessage(), failure.getMessage().contains("A-17"));
      assertTrue(failure.getMessage(), failure.getMessage().contains("wrong arguments"));
    } finally {
      Mockatcha.strictStubs(false);
    }
  }

  @Test
  public void strictModeLeavesAMethodWithNoStubsAlone() {
    Mockatcha.strictStubs(true);
    try {
      Store store = mock(Store.class);
      when(store.find("A-17")).thenReturn("timesheet");

      store.audit("unstubbed method, no complaint");

      assertEquals("timesheet", store.find("A-17"));
    } finally {
      Mockatcha.strictStubs(false);
    }
  }

  @Test
  public void strictModeLeavesASpyAlone() {
    Mockatcha.strictStubs(true);
    try {
      RealStore real = new RealStore();
      RealStore store = spy(RealStore.class, real);
      doReturn("stubbed").when(store).find("A-17");

      assertEquals("real", store.find("A-18"));
      assertEquals("stubbed", store.find("A-17"));
    } finally {
      Mockatcha.strictStubs(false);
    }
  }

  @Test
  public void strictModeIgnoresALenientStub() {
    Mockatcha.strictStubs(true);
    try {
      Store store = mock(Store.class);
      lenient().when(store.find("A-17")).thenReturn("timesheet");

      assertEquals(null, store.find("A-18"));
    } finally {
      Mockatcha.strictStubs(false);
    }
  }

  @Test
  public void strictModeDoesNotDisturbVerification() {
    Mockatcha.strictStubs(true);
    try {
      Store store = mock(Store.class);
      when(store.find("A-17")).thenReturn("timesheet");
      store.find("A-17");

      Mockatcha.verify(store).find("A-17");
      Mockatcha.verify(store, Mockatcha.never()).find("A-18");
    } finally {
      Mockatcha.strictStubs(false);
    }
  }

  @Test
  public void refusesAnObjectThatIsNotAMock() {
    assertThrows(IllegalArgumentException.class, () -> validateStubbing(new Object()));
  }

  public interface Store {

    String find(String id);

    void audit(String note);
  }

  public static class RealStore {

    public String find(String id) {
      return "real";
    }
  }
}

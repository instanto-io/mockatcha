/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.examples;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import io.instanto.mockatcha.MockatchaRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * Mockatcha's checks as a rule, in a module that only depends on the fix.
 *
 * <p>Nothing here registers anything: having {@code teavm-rule-support} on the test classpath,
 * ahead of {@code teavm-junit}, is enough.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class MockatchaRuleTeaVmTest {

  @Rule
  public MockatchaRule mockatcha = new MockatchaRule();

  @Test
  public void theRuleTurnedOnStrictStubbing() {
    Store store = mock(Store.class);
    when(store.find("A-17")).thenReturn("timesheet");

    AssertionError failure = assertThrows(AssertionError.class, () -> store.find("A-18"));

    assertEquals(true, failure.getMessage().contains("no arranged answer matched"));
    store.find("A-17");
  }

  @Test
  public void aTestWhoseStubsAreAllUsedPasses() {
    Store store = mock(Store.class);
    when(store.find("A-17")).thenReturn("timesheet");

    assertEquals("timesheet", store.find("A-17"));
  }

  public interface Store {
    String find(String id);
  }
}

/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** The checks a test inherits by extending {@link StrictTest}. */
@RunWith(TeaVMTestRunner.class)
// TeaVMTestRunner collects an inherited @Before once per class in the hierarchy, so StrictTest's
// setup runs twice on the JVM and the second session() call reports a nested session.
@SkipJVM
public class StrictTestBaseTeaVmTest extends StrictTest {

  @Test
  public void aUsedStubPassesTheInheritedCheck() {
    Store store = mock(Store.class);
    when(store.find("A-17")).thenReturn("timesheet");

    assertEquals("timesheet", store.find("A-17"));
  }

  @Test
  public void theCheckRunsForEveryTest() {
    Store store = mock(Store.class);
    when(store.find("A-18")).thenReturn("another");

    assertEquals("another", store.find("A-18"));
  }

  @Test
  public void theInheritedCheckReportsAnUnusedStub() {
    Store store = mock(Store.class);
    when(store.find("A-17")).thenReturn("timesheet");

    // The same call the runner makes after each test.
    AssertionError failure = assertThrows(AssertionError.class, this::checkMockatchaUsage);

    assertTrue(failure.getMessage(), failure.getMessage().contains("never used"));
  }

  public interface Store {
    String find(String id);
  }
}

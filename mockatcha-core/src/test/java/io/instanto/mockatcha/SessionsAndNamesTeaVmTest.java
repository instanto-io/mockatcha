/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.mockingDetails;
import static io.instanto.mockatcha.Mockatcha.session;
import static io.instanto.mockatcha.Mockatcha.spy;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Explicit lifecycle isolation and names carried into diagnostics. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class SessionsAndNamesTeaVmTest {

  @Test
  public void aSessionValidatesAndReleasesTheMocksItOwns() {
    MockatchaSession lifecycle = session();
    Store store = mock(Store.class);
    when(store.find("A-17")).thenReturn("timesheet");

    AssertionError failure = assertThrows(AssertionError.class, lifecycle::close);

    assertTrue(failure.getMessage(), failure.getMessage().contains("never used"));
    assertFalse(mockingDetails(store).isMock());
    assertThrows(IllegalStateException.class, () -> store.find("A-17"));
  }

  @Test
  public void sessionsCannotBeNested() {
    try (MockatchaSession lifecycle = session()) {
      assertThrows(IllegalStateException.class, Mockatcha::session);
    }
  }

  @Test
  public void aNamedMockAppearsInItsVerificationFailure() {
    Store store = mock(Store.class, "timesheet store");

    AssertionError failure =
        assertThrows(AssertionError.class, () -> verify(store).find("A-17"));

    assertTrue(failure.getMessage(), failure.getMessage().contains("timesheet store"));
  }

  @Test
  public void aMockNameMustContainVisibleText() {
    assertThrows(IllegalArgumentException.class, () -> mock(Store.class, "  "));
  }

  @Test
  public void namedSpiesUseTheSameDiagnosticPath() {
    Store store = spy(Store.class, id -> "real " + id, "real store boundary");

    AssertionError failure =
        assertThrows(AssertionError.class, () -> verify(store).find("missing"));

    assertTrue(failure.getMessage(), failure.getMessage().contains("real store boundary"));
  }

  interface Store {
    String find(String id);
  }
}

/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.mockingDetails;
import static io.instanto.mockatcha.Mockatcha.releaseMocks;
import static io.instanto.mockatcha.Mockatcha.validateStubbing;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/** Releasing hands back what a test left unchecked, so a later check does not see it. */
@RunWith(TeaVMTestRunner.class)
public class ReleaseTeaVmTest {

  interface Store {
    String find(String reference);
  }

  @Test
  public void aReleasedMockIsNotReportedByTheNextCheck() {
    Store store = mock(Store.class);
    when(store.find("A-17")).thenReturn("timesheet");

    releaseMocks();

    // Without the release this reports the stub above, as it would for any earlier test's mock.
    validateStubbing();
    assertFalse(mockingDetails(store).isMock());
  }

  @Test
  public void releasingTwiceIsHarmless() {
    Store store = mock(Store.class);
    when(store.find("A-17")).thenReturn("timesheet");

    releaseMocks();
    releaseMocks();

    validateStubbing();
    assertFalse(mockingDetails(store).isMock());
  }
}

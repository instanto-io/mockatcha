/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.examples;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import io.instanto.mockatcha.MockatchaRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** The same example, with Mockatcha's checks attached as a rule rather than a base class. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class TimesheetRuleTest {

  @Rule
  public MockatchaRule mockatcha = new MockatchaRule();

  @Test
  public void theRuleIsReallyRunning() {
    TimesheetRepository timesheets = mock(TimesheetRepository.class);
    when(timesheets.findDraft("A-17")).thenReturn(new Timesheet("A-17", 38));

    // Strict stubbing is on only because the rule turned it on.
    assertThrows(AssertionError.class, () -> timesheets.findDraft("A-18"));

    timesheets.findDraft("A-17");
  }

  @Test
  public void submitsADraftTimesheet() {
    TimesheetRepository timesheets = mock(TimesheetRepository.class);
    ApprovalGateway approvals = mock(ApprovalGateway.class);
    when(timesheets.findDraft("A-17")).thenReturn(new Timesheet("A-17", 38));

    Submission result = new TimesheetService(timesheets, approvals).submit("A-17");

    assertEquals(38, result.hours());
    verify(timesheets).markSubmitted("A-17");
  }
}

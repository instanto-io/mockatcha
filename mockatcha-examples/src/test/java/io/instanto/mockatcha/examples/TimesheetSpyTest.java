/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.examples;

import static io.instanto.mockatcha.Mockatcha.doThrow;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.spy;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * Spying, for a collaborator whose real behaviour is worth keeping.
 *
 * <p>A mock replaces every method. A spy keeps the real object and replaces only the calls a test
 * names, which suits a working in-memory collaborator better than rebuilding its behaviour in
 * stubs.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class TimesheetSpyTest {

  @Test
  public void keepsTheRealRepositoryBehaviourAndStillRecordsCalls() {
    InMemoryTimesheetRepository real = new InMemoryTimesheetRepository();
    real.addDraft(new Timesheet("A-17", 38));
    InMemoryTimesheetRepository timesheets = spy(InMemoryTimesheetRepository.class, real);

    Submission result =
        new TimesheetService(timesheets, mock(ApprovalGateway.class)).submit("A-17");

    assertEquals(38, result.hours());
    verify(timesheets).markSubmitted("A-17");
    assertEquals(List.of("A-17"), real.submitted());
  }

  @Test
  public void replacesOnlyTheCallTheTestNeedsToControl() {
    InMemoryTimesheetRepository real = new InMemoryTimesheetRepository();
    real.addDraft(new Timesheet("A-17", 38));
    InMemoryTimesheetRepository timesheets = spy(InMemoryTimesheetRepository.class, real);

    when(timesheets.findDraft("A-17")).thenReturn(new Timesheet("A-17", 46));

    Submission result =
        new TimesheetService(timesheets, mock(ApprovalGateway.class)).submit("A-17");

    assertTrue(result.reviewRequired());
    // Submission was not stubbed, so the real repository still recorded it.
    assertEquals(List.of("A-17"), real.submitted());
  }

  @Test
  public void arrangesAFailureWithoutRunningTheRealMethodFirst() {
    InMemoryTimesheetRepository real = new InMemoryTimesheetRepository();
    real.addDraft(new Timesheet("A-17", 38));
    InMemoryTimesheetRepository timesheets = spy(InMemoryTimesheetRepository.class, real);

    doThrow(new IllegalStateException("Timesheet store is unavailable"))
        .when(timesheets)
        .markSubmitted("A-17");

    IllegalStateException failure =
        assertThrows(
            IllegalStateException.class,
            () -> new TimesheetService(timesheets, mock(ApprovalGateway.class)).submit("A-17"));

    assertEquals("Timesheet store is unavailable", failure.getMessage());
    assertEquals(List.of(), real.submitted());
  }

  @Test
  public void mocksAConcreteClassWhenNoRealBehaviourIsWanted() {
    InMemoryTimesheetRepository timesheets = mock(InMemoryTimesheetRepository.class);
    when(timesheets.findDraft("A-17")).thenReturn(new Timesheet("A-17", 46));

    Submission result =
        new TimesheetService(timesheets, mock(ApprovalGateway.class)).submit("A-17");

    assertTrue(result.reviewRequired());
    verify(timesheets).markSubmitted("A-17");
    // Every method is replaced, including the one that would have reported real storage.
    assertNull(timesheets.submitted());
  }
}

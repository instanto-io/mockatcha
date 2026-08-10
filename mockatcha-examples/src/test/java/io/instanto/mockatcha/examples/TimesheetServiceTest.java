package io.instanto.mockatcha.examples;

import static io.instanto.mockatcha.ArgumentMatchers.anyInt;
import static io.instanto.mockatcha.ArgumentMatchers.anyString;
import static io.instanto.mockatcha.ArgumentMatchers.eq;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.never;
import static io.instanto.mockatcha.Mockatcha.times;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class TimesheetServiceTest {

  @Test
  public void returnsAResultFromTheDraftSuppliedByTheRepository() {
    TimesheetRepository timesheets = mock(TimesheetRepository.class);
    ApprovalGateway approvals = mock(ApprovalGateway.class);
    when(timesheets.findDraft("A-17")).thenReturn(new Timesheet("A-17", 38));

    Submission result = new TimesheetService(timesheets, approvals).submit("A-17");

    assertEquals(38, result.hours());
    assertFalse(result.reviewRequired());
    verify(timesheets).markSubmitted("A-17");
  }

  @Test
  public void requestsReviewForAnUnusuallyLongTimesheet() {
    TimesheetRepository timesheets = mock(TimesheetRepository.class);
    ApprovalGateway approvals = mock(ApprovalGateway.class);
    when(timesheets.findDraft("A-17")).thenReturn(new Timesheet("A-17", 46));

    Submission result = new TimesheetService(timesheets, approvals).submit("A-17");

    assertTrue(result.reviewRequired());
    verify(approvals).requestReview(eq("A-17"), anyInt());
  }

  @Test
  public void doesNotRequestReviewForAnOrdinaryWeek() {
    TimesheetRepository timesheets = mock(TimesheetRepository.class);
    ApprovalGateway approvals = mock(ApprovalGateway.class);
    when(timesheets.findDraft("A-17")).thenReturn(new Timesheet("A-17", 38));

    new TimesheetService(timesheets, approvals).submit("A-17");

    verify(approvals, never()).requestReview(eq("A-17"), anyInt());
  }

  @Test
  public void canBuildAResultFromTheInvocationArguments() {
    TimesheetRepository timesheets = mock(TimesheetRepository.class);
    ApprovalGateway approvals = mock(ApprovalGateway.class);
    when(timesheets.findDraft(anyString()))
        .thenAnswer(
            invocation -> new Timesheet((String) invocation.argument(0), 40));

    Submission result = new TimesheetService(timesheets, approvals).submit("B-24");

    assertEquals(40, result.hours());
    verify(timesheets).markSubmitted("B-24");
  }

  @Test
  public void canModelAResultThatChangesAcrossCalls() {
    TimesheetRepository timesheets = mock(TimesheetRepository.class);
    ApprovalGateway approvals = mock(ApprovalGateway.class);
    when(timesheets.findDraft("A-17"))
        .thenReturn(new Timesheet("A-17", 38))
        .thenReturn(new Timesheet("A-17", 46));
    TimesheetService service = new TimesheetService(timesheets, approvals);

    assertFalse(service.submit("A-17").reviewRequired());
    assertTrue(service.submit("A-17").reviewRequired());

    verify(timesheets, times(2)).markSubmitted("A-17");
    verify(approvals).requestReview("A-17", 46);
  }

  @Test
  public void keepsARepositoryFailureVisibleToTheCaller() {
    TimesheetRepository timesheets = mock(TimesheetRepository.class);
    ApprovalGateway approvals = mock(ApprovalGateway.class);
    when(timesheets.findDraft("A-17"))
        .thenThrow(new IllegalStateException("Timesheet store is unavailable"));

    IllegalStateException failure =
        assertThrows(
            IllegalStateException.class,
            () -> new TimesheetService(timesheets, approvals).submit("A-17"));

    assertEquals("Timesheet store is unavailable", failure.getMessage());
    verify(approvals, never()).requestReview(eq("A-17"), anyInt());
  }
}

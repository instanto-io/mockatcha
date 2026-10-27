/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.examples;

import static io.instanto.mockatcha.BDDMockatcha.given;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.spy;
import static io.instanto.mockatcha.bdd.Bdd.expect;
import static io.instanto.mockatcha.bdd.Bdd.scenario;

import io.instanto.mockatcha.Mockatcha;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Expressing domain behavior with Mockatcha BDD scenarios and fluent expectations. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class TimesheetBddScenarioTest {

  @After
  public void releaseMockatchaState() {
    Mockatcha.releaseMocks();
  }

  @Test
  public void submittingAStandardTimesheetRequiresNoReview() {
    TimesheetRepository timesheets = mock(TimesheetRepository.class);
    ApprovalGateway approvals = mock(ApprovalGateway.class);
    TimesheetService service = new TimesheetService(timesheets, approvals);

    scenario("Standard timesheet submission")
        .given(
            "a draft timesheet with standard 38 hours",
            () -> {
              given(timesheets.findDraft("ADA-1")).willReturn(new Timesheet("ADA-1", 38));
            })
        .when("submitting the timesheet for ADA-1", () -> service.submit("ADA-1"))
        .then(
            "it records 38 hours and requires no review",
            submission -> {
              expect(submission.hours()).toBe(38);
              expect(submission.reviewRequired()).toBeFalse();
            })
        .and(
            "marks the timesheet as submitted in the repository",
            () -> {
              expect(timesheets, "markSubmitted").toHaveBeenCalledWith("ADA-1");
              expect(timesheets, "markSubmitted").toHaveBeenCalledTimes(1);
            })
        .and(
            "does not contact the approval gateway",
            () -> {
              expect(approvals).toHaveNoInteractions();
            });
  }

  @Test
  public void highHoursTriggerAnApprovalReview() {
    TimesheetRepository timesheets = mock(TimesheetRepository.class);
    ApprovalGateway approvals = mock(ApprovalGateway.class);
    TimesheetService service = new TimesheetService(timesheets, approvals);

    scenario("High hours trigger an approval review")
        .given(
            "a draft timesheet with 48 hours",
            () -> {
              given(timesheets.findDraft("ADA-2")).willReturn(new Timesheet("ADA-2", 48));
            })
        .when("submitting the timesheet for ADA-2", () -> service.submit("ADA-2"))
        .then(
            "it flags that a review is required",
            submission -> {
              expect(submission.reviewRequired()).toBeTrue();
              expect(submission.hours()).toBeGreaterThan(40);
            })
        .and(
            "dispatches a review request to the approval gateway",
            () -> {
              expect(approvals, "requestReview").toHaveBeenCalledWith("ADA-2", 48);
              expect(approvals, "requestReview").toHaveBeenCalledTimes(1);
            })
        .and(
            "still marks the timesheet submitted",
            () -> {
              expect(timesheets, "markSubmitted").toHaveBeenCalledWith("ADA-2");
            });
  }

  @Test
  public void submittingWithoutDraftThrowsAppropriateFailure() {
    TimesheetRepository timesheets = mock(TimesheetRepository.class);
    ApprovalGateway approvals = mock(ApprovalGateway.class);
    TimesheetService service = new TimesheetService(timesheets, approvals);

    scenario("Submitting an unregistered timesheet")
        .given(
            "no draft exists in repository",
            () -> {
              given(timesheets.findDraft("UNKNOWN")).willReturn(null);
            })
        .when(
            "submission is attempted for an unregistered employee",
            () -> {
              expect(() -> service.submit("UNKNOWN"))
                  .toThrow(IllegalStateException.class, "No draft timesheet for UNKNOWN");
            })
        .then(
            "no submission or approval interactions occur",
            () -> {
              expect(timesheets, "markSubmitted").not().toHaveBeenCalled();
              expect(approvals).toHaveNoInteractions();
            });
  }

  @Test
  public void spiesOnRealRepositoryWithBddExpectations() {
    InMemoryTimesheetRepository realRepo = new InMemoryTimesheetRepository();
    realRepo.addDraft(new Timesheet("ADA-3", 35));
    InMemoryTimesheetRepository timesheets = spy(InMemoryTimesheetRepository.class, realRepo);
    ApprovalGateway approvals = mock(ApprovalGateway.class);
    TimesheetService service = new TimesheetService(timesheets, approvals);

    scenario("Spying on a working repository")
        .given(
            "a working in-memory repository",
            () -> {
              expect(realRepo.submitted()).toBeEmpty();
            })
        .when("the service submits through the spy", () -> service.submit("ADA-3"))
        .then(
            "the real repository records the submission",
            submission -> {
              expect(submission.hours()).toBe(35);
              expect(realRepo.submitted()).toContain("ADA-3");
              expect(realRepo.submitted()).toHaveSize(1);
            })
        .and(
            "the spy records the call",
            () -> {
              expect(timesheets, "markSubmitted").toHaveBeenCalledWith("ADA-3");
            });
  }
}

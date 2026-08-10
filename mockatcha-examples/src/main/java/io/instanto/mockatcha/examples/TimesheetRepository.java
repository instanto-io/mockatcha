package io.instanto.mockatcha.examples;

/** Storage boundary used by the timesheet service. */
public interface TimesheetRepository {

  Timesheet findDraft(String employeeId);

  void markSubmitted(String employeeId);
}

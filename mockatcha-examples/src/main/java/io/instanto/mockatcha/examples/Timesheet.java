package io.instanto.mockatcha.examples;

/** A draft timesheet returned by an application boundary. */
public final class Timesheet {

  private final String employeeId;
  private final int hours;

  public Timesheet(String employeeId, int hours) {
    this.employeeId = employeeId;
    this.hours = hours;
  }

  public String employeeId() {
    return employeeId;
  }

  public int hours() {
    return hours;
  }
}

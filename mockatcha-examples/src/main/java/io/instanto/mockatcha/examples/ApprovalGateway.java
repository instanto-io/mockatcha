package io.instanto.mockatcha.examples;

/** External approval boundary used only for unusually long timesheets. */
public interface ApprovalGateway {

  void requestReview(String employeeId, int hours);
}

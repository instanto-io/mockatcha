package io.instanto.mockatcha;

/** Describes how many matching invocations verification expects. */
public interface VerificationMode {

  void verify(int actualCount, String invocationDescription);
}

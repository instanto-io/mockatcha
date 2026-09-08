/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.rules;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Proves that a browser test's teardown failure reaches the rules wrapping its statement. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class AfterFailureTeaVmTest {

  private static final AssertionError AFTER_FAILURE = new AssertionError("after failed");

  @Rule
  public TestRule expectAfterFailure =
      (base, description) ->
          new Statement() {
            @Override
            public void evaluate() throws Throwable {
              try {
                base.evaluate();
              } catch (AssertionError failure) {
                assertEquals(AFTER_FAILURE, failure);
                return;
              }
              throw new AssertionError("The @After failure was swallowed");
            }
          };

  @After
  public void failAfterwards() {
    throw AFTER_FAILURE;
  }

  @Test
  public void bodyPassesBeforeTeardownFails() {}
}

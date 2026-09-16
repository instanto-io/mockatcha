/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.verify;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

/** Diagnostics name a method the same way whichever platform generated the mock. */
@RunWith(TeaVMTestRunner.class)
public class IdentityParityTeaVmTest extends MockatchaTestLifecycle {

  interface Reports {
    int summarise(String[] names);

    String combine(String name, int count);
  }

  @Test
  public void namesParameterTypesTheSameOnBothPlatforms() {
    Reports reports = mock(Reports.class);

    AssertionError failure =
        assertThrows(AssertionError.class, () -> verify(reports).combine("a", 1));

    assertTrue(failure.getMessage(), failure.getMessage().contains("combine(java.lang.String,int)"));
  }

  @Test
  public void namesAnArrayParameterTheSameOnBothPlatforms() {
    Reports reports = mock(Reports.class);

    AssertionError failure =
        assertThrows(AssertionError.class, () -> verify(reports).summarise(new String[] {"a"}));

    assertTrue(failure.getMessage(), failure.getMessage().contains("summarise([Ljava.lang.String;)"));
  }
}

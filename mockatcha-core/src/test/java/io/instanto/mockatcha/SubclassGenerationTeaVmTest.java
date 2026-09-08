/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * The narrowest proof that a generated subclass replaces a real method.
 *
 * <p>The wider behaviour of class mocks lives in {@link MockatchaClassTeaVmTest}. This test exists
 * so that a failure in subclass generation itself is obvious rather than hidden among stubbing and
 * verification failures.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class SubclassGenerationTeaVmTest {

  @Test
  public void generatedSubclassOverridesTheRealMethod() {
    Greeter greeter = mock(Greeter.class);

    when(greeter.describe()).thenReturn("generated");

    assertEquals("generated", greeter.describe());
    assertEquals("real", new Greeter().describe());
    assertTrue(greeter instanceof Greeter);
  }

  public static class Greeter {

    public String describe() {
      return "real";
    }
  }
}

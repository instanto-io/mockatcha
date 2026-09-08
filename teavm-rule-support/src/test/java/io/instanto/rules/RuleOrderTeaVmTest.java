/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.rules;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * The {@code order} attribute, which JUnit documents as "rules with a higher value are inner".
 *
 * <p>Declaration order here is the reverse of application order, so only {@code order} can be
 * deciding the outcome.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class RuleOrderTeaVmTest {

  static final List<String> EVENTS = new ArrayList<>();

  @Rule(order = 10)
  public TestRule declaredFirstWithHighOrder = new RecordingRule("high");

  @Rule(order = 1)
  public TestRule declaredSecondWithLowOrder = new RecordingRule("low");

  @Test
  public void theHigherOrderRuleIsTheInnerOne() {
    EVENTS.add("test");

    assertEquals(List.of("low before", "high before", "test"), EVENTS);
  }

  static final class RecordingRule implements TestRule {

    private final String name;

    RecordingRule(String name) {
      this.name = name;
    }

    @Override
    public Statement apply(Statement base, Description description) {
      return new Statement() {
        @Override
        public void evaluate() throws Throwable {
          EVENTS.add(name + " before");
          base.evaluate();
        }
      };
    }
  }
}

/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.bdd.Bdd.expect;
import static io.instanto.mockatcha.bdd.Bdd.scenario;
import static io.instanto.mockatcha.bdd.Bdd.spyOn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.instanto.mockatcha.Mockatcha;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class ScenarioTeaVmTest {

  @After
  public void releaseMockatchaState() {
    Mockatcha.releaseMocks();
  }

  @Test
  public void executesScenarioWithSupplierValueFlow() {
    Calculator calc = mock(Calculator.class);

    var thenStep =
        scenario("Adding numbers")
            .given(
                "calculator is stubbed to return 10 for (3, 7)",
                () -> {
                  spyOn(calc, "add").withArgs(3, 7).andReturn(10);
                })
            .when("adding 3 and 7", () -> calc.add(3, 7))
            .then(
                "the result is 10",
                sum -> {
                  expect(sum).toBe(10);
                })
            .and(
                "the add method was called once with (3, 7)",
                () -> {
                  expect(calc, "add").toHaveBeenCalledWith(3, 7);
                  expect(calc, "add").toHaveBeenCalledTimes(1);
                });

    assertEquals(Integer.valueOf(10), thenStep.result());
  }

  @Test
  public void executesScenarioWithRunnableActions() {
    AuditLog audit = mock(AuditLog.class);

    scenario("Recording an audit event")
        .given(
            "a clean audit log",
            () -> {
              expect(audit).toHaveNoInteractions();
            })
        .when(
            "recording a login event",
            () -> {
              audit.log("user login: ada");
            })
        .then(
            "the audit event is recorded",
            () -> {
              expect(audit, "log").toHaveBeenCalledWith("user login: ada");
            })
        .but(
            "no logout is recorded",
            () -> {
              expect(audit, "log").not().toHaveBeenCalledWith("user logout: ada");
            });
  }

  @Test
  public void reportsDetailedStepFailureWithScenarioTitle() {
    Calculator calc = mock(Calculator.class);
    spyOn(calc, "add").withArgs(1, 1).andReturn(3); // wrong result intentionally

    Scenario.WhenStep<Integer> step =
        scenario("Basic arithmetic").when("evaluating 1 + 1", () -> calc.add(1, 1));
    Consumer<Integer> verifyStep =
        sum -> {
          expect(sum).toBe(2);
        };

    AssertionError failure =
        assertThrows(AssertionError.class, () -> step.then("it should equal 2", verifyStep));

    assertTrue(failure.getMessage().contains("[Scenario] \"Basic arithmetic\""));
    assertTrue(failure.getMessage().contains("Step failed [then: it should equal 2]"));
    assertTrue(failure.getMessage().contains("Expected: <2> but was: <3>"));
  }

  @Test
  public void reportsUnexpectedExceptionWithStepContext() {
    Scenario s = scenario("Failing operation");
    Supplier<Void> failingAction =
        () -> {
          throw new IllegalStateException("db unavailable");
        };

    AssertionError failure =
        assertThrows(
            AssertionError.class,
            () -> s.when("an action throws an unchecked error", failingAction));

    assertTrue(failure.getMessage().contains("[Scenario] \"Failing operation\""));
    assertTrue(
        failure.getMessage().contains("Step error [when: an action throws an unchecked error]"));
    assertTrue(failure.getMessage().contains("IllegalStateException: db unavailable"));
  }

  public interface Calculator {
    int add(int a, int b);
  }

  public interface AuditLog {
    void log(String message);
  }
}

/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.bdd;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A Given-When-Then BDD scenario flow with step labels and diagnostic error reporting.
 *
 * <pre>{@code
 * scenario("Submitting an ordinary timesheet")
 *     .given("a draft with 38 hours", () -> {
 *         given(timesheets.findDraft("A-17")).willReturn(new Timesheet("A-17", 38));
 *     })
 *     .when("the timesheet is submitted", () -> service.submit("A-17"))
 *     .then("it reports 38 hours", submission -> {
 *         expect(submission.hours()).toBe(38);
 *         expect(submission.reviewRequired()).toBeFalse();
 *     })
 *     .and("marks the timesheet submitted", () -> {
 *         expect(timesheets, "markSubmitted").toHaveBeenCalledWith("A-17");
 *         expect(approvals).toHaveNoInteractions();
 *     });
 * }</pre>
 */
public final class Scenario {

  private static final String ASSERTION_REQUIRED = "assertion";

  private final String title;

  Scenario(String title) {
    this.title = Objects.requireNonNull(title, "title");
  }

  /** Starts a scenario with the given title. */
  public static Scenario scenario(String title) {
    return new Scenario(title);
  }

  /** The title of this scenario. */
  public String title() {
    return title;
  }

  /** Runs a setup step with a narrative description. */
  public Scenario given(String stepDescription, Runnable step) {
    executeStep("given", stepDescription, step);
    return this;
  }

  /** Runs a setup step without a description. */
  public Scenario given(Runnable step) {
    return given(null, step);
  }

  /** Runs an additional setup step with a description. */
  public Scenario and(String stepDescription, Runnable step) {
    executeStep("and", stepDescription, step);
    return this;
  }

  /** Runs an additional setup step without a description. */
  public Scenario and(Runnable step) {
    return and(null, step);
  }

  /** Runs the action under test, capturing its return value for subsequent assertions. */
  public <R> WhenStep<R> when(String stepDescription, Supplier<R> action) {
    R result = executeSupplier("when", stepDescription, action);
    return new WhenStep<>(title, result);
  }

  /** Runs the action under test without a description. */
  public <R> WhenStep<R> when(Supplier<R> action) {
    return when(null, action);
  }

  /** Runs the action under test when no return value is produced. */
  public WhenStep<Void> when(String stepDescription, Runnable action) {
    executeStep("when", stepDescription, action);
    return new WhenStep<>(title, null);
  }

  /** Runs the action under test without a description. */
  public WhenStep<Void> when(Runnable action) {
    return when(null, action);
  }

  private void executeStep(String keyword, String description, Runnable step) {
    Objects.requireNonNull(step, "step");
    try {
      step.run();
    } catch (AssertionError e) {
      throw formatFailure(keyword, description, e);
    } catch (Exception t) {
      throw formatError(keyword, description, t);
    }
  }

  private <R> R executeSupplier(String keyword, String description, Supplier<R> supplier) {
    Objects.requireNonNull(supplier, "action");
    try {
      return supplier.get();
    } catch (AssertionError e) {
      throw formatFailure(keyword, description, e);
    } catch (Exception t) {
      throw formatError(keyword, description, t);
    }
  }

  static AssertionError formatFailure(
      String title, String keyword, String description, AssertionError cause) {
    String stepLabel =
        description == null || description.isBlank() ? keyword : keyword + ": " + description;
    String message =
        "\n[Scenario] \"" + title + "\"\n  Step failed [" + stepLabel + "]: " + cause.getMessage();
    return new AssertionError(message, cause);
  }

  static AssertionError formatError(
      String title, String keyword, String description, Throwable error) {
    String stepLabel =
        description == null || description.isBlank() ? keyword : keyword + ": " + description;
    String message =
        "\n[Scenario] \""
            + title
            + "\"\n  Step error ["
            + stepLabel
            + "]: "
            + error.getClass().getName()
            + ": "
            + error.getMessage();
    return new AssertionError(message, error);
  }

  private AssertionError formatFailure(String keyword, String description, AssertionError cause) {
    return formatFailure(title, keyword, description, cause);
  }

  private AssertionError formatError(String keyword, String description, Throwable error) {
    return formatError(title, keyword, description, error);
  }

  /** The step representing the action under test in a {@link Scenario}. */
  public static final class WhenStep<R> {

    private final String title;
    private final R result;

    WhenStep(String title, R result) {
      this.title = title;
      this.result = result;
    }

    /** The result produced by the action under test. */
    public R result() {
      return result;
    }

    /** Asserts on the result with a narrative description. */
    public ThenStep<R> then(String stepDescription, Consumer<R> assertion) {
      executeConsumer("then", stepDescription, assertion);
      return new ThenStep<>(title, result);
    }

    /** Asserts on the result without a description. */
    public ThenStep<R> then(Consumer<R> assertion) {
      return then(null, assertion);
    }

    /** Asserts on collaborators or side effects with a narrative description. */
    public ThenStep<R> then(String stepDescription, Runnable assertion) {
      executeRunnable("then", stepDescription, assertion);
      return new ThenStep<>(title, result);
    }

    /** Asserts on collaborators or side effects without a description. */
    public ThenStep<R> then(Runnable assertion) {
      return then(null, assertion);
    }

    private void executeConsumer(String keyword, String description, Consumer<R> assertion) {
      Objects.requireNonNull(assertion, ASSERTION_REQUIRED);
      try {
        assertion.accept(result);
      } catch (AssertionError e) {
        throw formatFailure(title, keyword, description, e);
      } catch (Exception t) {
        throw formatError(title, keyword, description, t);
      }
    }

    private void executeRunnable(String keyword, String description, Runnable assertion) {
      Objects.requireNonNull(assertion, ASSERTION_REQUIRED);
      try {
        assertion.run();
      } catch (AssertionError e) {
        throw formatFailure(title, keyword, description, e);
      } catch (Exception t) {
        throw formatError(title, keyword, description, t);
      }
    }
  }

  /** The verification step of a {@link Scenario}. */
  public static final class ThenStep<R> {

    private final String title;
    private final R result;

    ThenStep(String title, R result) {
      this.title = title;
      this.result = result;
    }

    /** The result produced by the action under test. */
    public R result() {
      return result;
    }

    /** Additional assertion on the result with a narrative description. */
    public ThenStep<R> and(String stepDescription, Consumer<R> assertion) {
      executeConsumer("and", stepDescription, assertion);
      return this;
    }

    /** Additional assertion on the result without a description. */
    public ThenStep<R> and(Consumer<R> assertion) {
      return and(null, assertion);
    }

    /** Additional assertion on collaborators or side effects with a narrative description. */
    public ThenStep<R> and(String stepDescription, Runnable assertion) {
      executeRunnable("and", stepDescription, assertion);
      return this;
    }

    /** Additional assertion on collaborators or side effects without a description. */
    public ThenStep<R> and(Runnable assertion) {
      return and(null, assertion);
    }

    /** Contrastive assertion on the result with a narrative description. */
    public ThenStep<R> but(String stepDescription, Consumer<R> assertion) {
      executeConsumer("but", stepDescription, assertion);
      return this;
    }

    /** Contrastive assertion on the result without a description. */
    public ThenStep<R> but(Consumer<R> assertion) {
      return but(null, assertion);
    }

    /** Contrastive assertion on collaborators or side effects with a narrative description. */
    public ThenStep<R> but(String stepDescription, Runnable assertion) {
      executeRunnable("but", stepDescription, assertion);
      return this;
    }

    /** Contrastive assertion on collaborators or side effects without a description. */
    public ThenStep<R> but(Runnable assertion) {
      return but(null, assertion);
    }

    private void executeConsumer(String keyword, String description, Consumer<R> assertion) {
      Objects.requireNonNull(assertion, ASSERTION_REQUIRED);
      try {
        assertion.accept(result);
      } catch (AssertionError e) {
        throw formatFailure(title, keyword, description, e);
      } catch (Exception t) {
        throw formatError(title, keyword, description, t);
      }
    }

    private void executeRunnable(String keyword, String description, Runnable assertion) {
      Objects.requireNonNull(assertion, ASSERTION_REQUIRED);
      try {
        assertion.run();
      } catch (AssertionError e) {
        throw formatFailure(title, keyword, description, e);
      } catch (Exception t) {
        throw formatError(title, keyword, description, t);
      }
    }
  }
}

# BDD helpers for Mockatcha

The `bdd` module provides Jasmine-inspired BDD helpers for
[Mockatcha](../README.md) on both the JVM and TeaVM. It works with mocks and
spies created by Mockatcha, and can be mixed with the core API in the same test.

```java
PricingService pricing = mock(PricingService.class);
spyOn(pricing, "currency").andReturn("EUR");

expect(pricing.currency()).toBe("EUR");
expect(pricing, "currency").toHaveBeenCalledTimes(1);
```

## Use the module

The module includes `mockatcha-core`. Its main entry points are static methods:

```java
import io.instanto.mockatcha.bdd.CallLog;
import io.instanto.mockatcha.bdd.PropertyReader;

import static io.instanto.mockatcha.Mockatcha.*;
import static io.instanto.mockatcha.bdd.Bdd.*;
import static io.instanto.mockatcha.bdd.BddMatchers.*;
import static io.instanto.mockatcha.bdd.ObjectMatchers.*;
```

See [Getting started](../docs/getting-started.md#add-the-dependencies) for TeaVM and JUnit
configuration.

## Stub a method by name

The core API normally identifies a method by calling it:

```java
when(pricing.currency()).thenReturn("EUR");
```

On a spy, that expression calls the real method once during setup. Use `spyOn`
when the call would be slow, destructive, or unavailable in the browser:

```java
PricingService pricing = spy(PricingService.class, realPricing);
spyOn(pricing, "currency").andReturn("EUR");
```

`spyOn` selects a method on an existing Mockatcha mock or spy; it does not
create one.

Without `withArgs`, the stub covers every call to every overload with that
name. Narrow it when only one argument list should change:

```java
spyOn(pricing, "total").withArgs("A-17").andReturn(42);
```

The available answers are:

| Method | Result |
| --- | --- |
| `andReturn(value)` | Return one value. |
| `andReturnValues(a, b)` | Return each value in turn, then repeat the last. |
| `andThrow(failure)` | Throw the supplied failure. |
| `andCallFake(answer)` | Calculate the result from the call. |
| `andStub()` | Return Java's empty value. |
| `andCallThrough()` | Remove stubs for that method name. A spy then calls its real object. |

## Inspect calls

Use `verify` for a direct expectation:

```java
verify(pricing).total("A-17");
```

Use a call log when the test needs to examine several calls or their
arguments. Read it after the calls have happened; each log is a snapshot.

```java
CallLog totals = calls(pricing, "total");

assertEquals(2, totals.count());
assertEquals(List.of("A-17"), totals.first().arguments());
assertEquals("A-18", totals.mostRecent().argument(0));
assertEquals(List.of(List.of("A-17"), List.of("A-18")), totals.allArgs());
```

`any()`, `at(index)`, `argsFor(index)`, and `all()` cover the other common
reads. `calls(pricing)` includes every method on the mock.

`reset()` forgets the calls covered by that log but keeps the stubs:

```java
calls(pricing, "total").reset();
calls(pricing).reset();
```

## Match values

The module adds matchers for strings, collections, maps, and runtime types:

```java
when(audit.record(stringContaining("timesheet"))).thenReturn("kept");
when(audit.record(stringMatching("^audit:[A-Z]-\\d+$"))).thenReturn("kept");
when(audit.batch(containing("A-17"))).thenReturn(1);
when(audit.batch(containingExactly("A-16", "A-17"))).thenReturn(2);
when(audit.batch(ofSize(2))).thenReturn(3);
when(audit.tag(mapContaining("employee", "A-17"))).thenReturn("kept");
when(audit.tagAny(instanceOf(String.class))).thenReturn("a string");
```

`containing` allows extra items. `containingExactly` requires the same items,
including the same number of duplicates, but ignores order. Both work with
collections, object arrays, and primitive arrays.

As with the core matchers, if one argument uses a matcher, every argument in
that call must use one.

## Match object properties

A test may check a few properties without comparing the whole object:

```java
verify(repository).save(
        objectContaining(Timesheet.class, "employeeId", "A-17"));

verify(repository).save(
        objectContaining(Timesheet.class,
                Map.of("employeeId", "A-17", "hours", 38)));
```

A property can be a visible field or a public or protected no-argument method,
including `hours()`, `getHours()`, and `isApproved()`. Inherited properties are
included. An unknown property fails immediately and lists the available names.

You can also read a property directly:

```java
PropertyReader<Timesheet> reader = propertiesOf(Timesheet.class);
assertEquals("A-17", reader.read(saved.getValue(), "employeeId"));
```

## Express expectations

The `expect(...)` API provides Jasmine/Jest-style fluent assertions that compile cleanly on both the JVM and TeaVM:

### Values and collections

```java
expect(submission.hours()).toBe(38);
expect(submission.reviewRequired()).toBeFalse();
expect(submission.notes()).toContain("timesheet");
expect(submission.tags()).toHaveSize(2);
expect(submission.employeeId()).toMatch("^A-\\d+$");

// Inverted expectations
expect(submission.hours()).not().toBeGreaterThan(40);
expect(submission.id()).toBeNotNull();
```

### Mocks and spies

```java
expect(pricing, "currency").toHaveBeenCalled();
expect(pricing, "currency").toHaveBeenCalledTimes(1);
expect(pricing, "total").toHaveBeenCalledWith("A-17");
expect(pricing, "total").toHaveBeenLastCalledWith("A-18");
expect(pricing, "total").toHaveBeenNthCalledWith(1, "A-17");

// Whole mock checks
expect(approvals).toHaveNoInteractions();
expect(calls(pricing, "currency")).toHaveCount(1);
```

### Exceptions

```java
expect(() -> service.submit("UNKNOWN"))
    .toThrow(IllegalStateException.class, "No draft timesheet");

expect(() -> service.submit("A-17")).not().toThrow();
```

## Structure Given-When-Then scenarios

The `scenario(...)` builder structures test execution into standard BDD narrative steps with step descriptions, typed value passing, and contextual error diagnostics:

```java
scenario("High hours trigger an approval review")
    .given("a draft timesheet with 48 hours", () -> {
        given(timesheets.findDraft("A-17")).willReturn(new Timesheet("A-17", 48));
    })
    .when("the timesheet is submitted", () -> service.submit("A-17"))
    .then("it flags that a review is required", submission -> {
        expect(submission.reviewRequired()).toBeTrue();
        expect(submission.hours()).toBeGreaterThan(40);
    })
    .and("dispatches a review request to the approval gateway", () -> {
        expect(approvals, "requestReview").toHaveBeenCalledWith("A-17", 48);
    })
    .and("marks the timesheet submitted in repository", () -> {
        expect(timesheets, "markSubmitted").toHaveBeenCalledWith("A-17");
    });
```

If any step fails, the failure message pinpoints the exact step and scenario:

```text
[Scenario] "High hours trigger an approval review"
  Step failed [then: it flags that a review is required]: Expected: <true> but was: <false>
```

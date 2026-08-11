# Oolong

Oolong is a second vocabulary for [Mockatcha](../README.md) tests, borrowed from
Jasmine, the JavaScript testing framework.

Everything here works on ordinary Mockatcha mocks and spies, in the same test,
so you can use as much or as little of it as you like. It adds three things
Mockito has no need for but a browser test does:

- naming a method instead of calling it;
- reading a mock's call record as data; and
- taking control of the clock.

```java
spyOn(pricing, "currency").andReturn("EUR");
assertEquals(1, calls(pricing, "currency").count());
```

| | |
| --- | --- |
| **Get started** | [Setup](#setup) · [Configure by name](#configure-a-method-by-name) |
| **Inspect** | [Read the call record](#read-the-call-record) |
| **Describe arguments** | [Shapes](#describe-the-shape-of-an-argument) · [Object properties](#match-an-object-by-its-properties) |
| **Control time** | [The fake clock](#control-the-clock) |
| **Reference** | [What is not here](#what-is-not-here) |

## Setup

Oolong brings Mockatcha with it:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>oolong</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

## Configure a method by name

Mockatcha's `when(pricing.currency())` has to run `pricing.currency()` to work
out which call you mean. Oolong names the method instead:

```java
spyOn(pricing, "currency").andReturn("EUR");
```

Nothing is called, so a spy's real method never runs during setup.

The object has to be a mock or spy you already made — `spyOn` arranges
behaviour on it, it does not turn a plain object into a spy:

```java
PricingService pricing = spy(PricingService.class, real);
spyOn(pricing, "currency").andReturn("EUR");
```

By default every overload of that name is covered. `withArgs` narrows it to one
set of arguments:

```java
spyOn(pricing, "total").withArgs("A-17").andReturn(42);
```

Once you have named a method, these say what it should do:

| | |
| --- | --- |
| `andReturn(value)` | give back one value |
| `andReturnValues(a, b)` | give back each in turn, repeating the last |
| `andThrow(failure)` | fail |
| `andCallFake(answer)` | work the result out from the arguments |
| `andStub()` | do nothing and return an empty value |
| `andCallThrough()` | forget the arrangement, so a spy's real object answers again |

## Read the call record

`verify` states an expectation and fails on the spot, which is usually what you
want. Reading the record suits the other cases — a diagnostic, an assertion the
counting rules cannot express, or arguments you could not predict:

```java
CallLog totals = calls(pricing, "total");

assertEquals(2, totals.count());
assertEquals(List.of("A-17"), totals.first().arguments());
assertEquals("A-18", totals.mostRecent().argument(0));
```

`count`, `any`, `first`, `mostRecent`, `at`, `argsFor`, and `all` are available.
`calls(mock)` without a method name reads everything recorded on that mock.

## Describe the shape of an argument

These sit alongside Mockatcha's matchers and follow the same rule: if one
argument of a call uses a matcher, they all must.

```java
when(audit.record(stringContaining("timesheet"))).thenReturn("kept");
when(audit.record(stringMatching("^audit:[A-Z]-\\d+$"))).thenReturn("kept");
when(audit.batch(containing("A-17"))).thenReturn(1);
when(audit.batch(ofSize(2))).thenReturn(2);
when(audit.tag(mapContaining("employee", "A-17"))).thenReturn("kept");
```

`containing` and `ofSize` work on arrays and collections; extra items are
allowed, so they describe a shape rather than demand equality.

## Match an object by its properties

When the argument is an object, you often care about two of its fields and not
the rest. Name the type, then name the properties:

```java
verify(repository).save(objectContaining(Timesheet.class, "employeeId", "A-17"));

verify(repository).save(objectContaining(Timesheet.class,
        Map.of("employeeId", "A-17", "hours", 38)));
```

A property is a no-argument accessor — `hours()`, `getHours()`, or
`isApproved()` — or a visible field, whether declared on the class or inherited.
Private members are not properties.

The type has to be written out in full, as it is for `mock`. Misspell a property
and the test fails immediately, listing the names that do exist, rather than
quietly matching nothing.

The same reader is available on its own, which pairs well with a captor:

```java
PropertyReader<Timesheet> reader = propertiesOf(Timesheet.class);
assertEquals("A-17", reader.read(saved.getValue(), "employeeId"));
```

## Control the clock

Code that polls, debounces, retries, or animates is awkward to test against real
time. A test either sleeps — slow, and flaky — or reaches inside the code under
test to get at its timers.

Install the clock and time stops moving on its own:

```java
clock().install();
try {
    component.startPolling();

    verify(feed, never()).refresh();
    clock().tick(1000);
    verify(feed, times(4)).refresh();
} finally {
    clock().uninstall();
}
```

`tick` advances the clock and runs everything that falls due on the way,
including work a callback schedules while it runs.

While installed, the clock covers `setTimeout`, `setInterval`, `clearTimeout`,
`clearInterval`, `requestAnimationFrame`, and `cancelAnimationFrame`, and it is
what `System.currentTimeMillis()`, `new Date()`, `Date.now()`, and
`performance.now()` report:

```java
clock().install().setTime(1_700_000_000_000L);

assertEquals(1_700_000_000_000L, System.currentTimeMillis());
clock().tick(250);
assertEquals(1_700_000_000_250L, System.currentTimeMillis());
```

`setTime` fixes the instant the clock reports, so a test that formats or
compares times reads the same on every run. `pending()` says how much work is
still waiting, which is a readable way to assert that nothing was scheduled.

Two rules:

- **install and uninstall inside one synchronous test.** Anything else that
  schedules work meanwhile is queued rather than run, so an `@Async` test will
  hang.
- **uninstall in a `finally` block or an `@After` method,** so a failing
  assertion does not leave the clock installed for the tests that follow.

There is one clock, because the browser has one set of timer functions.

## What is not here

**`expect(spy).toHaveBeenCalledWith(...)`** — Jasmine's assertion style is left
out on purpose. JUnit's assertions and Mockatcha's `verify` already cover it.

**`createSpyObj('name', ['a', 'b'])`** — builds an object with no type. In Java
the interface already exists, so `mock(Notifier.class)` is shorter and the
compiler checks it.

**Jasmine's in-place `spyOn`** replaces a method on an object that already
exists. TeaVM decides method dispatch while it compiles, so the object has to be
a mock or spy from the start.

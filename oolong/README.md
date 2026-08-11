# Oolong

Oolong is a second vocabulary for [Mockatcha](../README.md) tests, borrowed from
Jasmine, the JavaScript testing framework.

Everything here works on ordinary Mockatcha mocks and spies, in the same test,
so you can use as much or as little of it as you like. It adds three things:

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
| **Reference** | [Coming from Jasmine](#coming-from-jasmine) |

The same guidance applies as for
[Mockatcha itself](../README.md#when-to-use-it): portable logic belongs in a JVM
test, and these are for the code that has to run in a browser. The fake clock is
squarely in that second group.

For tests that render, [`oolong-dom`](../oolong-dom/README.md) adds a container
per test, queries, events, and assertions about elements.

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

Naming it leaves a spy's real method untouched during setup.

`spyOn` arranges behaviour on a mock or spy you have already made:

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

`count`, `any`, `first`, `mostRecent`, `at`, `argsFor`, and `all` are available,
along with `allArgs()` for every call's arguments at once:

```java
assertEquals(List.of(List.of("A-17"), List.of("A-18")), totals.allArgs());
```

`calls(mock)` without a method name reads everything recorded on that mock.

`reset()` forgets the calls a log covers while keeping any arranged behaviour,
which suits a fixture shared between phases of a longer test:

```java
calls(pricing, "total").reset();   // just this method
calls(pricing).reset();            // everything on the mock
```

## Describe the shape of an argument

These sit alongside Mockatcha's matchers and follow the same rule: if one
argument of a call uses a matcher, they all must.

```java
when(audit.record(stringContaining("timesheet"))).thenReturn("kept");
when(audit.record(stringMatching("^audit:[A-Z]-\\d+$"))).thenReturn("kept");
when(audit.batch(containing("A-17"))).thenReturn(1);
when(audit.batch(containingExactly("A-16", "A-17"))).thenReturn(2);
when(audit.batch(ofSize(2))).thenReturn(3);
when(audit.tag(mapContaining("employee", "A-17"))).thenReturn("kept");
when(audit.tagAny(instanceOf(String.class))).thenReturn("a string");
```

`containing` and `containingExactly` both work on arrays and collections and
ignore order. `containing` allows extra items; `containingExactly` expects the
same items and no others.

## Match an object by its properties

When the argument is an object, you often care about two of its fields and not
the rest. Name the type, then name the properties:

```java
verify(repository).save(objectContaining(Timesheet.class, "employeeId", "A-17"));

verify(repository).save(objectContaining(Timesheet.class,
        Map.of("employeeId", "A-17", "hours", 38)));
```

A property is a public or protected no-argument accessor — `hours()`,
`getHours()`, or `isApproved()` — or a visible field, whether declared on the
class or inherited.

The type is written out in full, as it is for `mock`. Misspell a property and
the test fails immediately, listing the names that do exist.

The reader is available on its own, which pairs well with a captor:

```java
PropertyReader<Timesheet> reader = propertiesOf(Timesheet.class);
assertEquals("A-17", reader.read(saved.getValue(), "employeeId"));
```

## Control the clock

Code that polls, debounces, retries, or animates is awkward to test against real
time. Install the clock and time moves only when the test says so:

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
  schedules work meanwhile is queued rather than run. That includes TeaVM's own
  asynchronous support: code that suspends, such as `Thread.sleep`, resumes
  through `setTimeout`, so it will wait for a `tick` that a stalled test never
  reaches.
- **uninstall in a `finally` block or an `@After` method,** so a failing
  assertion does not leave the clock installed for the tests that follow.

There is one clock, because the browser has one set of timer functions.

## Coming from Jasmine

| Jasmine | Oolong |
| --- | --- |
| `spyOn(obj, 'method')` | `spyOn(mock, "method")`, on a mock or spy you created |
| `.and.returnValue(v)` | `.andReturn(v)` |
| `.and.returnValues(a, b)` | `.andReturnValues(a, b)` |
| `.and.throwError(e)` | `.andThrow(e)` |
| `.and.callFake(fn)` | `.andCallFake(answer)` |
| `.and.callThrough()` | `.andCallThrough()` |
| `.and.stub()` | `.andStub()` |
| `.withArgs(...)` | `.withArgs(...)` |
| `spy.calls.count()` | `calls(mock, "method").count()` |
| `spy.calls.argsFor(n)` | `calls(mock, "method").argsFor(n)` |
| `spy.calls.allArgs()` | `calls(mock, "method").allArgs()` |
| `spy.calls.mostRecent()` | `calls(mock, "method").mostRecent()` |
| `spy.calls.reset()` | `calls(mock, "method").reset()` |
| `jasmine.any(Type)` | `instanceOf(Type.class)` |
| `jasmine.stringMatching(re)` | `stringMatching(re)` |
| `jasmine.arrayContaining([...])` | `containing(...)` |
| `jasmine.arrayWithExactContents([...])` | `containingExactly(...)` |
| `jasmine.objectContaining({...})` | `objectContaining(Type.class, ...)` |
| `jasmine.clock().install()` | `clock().install()` |
| `jasmine.clock().tick(ms)` | `clock().tick(ms)` |
| `jasmine.clock().mockDate(d)` | `clock().setTime(millis)` |
| `createSpyObj('n', ['a'])` | `mock(SomeInterface.class)` |
| `expect(spy).toHaveBeenCalled()` | `verify(mock).method()`, from Mockatcha |

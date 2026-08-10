# Oolong

Mockatcha keeps Mockito's shape, because most Java developers already read it
fluently. Jasmine, the JavaScript testing framework, solves three problems
Mockito never had to, and all three turn up in browser tests:

- it names the method being configured instead of calling it;
- it reads a spy's call record as data rather than as an assertion; and
- it takes control of the clock.

Oolong ports those three ideas. It is a layer over Mockatcha, not a
replacement: the two vocabularies work on the same objects, in the same test.

```java
PricingService pricing = spy(PricingService.class, realPricing);

spyOn(pricing, "currency").andReturn("EUR");
assertEquals(1, calls(pricing, "currency").count());
verify(pricing).currency();
```

## Contents

| Area | Jump to |
| --- | --- |
| Configure by name | [spyOn](#configure-a-method-by-name) |
| Read the record | [Call inspection](#read-the-call-record) |
| Control time | [Fake clock](#take-control-of-the-clock) |
| Describe arguments | [Matchers](#describe-the-shape-of-an-argument) |
| Reference | [What did not port](#what-did-not-port) · [Setup](#add-oolong-to-a-teavm-test-suite) |

## Configure a method by name

`when(pricing.currency())` has to evaluate the call to know which one is meant.
That is precise, and it is also the reason a spy runs its real method during
setup. Naming the method avoids the evaluation entirely:

```java
spyOn(pricing, "currency").andReturn("EUR");
```

Every overload of that name is covered. `withArgs` narrows to one set of
arguments:

```java
spyOn(pricing, "total").withArgs("A-17").andReturn(42);
```

The behaviours follow Jasmine's vocabulary:

| Call | Effect |
| --- | --- |
| `andReturn(value)` | answer every matching call with one value |
| `andReturnValues(a, b)` | answer with each value in turn, repeating the last |
| `andThrow(failure)` | fail every matching call |
| `andCallFake(answer)` | calculate a result from the arguments |
| `andStub()` | answer with Java's empty value, and record the call |
| `andCallThrough()` | forget the arrangement, so a spy's real object answers again |

The object passed to `spyOn` must already be a Mockatcha mock or spy. Jasmine
replaces a method on an object that already exists; TeaVM compiles method
dispatch ahead of time and cannot. This is the one place where the port is
shallower than the original, and `spyOn` says so rather than failing obscurely.

## Read the call record

`verify` states an expectation and fails on the spot, which is the clearer way
to describe intended behaviour. Reading the record suits the other cases: a
diagnostic, an assertion the built-in verification modes cannot express, or a
test inspecting arguments it could not predict.

```java
CallLog totals = calls(pricing, "total");

assertEquals(2, totals.count());
assertTrue(totals.any());
assertEquals(List.of("A-17"), totals.first().arguments());
assertEquals("A-18", totals.mostRecent().argument(0));
```

`count`, `any`, `first`, `mostRecent`, `at`, `argsFor`, and `all` are available.
`calls(mock)` without a method name reads everything recorded on that mock.

## Take control of the clock

Code that polls, debounces, retries, or animates is awkward to test against real
time. A test either sleeps, which makes it slow and flaky, or reaches into the
code under test to expose its timers.

Installing the clock replaces the browser's timer functions, so scheduled work
waits until the test says otherwise:

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

`setTimeout`, `setInterval`, `clearTimeout`, `clearInterval`,
`requestAnimationFrame`, `cancelAnimationFrame`, and `Date.now` are all
intercepted, whether the code under test reaches them through TeaVM's
`Window` API or through its own JavaScript interop.

`tick` runs everything due in due order, including work scheduled by a callback
that has just run, and leaves the clock at exactly the requested instant.
`setTime` fixes the instant the clock reports, so a test that formats or
compares times is reproducible. `pending()` reports how much work is still
waiting, which is the readable way to assert that nothing was scheduled.

Two constraints follow from replacing global functions:

- **install and uninstall within one synchronous test.** Anything else that
  schedules work in the meantime — including TeaVM's own asynchronous support —
  is queued rather than run, so an `@Async` test will hang.
- **uninstall in a `finally` block or an `@After` method.** A failing assertion
  must not leave the browser's timers replaced for the tests that follow.

There is one clock, because the browser has one set of timer functions.

## Describe the shape of an argument

These complement Mockatcha's own matchers and follow the same rule: if one
argument of a call uses a matcher, every argument must use one.

```java
when(audit.record(stringMatching("^audit:[A-Z]-\\d+$"))).thenReturn("kept");
when(audit.batch(containing("A-17"))).thenReturn(1);
when(audit.batch(ofSize(2))).thenReturn(2);
when(audit.tag(mapContaining("employee", "A-17"))).thenReturn("kept");
```

`stringContaining`, `stringMatching`, `containing`, `ofSize`, and `mapContaining`
are available. Mockatcha itself supplies `any`, `anyString`, `anyInt`,
`anyLong`, `anyDouble`, the rest of the primitive matchers, `eq`, `isNull`,
`isNotNull`, and `argThat`.

## What did not port

**`jasmine.objectContaining`** inspects arbitrary properties of a JavaScript
object. Java has no portable equivalent — TeaVM strips reflection unless a type
opts in — so `mapContaining` covers the case where the value really is a map,
and `argThat` covers the rest with an ordinary Java condition.

**`expect(spy).toHaveBeenCalledWith(...)`** is deliberately absent. It would
compete with JUnit's assertions and Mockatcha's `verify` without adding
anything either one lacks.

**`createSpyObj('name', ['a', 'b'])`** builds an object with no type. In Java
the interface already exists, so `mock(Notifier.class)` is both shorter and
checked by the compiler.

## Add Oolong to a TeaVM test suite

Oolong brings `mockatcha-core` with it:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>oolong</artifactId>
  <version>${mockatcha.version}</version>
  <scope>test</scope>
</dependency>
```

Unlike Mockatcha, Oolong depends on TeaVM's JavaScript interop, because the
fake clock has to replace the browser's own functions. Tests that use only
`spyOn`, `calls`, and the matchers still run anywhere TeaVM does.

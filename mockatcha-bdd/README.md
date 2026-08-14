# BDD helpers for Mockatcha

The `bdd` module provides additional browser-testing APIs for
[Mockatcha](../README.md). It works with mocks and spies created by Mockatcha,
and can be mixed with the core API in the same test.

```java
PricingService pricing = mock(PricingService.class);
spyOn(pricing, "currency").andReturn("EUR");

assertEquals("EUR", pricing.currency());
assertEquals(1, calls(pricing, "currency").count());
```

## Add the module

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>mockatcha-bdd</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

The module includes `mockatcha-core`. Its main entry points are static methods:

```java
import io.instanto.mockatcha.bdd.CallLog;
import io.instanto.mockatcha.bdd.PropertyReader;

import static io.instanto.mockatcha.Mockatcha.*;
import static io.instanto.mockatcha.bdd.Bdd.*;
import static io.instanto.mockatcha.bdd.BddMatchers.*;
import static io.instanto.mockatcha.bdd.ObjectMatchers.*;
```

See the [main setup guide](../README.md#setup) for TeaVM and JUnit
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

Sometimes a test cares about a few properties rather than the whole object:

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

## Control browser time

Install the clock for one test and always uninstall it:

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

`tick` advances time and runs callbacks as they become due, including callbacks
scheduled by another callback. The clock replaces timeouts, intervals, and
animation frames. It also controls `Date`, `Date.now()`, and
`System.currentTimeMillis()`.

Set a fixed date when the code formats or compares absolute times:

```java
clock().install().setTime(1_700_000_000_000L);
try {
    assertEquals(1_700_000_000_000L, System.currentTimeMillis());
    clock().tick(250);
    assertEquals(1_700_000_000_250L, System.currentTimeMillis());
} finally {
    clock().uninstall();
}
```

`performance.now()` and the timestamp passed to an animation-frame callback
use elapsed time, starting at zero when the clock is installed. `pending()`
returns the number of callbacks still waiting.

### Test a retry without waiting

`RetryingLoader.load()` makes a request immediately. If the request fails, this
code schedules another attempt after one second:

```java
JSPromise<String> load() {
    return new JSPromise<>((resolve, reject) -> {
        try {
            resolve.accept(gateway.load());
        } catch (IllegalStateException firstFailure) {
            Window.setTimeout(() -> {
                try {
                    resolve.accept(gateway.load());
                } catch (Throwable retryFailure) {
                    reject.accept(retryFailure);
                }
            }, 1000);
        }
    });
}
```

The gateway stub below has two outcomes in call order. `loader.load()` makes
the first call, which throws and schedules the timeout. `tick(1000)` fires that
timeout, so the loader makes its second call and receives `"ready"`:

```java
Gateway gateway = mock(Gateway.class);
when(gateway.load())
        .thenThrow(new IllegalStateException("unavailable")) // first call
        .thenReturn("ready");                                // retry
RetryingLoader loader = new RetryingLoader(gateway);

clock().install();
try {
    JSPromise<String> result = loader.load(); // fails and schedules the retry
    clock().tick(1000);                       // runs the scheduled retry

    assertEquals("ready", result.await());
    verify(gateway, times(2)).load();
} finally {
    clock().uninstall();
}
```

`await()` reads the promise result, and the verification confirms that the
gateway was called twice.

For DOM queries, events, and element assertions, continue with
[Mockatcha DOM](../mockatcha-dom/README.md).

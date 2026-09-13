# Mocks and spies

## Mock a class

`mock` accepts a concrete class as well as an interface:

```java
PricingService pricing = mock(PricingService.class);
when(pricing.total("A-17")).thenReturn(42);
```

Name a collaborator when a test uses several mocks of the same type:

```java
PricingService pricing = mock(PricingService.class, "regional pricing");
```

The name appears in verification, strict-stubbing, and unused-stub diagnostics.

A class mock is an instance of the requested class. Its no-argument constructor
runs, and Mockatcha replaces every public and protected method that it can
override.

Class mocks have these constraints:

- the class must be extendable;
- it must have a no-argument constructor;
- `static`, `private`, and `final` methods retain their real behaviour; and
- `equals`, `hashCode`, and `toString` use identity and a fixed description.

Invalid class mocks fail during TeaVM compilation and identify the violated
rule. Use an interface when the design already provides one. Class mocks cover
boundaries whose design you cannot change.

## Spy on an object

A mock replaces all eligible methods. A spy delegates unstubbed calls to an
existing object:

```java
InMemoryTimesheetRepository real = new InMemoryTimesheetRepository();
InMemoryTimesheetRepository timesheets =
        spy(InMemoryTimesheetRepository.class, real);

when(timesheets.findDraft("A-17"))
        .thenReturn(new Timesheet("A-17", 46));
```

The stub replaces `findDraft`. Other calls reach `real` and are recorded, so
verification works as usual:

```java
verify(timesheets).markSubmitted("A-17");
```

An interface and one of its implementations work in the same way:

```java
Notifier notifier = spy(Notifier.class, new EmailNotifier());
```

A spy has two limitations:

- the spy and wrapped object have different identities; and
- a real method that calls another method on `this` reaches the wrapped object
  directly, bypassing stubs on the spy.

For example, a stubbed `currency()` affects direct calls to
`pricingSpy.currency()`. If the wrapped object's `summary()` calls its own
`currency()`, it receives the real result. Use a mock if the test must replace
that internal call.

## Stub a spy without calling the real method

`when(spy.currency())` invokes `currency()` once while arranging the stub. Use
the `do...` form if that call is slow, destructive, or unavailable in a browser:

```java
doReturn("EUR").when(pricingSpy).currency();
doThrow(new IllegalStateException("offline")).when(pricingSpy).currency();
doAnswer(call -> call.argument(0) + "!")
        .when(pricingSpy).describe(anyString());
doNothing().when(pricingSpy).record("audited");
```

The real method is not called during setup. The same forms also work on plain
mocks.

## Use given and then aliases

`mockatcha-core` includes `BDDMockatcha`, which provides `given(...)` and
`then(...)` aliases:

```java
// given
given(repository.findDraft("A-17"))
        .willReturn(new Timesheet("A-17", 38));

// when
Submission result = service.submit("A-17");

// then
then(repository).should().markSubmitted("A-17");
then(notifications).shouldHaveNoInteractions();
```

`given(...).willReturn/willThrow/willAnswer` corresponds to
`when(...).thenReturn/thenThrow/thenAnswer`. `then(mock).should()` corresponds
to `verify(mock)` and accepts counts such as `should(times(2))` or an order via
`should(order)`. `shouldHaveNoInteractions` and
`shouldHaveNoMoreInteractions` correspond to the `verifyNo...` calls.

Use `willReturn(v).given(mock).method()` instead of `doReturn` when setup must
avoid calling a real method.

The aliases delegate to the core API and can be mixed with `when(...)` and
`verify(...)`. They are separate from the
[`mockatcha-bdd` module](../mockatcha-bdd/README.md), which provides a browser
call logs and additional matchers.

Continue with [strictness and lifecycle](strictness-and-lifecycle.md).

[Documentation index](README.md) · [Project README](../README.md)

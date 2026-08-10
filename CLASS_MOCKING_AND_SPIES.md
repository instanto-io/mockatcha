# Class mocking and spies: implementation handoff

Mockatcha currently generates implementations of Java interfaces while TeaVM
compiles a test. The next useful step is to support ordinary classes and spies
without bringing JVM bytecode agents or browser-only APIs into the test runtime.

This document defines that step. It records the behaviour users should see,
the TeaVM constraints behind the design, and a sequence that leaves the
repository in a useful state after every milestone.

## Recommended direction

Generate a subclass for each concrete type while TeaVM compiles the test. Each
overridden method should use the existing `MockRuntime` for matching, stubbing,
call recording, and verification.

Class mocks and spies should share that generated subclass:

- a class mock returns configured answers or Java's empty default value;
- a spy delegates an unstubbed call to a real instance;
- both record and verify calls through the same runtime used by interface
  mocks.

This keeps the public API familiar:

```java
PricingService pricing = mock(PricingService.class);
when(pricing.total("A-17")).thenReturn(42);

PricingService realPricing = new PricingService(exchangeRates);
PricingService pricingSpy = spy(PricingService.class, realPricing);
when(pricingSpy.currency()).thenReturn("EUR");
```

The two-argument spy form is intentional for the first release. TeaVM must
know the concrete class at compilation time, while a generic `spy(T value)`
does not reliably expose that class as a constant to the metaprogramming call.
A convenient `spy(T)` overload can follow only if a prototype proves that its
call-site type is stable in all supported TeaVM builds.

## Why this cannot use the interface generator unchanged

The current `mock(Class<T>)` metaprogram uses TeaVM's `proxy` operation.
`proxy` creates an implementation of an interface; it cannot extend a class.
JavaScript `Proxy` is not an alternative because Mockatcha tests Java method
dispatch after TeaVM has compiled it, not arbitrary JavaScript property access.

TeaVM does provide compile-time class-generation seams. The practical options
to prototype are:

1. Generate subclass bytecode with TeaVM's relocated ASM library and submit it
   through the metaprogramming `createClass` operation.
2. Install a TeaVM compiler plugin and submit a generated `ClassHolder` through
   `ClassHolderTransformerContext`.

Start with the first route because it keeps generation next to the existing
metaprogram and requires less compiler-plugin plumbing. Isolate it behind a
`SubclassGenerator` so a later TeaVM release can replace the deprecated
`createClass` entry point without changing Mockatcha's public API or runtime.
If a minimal constructor-and-one-method prototype is unreliable on TeaVM 0.15,
move that generator behind a compiler plugin rather than accumulating special
cases in `Mockatcha`.

TeaVM's metaprogramming model and its ahead-of-time timing are described in the
[TeaVM metaprogramming documentation](https://www.teavm.org/docs/runtime/metaprogramming.html).

## Supported class boundary for the first release

The first class-mocking milestone should accept a type only when all of these
conditions hold:

- it is a non-final, non-interface class;
- it is not an enum, record, anonymous, local, or sealed class;
- it has an accessible no-argument constructor;
- the methods being mocked are non-static, non-private, and non-final; and
- the generated subclass can legally see every constructor and method it
  emits.

Public and protected instance methods can be intercepted. Package-private
members should wait until generated-package placement has an explicit test.
Private, static, and final methods cannot be overridden and must not appear to
be mocked.

Reject an unsupported type during TeaVM compilation with a message that names
the type and the failed rule. A compile-time error is much more useful than a
partly working object in the browser.

Calling the no-argument constructor is a visible limitation. Mockito can often
allocate objects without running a constructor by using JVM-specific
facilities; Mockatcha cannot assume those facilities in TeaVM. Do not hide this
difference. Constructor-free allocation is a separate research task.

## Generated subclass shape

For a class named `PricingService`, generation should conceptually produce:

```java
final class PricingService$Mockatcha extends PricingService {
    private MockState mockatchaState;
    private PricingService mockatchaDelegate;

    PricingService$Mockatcha(MockState state, PricingService delegate) {
        super();
        this.mockatchaState = state;
        this.mockatchaDelegate = delegate;
    }

    @Override
    public int total(String account) {
        return MockRuntime.invokeInt(
                mockatchaState,
                "total(java.lang.String)",
                new Object[] { account },
                () -> mockatchaDelegate.total(account));
    }
}
```

The real generated form may use a small internal callback interface instead of
the illustrative lambda. The important points are stable method identifiers,
one state object per generated instance, and an optional real-call path.

Generate one subclass per mocked Java class and cache it by source type during
a TeaVM compilation. Instances must still receive separate `MockState`
objects. Use stable generated names so incremental builds do not create a new
class graph on every run.

The generator must walk inherited overridable methods, de-duplicate overrides
by JVM signature, and preserve declared return and thrown types. Synthetic and
bridge methods need explicit tests before they are emitted independently.

## Runtime change: distinguish no stub from a stubbed null

`MockRuntime.invokeObject` currently returns `null` for both an unstubbed call
and a stub explicitly configured with `thenReturn(null)`. A spy must distinguish
those cases: no stub calls the delegate, while a stubbed `null` must return
`null`.

Introduce one internal dispatch result, for example:

```java
final class DispatchResult {
    boolean stubbed();
    Object value();
}
```

The generated method can then apply this order:

1. consume argument matchers or verification state;
2. record the invocation when this is an ordinary call;
3. return or throw the matching stub when one exists;
4. call the real delegate for a spy;
5. otherwise return the Java default for a class mock.

Keep the existing primitive entry points so generated code does not duplicate
boxing and default-value rules. Add parallel spy-aware entry points only if the
generator cannot express the fallback cleanly through one internal dispatch
method.

Verification must never call the delegate. Stubbing with the initial
`when(spy.method())` form will call the real method once while the expression is
evaluated, matching Mockito's familiar behaviour. The runtime must then remove
that setup invocation from history as it already does for mocks.

## Spy behaviour

A spy is a generated subclass that delegates unstubbed overridable methods to
the supplied real object. It is not the same identity as that object, and code
which compares the two with `==` will observe that difference.

The first release should specify these rules:

- the delegate must be non-null and an instance of the supplied class;
- unstubbed overridable methods call the delegate exactly once;
- stubbed methods do not call the delegate after setup;
- verification reads recorded spy calls without invoking the delegate;
- `reset` removes stubs and call history but retains spy delegation;
- `clearInvocations` retains both stubs and delegation; and
- calls made by one real method directly to another on the delegate are not
  intercepted, because `this` inside that method is the delegate.

That final rule is the cost of delegation. A true self-intercepting spy would
have to run inherited method bodies on the generated subclass and reproduce
the original object's fields. Field copying is error-prone and constructor
bypassing is unavailable, so it should not be part of the first milestone.

Add Mockito-shaped `doReturn`/`doThrow` stubbing after the basic spy works.
Those APIs avoid calling a real method during setup and are important when the
method is expensive or unsafe:

```java
doReturn("EUR").when(pricingSpy).currency();
```

They require a small pending-stub state in `MockRuntime`, but no generator
change.

## Object methods

Treat `equals`, `hashCode`, and `toString` deliberately rather than letting
them fall out of method discovery:

- generated mocks and spies should use identity equality and identity hash
  codes for runtime state lookup;
- `toString` should provide a stable diagnostic such as
  `Mockatcha mock of PricingService`; and
- these methods should not be recorded or stubbed in the first class release.

This protects the `IdentityHashMap` used by `MockRuntime` and prevents logging
or assertion messages from unexpectedly calling the real object.

## Delivery sequence

### 1. Prove subclass generation

Add one internal fixture with an accessible no-argument constructor and a
single public object-returning method. Generate a subclass in a TeaVM test,
instantiate it, and prove the override runs. Do not change the public API until
this browser test passes.

### 2. Add class mocks

Extend `mock(Class<T>)` to route interfaces through the current proxy and
supported classes through `SubclassGenerator`. Cover object, primitive, void,
overloaded, inherited, exception-throwing, final, and constructor cases.

Acceptance criteria:

- existing interface tests remain unchanged and green;
- class mocks support the existing stubbing, matching, verification, and
  reset APIs;
- unsupported classes fail at compilation with an actionable message; and
- generated classes add no browser DOM, CDI, Sarto, or Verrai dependency.

### 3. Add delegation spies

Add `spy(Class<T>, T)` using the same subclass and the dispatch distinction
described above. Test real fallback, stub precedence, thrown real exceptions,
verification without a second real call, reset, and identity behaviour.

### 4. Add safe spy stubbing

Implement `doReturn` and `doThrow`, then document when they are preferable to
`when`. Only after this is stable should the project investigate `spy(T)` or
constructor-free class mocks.

## Browser test matrix

Every supported behaviour must run through `TeaVMTestRunner`, not solely on the
JVM. Include fixtures for:

| Area | Cases |
| --- | --- |
| Class shape | direct class, inherited method, abstract class, final rejection, missing constructor rejection |
| Results | object, every primitive, `void`, explicit `null`, thrown exception |
| Dispatch | exact stub, matcher stub, consecutive answers, unstubbed default, verification |
| Spy | real fallback, stubbed override, real failure, reset, clear history, self-call limitation |
| Methods | overloads, generic bridge method, protected method, final method excluded, Object methods |
| Isolation | two instances of one generated class retain independent state |

Compile-failure cases may need a small Maven Invoker or compiler-harness module
because a deliberately unsupported metaprogram cannot live in the normal green
test source set.

## Explicitly deferred work

The following features need different machinery or a separate design:

- static and constructor mocking;
- final classes and final or private methods;
- constructor-free allocation;
- field copying and self-intercepting spies;
- native JavaScript classes;
- asynchronous or timeout-based verification; and
- inline modification of already-created objects.

Deferring these is not an API dead end. The generated-subclass seam and shared
runtime support ordinary class mocking and useful boundary spies while keeping
Mockatcha small and portable.

## First coding task for the next implementer

Create `SubclassGenerator` and a browser-only proof test for a package-local
fixture with a public no-argument constructor and one method. The test should
assert only that the generated override returns a known value. Record which of
the two TeaVM generation seams was used and why in this document before
exposing class support through `Mockatcha.mock`.

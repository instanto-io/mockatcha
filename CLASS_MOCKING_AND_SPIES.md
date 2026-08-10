# Class mocking and spies: design record

Mockatcha generates mocks while TeaVM compiles a test. The first release handled
Java interfaces. It now also handles ordinary classes and delegation spies.

This document records what was built, the TeaVM constraints that shaped it, the
supported boundary, and what remains deferred. It replaces the implementation
handoff that preceded the work; the delivery sequence it described has been
completed.

## What a test can now do

```java
PricingService pricing = mock(PricingService.class);
when(pricing.total("A-17")).thenReturn(42);

PricingService realPricing = new PricingService(exchangeRates);
PricingService pricingSpy = spy(PricingService.class, realPricing);
when(pricingSpy.currency()).thenReturn("EUR");

doReturn("EUR").when(pricingSpy).currency();
```

The two-argument spy form is deliberate. TeaVM must know the concrete class
while it compiles the call, and a generic `spy(T value)` does not reliably
expose that class as a constant to the metaprogram.

`spy(Class<T>, T)` also accepts an interface and an implementation of it, which
costs nothing extra and gives a browser test the equivalent of Mockito's
`delegatesTo`.

## Which TeaVM seam was used, and why

**A generated subclass, submitted through `createClass`, instantiated through
`Metaprogramming.caller(constructor).construct(...)`.**

The route was chosen after ruling out two cheaper ones.

### TeaVM's `proxy` can extend a class, but will not override anything

The premise of the original handoff — that `proxy` cannot extend a class — is
not true of TeaVM 0.15. `MetaprogrammingImpl.proxy` sets the generated class's
parent to the requested type whenever that type is not an interface. What it
does not do is supply bodies for methods it finds concrete:

```java
if (methodImpl.method.getProgram() != null && ...
        || !methodImpl.method.hasModifier(ElementModifier.ABSTRACT)) {
    continue;
}
```

So proxying a concrete class yields an object that is an instance of that class,
inherits every real method, and intercepts nothing.

### The abstract shim looked ideal and does not work

That filter suggests an elegant repair: generate an abstract subclass which
redeclares each overridable method as `abstract` and contains no bodies at all,
then hand *that* to `proxy`. TeaVM would then treat the class exactly as it
treats an interface, and all of the argument packing, boxing, and value capture
would be reused unchanged.

This fails on TeaVM 0.15. `proxy` resolves its argument through
`agent.getUnprocessedClassSource()`, while a class submitted through
`createClass` reaches only the processed source. The shim is therefore invisible
to the one operation that would consume it, and `proxy` fails with a null class
reader.

The approach is worth revisiting if a later TeaVM release resolves proxy targets
through the processed class source. It would delete most of
`SubclassGenerator`.

### So the subclass is generated whole

`SubclassGenerator` emits the complete subclass with TeaVM's relocated ASM and
submits it through `MetaprogrammingEnvironment.createClass`. The metaprogram
then instantiates it with `caller(constructor).construct(state, delegate)`,
which needs no class-source lookup.

Generated bodies are deliberately thin. Each one packs its arguments, asks
`MockRuntime` what to do, and either calls the real object or converts the
runtime's answer. No matching, stubbing, or verification logic exists in
bytecode.

Bytecode is emitted at class file version 49 so that generated methods need no
stack map frames, which would otherwise have to be computed for every branch by
loading the mocked class's whole hierarchy inside the compiler.

## Generated subclass shape

For a class named `PricingService`, generation produces the equivalent of:

```java
final class PricingService$Mockatcha extends PricingService {
    private final MockState mockatchaState;
    private final PricingService mockatchaDelegate;

    PricingService$Mockatcha(MockState state, PricingService delegate) {
        super();
        this.mockatchaState = state;
        this.mockatchaDelegate = delegate;
    }

    @Override
    public int total(String account) {
        Object result = MockRuntime.dispatchSpy(mockatchaState,
                "total(java.lang.String)", new Object[] { account });
        if (MockRuntime.callsReal(result, mockatchaDelegate)) {
            return mockatchaDelegate.total(account);
        }
        return MockRuntime.toInt(result);
    }

    @Override public boolean equals(Object other) { ... }
    @Override public int hashCode() { ... }
    @Override public String toString() { ... }
}
```

One generated class serves both mocks and spies: a mock is an instance whose
delegate is null, which `MockRuntime.callsReal` reports as "answer from the
runtime". The class is generated once per mocked type per TeaVM compilation and
named after that type, so an incremental build reuses the same class graph.
Instances still receive their own `MockState`.

The generator walks inherited overridable methods and de-duplicates by JVM
signature. Bridge methods are left in place rather than emitted independently:
the real bridge calls the overridden method virtually, so a call through the
erased signature reaches the override and one stub still covers one
source-level method.

## Runtime: no stub is distinct from a stubbed null

`MockRuntime.invokeObject` previously returned `null` both for an unstubbed call
and for a stub configured with `thenReturn(null)`. A spy has to tell those
apart.

Dispatch now returns the sentinel `MockRuntime.REAL_CALL` when nothing is
stubbed, and the generated method decides:

1. consume argument matchers, or apply arranged answers, or verify;
2. record the invocation when this is an ordinary call;
3. return or throw the matching stub when one exists;
4. call the real delegate for a spy;
5. otherwise return the Java default.

Verification never calls the delegate. Stubbing through `when(spy.method())` does
call the real method once while the expression is evaluated, matching Mockito's
familiar behaviour, and the runtime removes that setup invocation from history.
`doReturn`, `doThrow`, `doAnswer`, and `doNothing` arrange the answer before the
call is written, so the real method never runs.

## Supported class boundary

A type is accepted when all of these hold:

- it is a non-final, non-interface class;
- it is not an enum, record, annotation, array, or anonymous class;
- it has a non-private no-argument constructor.

Within an accepted class, public and protected instance methods are
intercepted. Static, private, and final methods keep their real behaviour
because they cannot be overridden, and Mockatcha does not pretend otherwise.
Package-private methods are left alone until generated-package placement has an
explicit test.

An unsupported type is rejected while TeaVM compiles the test, with a message
naming the type and the failed rule:

```
Mockatcha cannot mock this type: com.example.Rates is final and cannot be subclassed
```

Running the no-argument constructor is a visible limitation. Mockito can often
allocate objects without running a constructor by using JVM-specific
facilities; Mockatcha cannot assume those in TeaVM.

## Object methods

`equals`, `hashCode`, and `toString` are answered by the generated subclass
rather than left to method discovery:

- `equals` and `hashCode` are identity-based, which protects the
  `IdentityHashMap` the runtime keys state on and stops two mocks of one class
  comparing equal through inherited field comparison;
- `toString` returns `Mockatcha mock of PricingService` or `Mockatcha spy of
  PricingService`, so a logged mock never runs real code; and
- none of the three is recorded or stubbed.

A class that makes one of them final keeps its own. Interface mocks are
unchanged and still inherit `Object`'s behaviour, because TeaVM's proxy
generator does not override non-abstract methods.

## Spy behaviour

A spy is a generated subclass that delegates unstubbed overridable methods to
the supplied real object. It is not the same object, and `==` observes that.

- the delegate must be non-null, which is checked when the spy is created;
- unstubbed overridable methods call the delegate exactly once;
- stubbed methods do not call the delegate after setup;
- verification reads recorded calls without invoking the delegate;
- `reset` removes stubs and call history but retains delegation, because
  delegation is compiled in rather than held in runtime state;
- `clearInvocations` retains both stubs and delegation; and
- a real method calling another method on itself is not intercepted, because
  `this` inside that method is the delegate.

That last rule is the cost of delegation, and it is tested rather than hidden. A
self-intercepting spy would have to run inherited method bodies on the generated
subclass and reproduce the original object's fields. Field copying is
error-prone and constructor bypassing is unavailable, so it is out of scope.

## Test coverage

Every supported behaviour runs through `TeaVMTestRunner` in Chrome.

| Area | Where |
| --- | --- |
| Subclass generation on its own | `SubclassGenerationTeaVmTest` |
| Class shape, results, dispatch, methods, isolation | `MockatchaClassTeaVmTest` |
| Spies, arranged answers, delegation limits | `MockatchaSpyTeaVmTest` |
| Interface mocking, unchanged | `MockatchaTeaVmTest` |
| Consumer-facing examples | `mockatcha-examples` |

The rules that reject an unsupported class are covered by
`SubclassGeneratorRulesTest`, which runs on the JVM against a stand-in for
TeaVM's introspection. A deliberately unsupported metaprogram cannot live in a
green browser test source set, so the compile failure itself is checked by hand;
a Maven Invoker module would be needed to automate it.

## Explicitly deferred work

- static and constructor mocking;
- final classes and final or private methods;
- package-private methods;
- constructor-free allocation;
- field copying and self-intercepting spies;
- a single-argument `spy(T)`;
- argument captors;
- native JavaScript classes; and
- asynchronous or timeout-based verification.

Deferring these is not an API dead end. The generated-subclass seam and the
shared runtime already support ordinary class mocking and useful boundary spies
while keeping Mockatcha small and portable.

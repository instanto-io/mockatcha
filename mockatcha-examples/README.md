# Mockatcha examples

This module is a small timesheet application whose tests run in a browser
through TeaVM. It demonstrates Mockatcha as a consumer would use it; the module
uses only Mockatcha's public API.

Start with
[`TimesheetServiceTest`](src/test/java/io/instanto/mockatcha/examples/TimesheetServiceTest.java).
Its tests introduce the ideas in this order:

1. stub a boundary and assert the result produced by the real service;
2. verify an important side effect without coupling the test to every call;
3. use `never()` to describe behaviour that must not happen;
4. use matchers where an exact value is not important;
5. calculate a stubbed result from the invocation arguments;
6. return different values from consecutive calls; and
7. make a collaborator fail so the service's failure behaviour is explicit.

Then read
[`TimesheetSpyTest`](src/test/java/io/instanto/mockatcha/examples/TimesheetSpyTest.java),
which uses a working `InMemoryTimesheetRepository` instead of stubbing storage
from scratch. Its tests show:

1. a spy that keeps real behaviour and still records calls;
2. replacing only the one call a test needs to control;
3. `doThrow`, which arranges a failure without running the real method first;
   and
4. mocking that same concrete class when no real behaviour is wanted.

Next read
[`TimesheetBddScenarioTest`](src/test/java/io/instanto/mockatcha/examples/TimesheetBddScenarioTest.java),
which expresses business domain rules as executable Given-When-Then scenarios using `mockatcha-bdd`:

1. structure tests into readable `scenario("...")` steps (`given`, `when`, `then`, `and`);
2. pass results directly from `when(Supplier<R>)` into `then(Consumer<R>)` with compile-time type safety;
3. use fluent `expect(...)` assertions for values, mocks, spies, and exceptions; and
4. get detailed step-level diagnostics in JVM and browser test logs if a step fails.

Finally, [`SparklineTest`](src/test/java/io/instanto/mockatcha/examples/SparklineTest.java)
is the case for testing in a browser at all. `Sparkline` draws to a canvas, so
there is no JVM test to write; the readings it draws are mocked, the canvas is
real, and the assertions read pixels back from it.

Run the examples from the repository root with:

```bash
mvn -pl mockatcha-examples -am test
```

These examples compile and run in Chrome. Changes to Mockatcha's public API must
keep them passing.

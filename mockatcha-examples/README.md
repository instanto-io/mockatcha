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

Run the examples from the repository root with:

```bash
mvn -pl mockatcha-examples -am test
```

These are executable examples rather than copied snippets, so changes to
Mockatcha's public API must continue to compile and pass in Chrome.

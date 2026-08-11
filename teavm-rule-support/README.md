# JUnit rules for TeaVM's test runner

`TeaVMTestRunner` runs `@Before` and `@After` methods but not `@Rule` fields, so
a rule silently does nothing in a browser test. This module is a working fix,
running in Chrome, and [`teavm-junit-rules.patch`](teavm-junit-rules.patch) is
the same change against `teavm` at tag 0.15.0.

[`PULL_REQUEST.md`](PULL_REQUEST.md) is the accompanying description, ready to
review before anything is sent.

Nothing here is part of Mockatcha. The classes under `org/teavm/junit` and
`org/teavm/classlib` are copies that shadow the ones in the TeaVM jars, because
test classes come before dependencies on the classpath. That makes the patch
testable without building TeaVM.

## What the change does

`TestEntryPointTransformer` already generates the bodies of
`TestEntryPoint.before()` and `after()`, emitting a direct call to each
annotated method it finds in the test class hierarchy. It now also generates a
body for a new `applyRules` method:

```java
static Statement applyRules(Statement base, String name) {
    Statement statement = base;
    Description description = describe("com.example.MyTest", name);
    statement = ((TestRule) testCase.firstRule).apply(statement, description);
    statement = ((TestRule) testCase.secondRule).apply(statement, description);
    return statement;
}
```

The fields are read directly, so nothing needs reflection at runtime. When the
test class declares no rules the generated body returns its argument, which
costs a compiled program and nothing else.

`TestEntryPoint.run` then wraps each launcher in a `Statement` and hands it to
the rules:

```java
for (Launcher launcher : launchers) {
    applyRules(new LaunchStatement(launcher), name).evaluate();
}
```

`LaunchStatement.evaluate` runs `before()`, the test, and `after()`, so rules
sit outside the `@Before` and `@After` methods, as they do in JUnit.

## The classlib addition

`org.junit.runner.Description` holds its children in a
`ConcurrentLinkedQueue`, which TeaVM had no emulation for, so the real JDK class
was compiled and its static initialiser reached `MethodHandles.lookup()`:

```
Method java.lang.invoke.MethodHandles.lookup()Ljava/lang/invoke/MethodHandles$Lookup; was not found
    at java.util.concurrent.ConcurrentLinkedQueue.<clinit>
    at org.junit.runner.Description.<init>
```

`TConcurrentLinkedQueue` fixes that. It is a queue backed by an `ArrayDeque`,
which is all a single-threaded runtime needs, and it is useful beyond this
change: any code constructing a `Description`, or using the queue directly, hits
the same wall today.

## Ordering

Rules are applied in declaration order, each wrapping the previous, so the last
field declared is the outermost. That matches what JUnit's own `RunRules` does
with the list it is given. Superclass rules are applied before subclass rules.

JUnit 4.13's `@Rule(order = ...)` is not read yet; supporting it means sorting
the collected fields before emitting the chain.

## What is left for a MethodRule

A `MethodRule` receives a `FrameworkMethod`, which wraps
`java.lang.reflect.Method`, and JUnit rejects a null one in its constructor.
TeaVM can produce a `Method` only for a method annotated `@Reflectable`.

That makes `MethodRule` reachable rather than impossible: the transformer
already visits the test class and could add `@Reflectable` to its test methods,
then emit `new FrameworkMethod(testClass.getDeclaredMethod(name))`. It is more
machinery than `TestRule` needed, for the older of the two interfaces, so this
change reports a clear error instead:

```
Field com.example.MyTest.rule of type com.example.SomeMethodRule is a MethodRule,
which TeaVM cannot run because it needs java.lang.reflect.Method. Use a TestRule.
```

## Using it in another module

Add the dependency, above `teavm-junit` so that its copies of the runner classes
come first on the classpath:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>teavm-rule-support</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

That is the whole setup. There is nothing to register and nothing to call: the
work happens while TeaVM compiles the test, and `@Rule` fields start being
honoured.

`mockatcha-examples` does exactly this, and `TimesheetRuleTest` there proves the
rule is running rather than quietly doing nothing.

## Running it

```bash
mvn -pl teavm-rule-support test
```

`RuleSupportTeaVmTest` declares two rules and checks that both wrap the test,
that they nest and unwind in the right order, and that the `Description` a rule
receives names the test class and method.

# Getting started

## Choose the test runtime

Keep portable domain logic, algorithms, and state machines in JVM tests with
Mockito. Those tests have all of Mockito available and usually finish much
faster than browser tests.

Run a test through TeaVM and Mockatcha when:

- the code uses timers, the DOM, canvas, storage, or another browser API;
- a collaborator depends on WebSocket, IndexedDB, or another browser-only API;
  or
- the assertion concerns JavaScript produced by TeaVM, such as emulated `long`
  arithmetic, collection iteration order, or browser date and number formats.

## Compile-time mock generation

Mockito creates mocks at runtime through reflection and JVM class generation.
TeaVM compiles the complete program ahead of time, so Mockatcha generates each
mock while TeaVM compiles the test.

This leads to three rules:

- Pass a class literal, such as `mock(ProfileRepository.class)`, so Mockatcha
  can identify the type during compilation.
- Invalid requests, including a final class, fail during compilation.
- A test that creates Mockatcha mocks must run through TeaVM.

## Add the dependencies

Add Mockatcha Core to the test classpath:

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>mockatcha-core</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

The test classpath also needs `teavm-classlib`, `teavm-junit`, and `junit`.
Configure Surefire to send TeaVM tests to a browser:

```xml
<systemPropertyVariables>
  <teavm.junit.target>${project.build.directory}/js-tests</teavm.junit.target>
  <teavm.junit.js.runner>browser-chrome</teavm.junit.js.runner>
</systemPropertyVariables>
```

### Choose and install the browser runner

These elements define JVM system properties read by `TeaVMTestRunner`:

- `teavm.junit.target` is the directory where TeaVM writes the generated
  JavaScript test files and runner pages.
- `teavm.junit.js.runner` selects how TeaVM executes those files.

The value `browser-chrome` tells TeaVM to start an external process using the
command `chrome`. It is a runner selector, not a Maven dependency or a bundled
browser. Install Chrome or Chromium and ensure that `chrome` resolves to its
executable on the `PATH` inherited by Maven. If the Chromium executable has a
different name, provide a `chrome` executable or wrapper on that `PATH`.

The browser must be installed on developer machines and CI agents. This
repository maps its `mockatcha.test.browser` property to
`teavm.junit.js.runner` and defaults it to `browser-chrome`. Run its tests with
TeaVM's Firefox runner like this:

```bash
mvn test -Dmockatcha.test.browser=browser-firefox
```

That setting expects the external command `firefox` to be available instead.

Downstream projects can use the artifacts published to GitHub Packages. Public
package visibility allows CI and team builds to resolve them without access to
this repository.

When working from a clone, install the artifacts locally first:

```bash
./mvnw install
```

## Write the first test

Suppose a component gets its text from a service:

```java
public interface GreetingService {
    String hello(String name);
}
```

The test replaces the service, arranges its result, and runs the real component:

```java
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class GreetingComponentTest {

    @Test
    public void showsTheGreetingFromTheService() {
        GreetingService greetings = mock(GreetingService.class);
        when(greetings.hello("Ada")).thenReturn("Hello Ada");

        String shown = new GreetingComponent(greetings).greet("Ada");

        assertEquals("Hello Ada", shown);
        verify(greetings).hello("Ada");
    }
}
```

`mock(...)` creates a stand-in. Unstubbed methods return Java's empty value:
`null`, `0`, or `false`. `when(...)` arranges a result for one call.
`verify(...)` checks that a call occurred.

`@RunWith(TeaVMTestRunner.class)` compiles and runs the test in a browser.
`@SkipJVM` prevents JUnit from also running it on the JVM, where the generated
mock is unavailable.

Continue with [stubbing](stubbing.md).

[Documentation index](README.md) · [Project README](../README.md)

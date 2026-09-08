# Mockatcha Webapp Testkit

The Webapp Testkit extends Mockatcha DOM and TeaVM's test runner to arbitrary
web applications. The application under test can use any web stack; it does not
need to use Java or TeaVM. The test remains a Java test compiled by TeaVM, with
Mockatcha DOM providing its queries, interactions, waiting, and assertions.

The testkit stages the built application on `TeaVMTestRunner`'s classpath
resource server. `FramedApplication` opens it in a same-origin frame so the Java
test can inspect and operate its DOM.

These are real browser tests. TeaVM does not bundle Chromium. The repository's
default `browser-chrome` runner requires Chrome or Chromium to be installed and
available to the test process as `chrome`, on developer machines and in CI.

`ApplicationFiles.stage(source, destination)` copies the complete application tree
to a test-classpath directory. It adds a small error observer at the start of each
HTML head, before application scripts run. This reports startup exceptions, failed
resources and unhandled promise rejections. It does not select, replace or load
application assets. The original build output is unchanged.

## Build setup

Add `io.instanto:mockatcha-webapp-testkit:0.1.0-SNAPSHOT` as a test dependency.
Run its Java entry point after building the subject application, before browser
tests. For example, configure `exec-maven-plugin`:

```xml
<execution>
  <id>stage-application</id>
  <phase>process-test-resources</phase>
  <goals><goal>java</goal></goals>
  <configuration>
    <mainClass>io.instanto.mockatcha.browser.ApplicationFiles</mainClass>
    <classpathScope>test</classpathScope>
    <arguments>
      <argument>${application.build.directory}</argument>
      <argument>${project.build.testOutputDirectory}/applications/demo</argument>
    </arguments>
  </configuration>
</execution>
```

`application.build.directory` is the complete built site directory. The test URL is
`/resources/applications/demo/index.html`. Relative scripts, styles, images and GWT
permutations retain their paths. An application using origin-root URLs must be
configured for this deployment prefix, just as for any other subdirectory deployment.

The stager refreshes only directories bearing its own marker. It rejects symlinks,
copies into the source tree, existing unrelated destinations and HTML without an
explicit head. HTML must be UTF-8. The test document's CSP must allow the diagnostic
observer; the stager does not weaken CSP. If the observer cannot run, readiness
reports that it is missing so startup errors do not disappear.

## Use the staged application

The testkit handles the JVM build step. In the browser test,
`FramedApplication` from `mockatcha-dom` opens the staged application and gives
DOM queries access to its document:

```java
@Rule
public DomRule dom = new DomRule();

@Test
public void enablesNotifications() {
    try (FramedApplication app = FramedApplication.open(
            "/resources/applications/demo/index.html#buttons")) {
        app.awaitReady(page -> "true".equals(page.root().getAttribute("data-ready")));
        Dom.click(app.page().findByRole("button", "Notifications"));
        expect(app.page().findByRole("button", "Notifications"))
                .toHaveAttribute("aria-pressed", "true");
    }
}
```

See [Mockatcha DOM](../mockatcha-dom/README.md#parent-pages-and-frames) for frame
readiness, diagnostics, cleanup, and cross-window DOM support.

## Sample application

The repository includes a
[plain HTML, CSS, and JavaScript application](../mockatcha-webapp-example/src/main/resources/webapp)
with no Java, TeaVM, Maven, or npm dependency:

```text
mockatcha-webapp-example/
└── src/main/resources/webapp/
    ├── index.html
    ├── app.css
    └── app.js

mockatcha-dom/
└── src/test/java/io/instanto/mockatcha/dom/
    └── FramedApplicationIntegrationTest.java
```

The application supplies a text field, select control, toggle button,
asynchronous readiness signal, and responsive style. The
[`FramedApplicationIntegrationTest`](../mockatcha-dom/src/test/java/io/instanto/mockatcha/dom/FramedApplicationIntegrationTest.java)
types, selects, clicks, queries accessible elements, inspects styles, and checks
state from Java. These are integration tests owned by `mockatcha-dom`; they test
its behaviour against the separately built sample application.

During `mockatcha-dom`'s `process-test-resources` phase, `ApplicationFiles`
copies the example module's application tree to:

```text
${project.build.testOutputDirectory}/applications/plain-js
```

`TeaVMTestRunner` serves that directory at
`/resources/applications/plain-js/index.html`. The Java test opens the URL with
`FramedApplication`, waits for the application's `data-ready` attribute, and
then uses Mockatcha DOM inside the frame.

The sample test uses these dependencies:

| Dependency | Role |
| --- | --- |
| `mockatcha-dom` | Frames, queries, interactions, waiting, and assertions. |
| `mockatcha-webapp-testkit` | Copies and instruments the application during the Maven build. |
| `teavm-rule-support` | Runs `DomRule` for cleanup after each browser test. |
| `teavm-classlib` | Supplies TeaVM's Java class-library implementation. |
| `teavm-junit` | Compiles and runs the JUnit test through TeaVM. |
| `junit` | Supplies `@Test`, `@Rule`, and assertions. |

The sample does not use `mockatcha-core`. Add it only when the Java test also
needs mocks or spies.

## Direct component tests vs staged application tests

Direct component tests are TeaVM-focused. TeaVM compiles the Java test and the
component under test into the same JavaScript output, so the test can construct
the component through its Java API and attach it to Mockatcha's container. The
exact construction API belongs to the component; for example:

```java
TimesheetComponent timesheet = new TimesheetComponent(timesheetService);
Dom.container().appendChild(timesheet.element());

expect(Dom.findByRole("heading", "July timesheet")).toBeVisible();
```

This suits a Java UI component whose source and dependencies TeaVM can compile.
For example, it can cover portable Java code from a GWT project when that code
has no GWT-specific dependencies. A Java wrapper around a JavaScript component
can also use this approach when it exposes the component to the TeaVM test. The
component renders in TeaVM's test document, where the test controls its
dependencies and lifecycle and can use Mockatcha mocks alongside Mockatcha DOM.

A staged application test treats the application as completed web output. The
application is built separately, whether by GWT, TeaVM, JavaScript tooling, or
another web stack. The test cannot call its source-level Java components. It
opens the built application through its normal entry point and exercises the
resulting DOM in a same-origin frame.

Staging loads the application's scripts, styles, images, routes, and other
assets in their deployed layout. It therefore covers application startup,
asset paths, integration between components, and behaviour that depends on the
real page or viewport. Both approaches use the same Mockatcha DOM queries,
interactions, waiting, and assertions, and both require the browser used by
TeaVM's test runner to be installed.

# Mockatcha DOM

Mockatcha DOM tests browser UI through the elements a user can find and the
actions a user can take. It provides an isolated test container, accessible
queries, browser interactions, waiting, and readable assertions.

The application still renders its own UI. Mockatcha DOM supplies the test
boundary around it. It can be used on its own, with Mockatcha Core for the
parts of a test that can be mocked, or with the BDD clock when the UI depends
on time.

## Add the module

```xml
<dependency>
  <groupId>io.instanto</groupId>
  <artifactId>mockatcha-dom</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <scope>test</scope>
</dependency>
```

Mockatcha DOM is part of the Mockatcha toolkit but does not depend on
`mockatcha-core`. Add [Mockatcha Core](../README.md) when a test needs mocks, or
the [BDD helpers](../mockatcha-bdd/README.md) when it needs the browser clock.

DOM tests run in a real browser through TeaVM's test runner. The repository
defaults to `browser-chrome`, which requires Chrome or Chromium to be installed
and available to the test process. See
[Choose and install the browser runner](../docs/getting-started.md#choose-and-install-the-browser-runner)
for what the TeaVM runner properties mean, how the browser command is resolved,
and how to select Firefox.

## Set up the test boundary

`Dom.container()` creates an element for the test and attaches it to the
document. In a direct component test, TeaVM compiles the Java test and component
together. Construct the component through its own Java API and attach its
element to the container. For example:

```java
TimesheetComponent timesheet = new TimesheetComponent(timesheetService);
Dom.container().appendChild(timesheet.element());

expect(findByRole("heading", "July timesheet")).toBeVisible();
```

`TimesheetComponent` and `element()` represent the application's API; Mockatcha
does not prescribe a component interface. This direct approach requires the
component source and its dependencies to be compilable by TeaVM. Use the
[webapp testkit](../mockatcha-webapp-testkit/README.md#direct-component-tests-vs-staged-application-tests)
when the application is built separately.

Every query stays inside this container. Add `DomRule` so it is removed after
each test, including a failed test:

```java
@Rule
public DomRule dom = new DomRule();
```

TeaVM needs JUnit rule support to execute the rule. The reusable
[`teavm-rule-support` module](../teavm-rule-support/README.md) provides it. If a
runner cannot execute rules, extend `DomTest` for equivalent `@After` cleanup.
`Dom.reset()` is also available for manual cleanup.

For a test that only needs a small HTML fixture, `render` replaces the
container contents directly:

```java
render("<button>Save</button>");
expect(findByRole("button", "Save")).toBeEnabled();
```

## Find elements as a user would

Prefer queries based on the information presented to the user:

```java
HTMLElement submit = findByRole("button", "Submit timesheet");
HTMLElement account = findByLabelText("Account");
HTMLElement search = findByPlaceholderText("Search activities");
HTMLElement code = findByDisplayValue("OPS-142");
HTMLElement map = findByAltText("Airport map");
HTMLElement help = findByTitle("About activity codes");
```

`findByRole` recognises common implicit HTML roles as well as explicit ARIA
roles. Its second argument is the accessible name. `findByLabelText` follows
HTML labels, `aria-label`, and `aria-labelledby`.

`findByTestId` is available when an element has no useful accessible identity.
CSS queries remain useful for implementation-level checks:

```java
HTMLElement row = find("tr[data-entry-id='17']");
```

### Choose the expected number of results

A singular query requires exactly one match. It fails when there are none or
when the result is ambiguous:

```java
HTMLElement save = findByRole("button", "Save");
```

Use the optional form when absence is valid, or the plural form when several
matches are expected:

```java
HTMLElement warning = findByRoleOrNull("alert");
List<HTMLElement> rows = findAllByRole("row");
```

The same `findBy...`, `findBy...OrNull`, and `findAllBy...` forms are available
for text, role, label, placeholder, display value, alt text, title, and test ID
queries. An ambiguity failure lists every match and the current query scope.

### Restrict a query to part of the page

Use `within` when repeated controls belong to different rows, dialogs, or
sections:

```java
HTMLElement submittedRow = find("tr[data-entry-id='17']");
DomScope row = within(submittedRow);

expect(row.findByRole("cell", "Submitted")).toBeVisible();
click(row.findByRole("button", "Open"));
```

Queries made through the scope only inspect descendants of its root element.
Scopes can be narrowed again with `row.within(element)`.

### Match variable text

Text is normalised by trimming its ends and collapsing whitespace. A string
argument requires the complete normalised value. Use `TextMatch` for partial
or case-insensitive text:

```java
findByText(TextMatch.containing("3 activities"));
findByRole(role("button").named(TextMatch.exactIgnoringCase("save")));
```

Available matchers are `exact`, `containing`, `exactIgnoringCase`, and
`containingIgnoringCase`.

### Filter by accessible state

`RoleQuery` can distinguish elements that share a role and name:

```java
findByRole(role("heading").level(2));
findByRole(role("checkbox").checked(true));
findByRole(role("option").selected(true));
findByRole(role("button").expanded(false));
findByRole(role("button").pressed(true));
```

## Interact with the page

```java
click(findByRole("button", "Add activity"));
type(findByLabelText("Description"), "Stand review");
clear(findByLabelText("Description"));
check(findByLabelText("Billable"));
uncheck(findByLabelText("Billable"));
select(findByLabelText("Status"), "submitted");
selectOptions(findByLabelText("Teams"), "ops", "airports");
hover(findByTitle("Approval details"));
tab();
tabBack();
```

`type` replaces the current value one character at a time. It uses the native
value setter and dispatches keyboard, `beforeinput`, `input`, and `change`
events so controlled inputs can observe the edit. `selectOptions` supports
multiple selections. Interaction helpers return the element they acted on.

`press(element, "ArrowRight")` sends keydown and keyup with both modern
`key`/`code` and legacy `keyCode`/`which` fields. `type` uses the same event
factory; its keypress event carries the character code, while keydown and keyup
carry the physical-key code. Common navigation, editing and function keys are
supported. Printable ASCII uses US keyboard positions; unknown physical keys
keep an empty `code` and zero legacy code. These helpers do not infer modifier
keys, keyboard layouts or native default actions such as Tab traversal (use
`tab()` for that).

Some UI frameworks batch state updates. Put the interaction inside that
framework's normal test flush boundary when required.

## Wait for rendering

Use a synchronous `findBy...` query when the action renders immediately. Use
`awaitBy...` when an element appears after a promise, browser callback, network
response, or framework continuation:

```java
click(findByRole("button", "Load entries"));

HTMLElement table = awaitByRole("table", "July entries");
expect(table).toBeVisible();
```

An awaited query retries for up to one second. It applies the same cardinality
rules as a synchronous query, so it does not accept an ambiguous result. A
timeout reports the last query failure and the current scope contents.

Use `waitFor` when the element already exists but one of its properties changes:

```java
waitFor(() -> expect(findByRole("status")).toHaveText("Saved"));
```

The overload `waitFor(assertion, timeoutMilliseconds)` sets a different
timeout. The supplier overload can return a value.

### Await rendering while the fake clock is installed

The BDD clock controls application timers. Mockatcha DOM uses the saved real browser
timer only for its internal retry loop, so an awaited query can resume while
the fake clock remains installed. This retry loop is not exposed as a second
test clock and does not advance fake time.

For a UI that displays a result after a controlled delay, advance the delay and
then await the rendering work that follows it:

```java
clock().install();
try {
    click(findByRole("button", "Save"));

    clock().tick(500);                 // runs the application's delayed callback
    expect(awaitByRole("status"))      // allows its continuation to render
            .toHaveText("Saved");
} finally {
    clock().uninstall();
}
```

`tick` runs application callbacks that are due. `awaitByRole` then yields while
promise or framework work updates the DOM. The fake clock can remain installed
between those operations.

## Check the result

```java
expect(findByRole("heading"))
        .toHaveAccessibleName("July timesheet")
        .toHaveRole("heading");
expect(findByLabelText("Hours")).toBeRequired().toBeValid();
expect(findByRole(role("button").expanded(true))).toBeExpanded();
expect(findByRole(role("option").selected(true))).toBeSelected();
expect(find("form")).toHaveFormValues(Map.of(
        "account", "A-17",
        "hours", "7.5"));
```

Other checks cover text, classes, attributes, values, styles, visibility,
enabled and checked state, focus, document attachment, empty content, child
elements, and HTML fragments. `not()` negates the next check only.

A failed check includes the element that was inspected:

```text
Wanted text "Saved" but found "Saving".
The element is:
<p role="status">Saving</p>
```

The browser tests in [`src/test`](src/test/java/io/instanto/mockatcha/dom)
contain complete examples for scopes, interactions, waiting, fake time, and
promise-driven rendering.

## Parent pages and frames

`Dom.within(element)` starts a scope at any accessible element. The element may
belong to the test page, another container or a same-origin child frame. Queries,
labels, focus, computed styles and generated input events use its owning document.
Calling `scope.within(element)` still requires the element to be inside that scope.

```java
DomScope child = Dom.within(frame.getContentDocument().getBody());
Dom.click(child.findByRole("button", "Save"));
expect(child.findByRole("status")).toHaveText("Saved");
```

For a separately built application, `FramedApplication` handles the frame lifecycle:

```java
try (FramedApplication app = FramedApplication.open(
        "/resources/applications/demo/index.html#buttons", 375, 700)) {
    app.awaitReady(page -> "true".equals(page.root().getAttribute("data-ready")));
    DomScope page = app.page();
    Dom.click(page.findByRole("button", "Notifications"));
    expect(page.findByRole("button", "Notifications")).toHaveAttribute("aria-pressed", "true");
}
```

Use the [webapp testkit](../mockatcha-webapp-testkit/README.md) for a separately
built application. It extends Mockatcha DOM and TeaVM's test runner to
applications built with any web stack; the application itself does not need to
use TeaVM. The testkit retains the asset tree and observes errors before startup.
The application loads its own styles and scripts and runs its normal entry point.
Readiness is an application condition, independent of the iframe load event.
`page()`, `assertHealthy()` and `close()` report captured child errors; close also
removes the frame if an error occurred. Close each application after a test.

Frames remain rendered for focus, visibility and viewport-dependent CSS checks.
Cross-origin URLs and navigation are rejected. Existing direct widget tests can
continue to use `Dom.container()` with their framework's normal lifecycle.

### Synthetic interactions

`Dom.click()` and the typing helpers dispatch browser events from JavaScript.
They exercise application event handlers and resulting state changes, but the
browser does not classify these events as trusted user input. Use browser
automation when a test depends on native touch handling or browser gestures.

### Implementation note: DOM casts across frames

Tests do not need to track the window hierarchy before casting a DOM element.
Once a query returns an element, Mockatcha uses that element's owning document
to find the correct window automatically. This also works with multiple sibling
or nested same-origin frames because every element points directly to its own
document and window. The test must still use the appropriate `DomScope` to
query the intended frame.

Java identifies a class by its type. JavaScript uses constructor objects and
prototype chains for the equivalent `instanceof` check. A constructor is not
identified by its name alone: it is an object belonging to a particular browser
window.

The TeaVM test page and every iframe have separate `Window` objects. Each window
creates its own DOM constructors, so these two references have the same name but
are different objects:

```javascript
window.HTMLInputElement !== frame.contentWindow.HTMLInputElement
```

An input created by the framed application inherits from the iframe's
`HTMLInputElement.prototype`. The resulting JavaScript checks behave like this:

```javascript
input instanceof window.HTMLInputElement                     // false
input instanceof frame.contentWindow.HTMLInputElement        // true
```

TeaVM represents JavaScript DOM types such as `HTMLInputElement` as JSO types.
When TeaVM 0.15 compiles a Java `instanceof` expression or checked cast, its
normal JavaScript check receives the constructor from the test page. That works
for elements created in the test page, but it rejects an element of the correct
type when `FramedApplication` returns it from an iframe.

Mockatcha DOM includes a TeaVM compiler plugin that makes this check aware of
the element's window. It first performs TeaVM's normal constructor check. If
that fails for a native DOM type, it follows the element directly to
`ownerDocument.defaultView`; it does not traverse or infer the frame hierarchy.
The plugin obtains the constructor with the same name from that window and
checks the element against it. This allows:

```java
HTMLInputElement name =
        (HTMLInputElement) app.page().findByLabelText("Child name");
```

The fallback is restricted to standard DOM node and element constructors. It
does not accept a cast merely because another constructor has the same name.
Casting the input to `HTMLButtonElement` therefore still throws
`ClassCastException`, and checks for unrelated JavaScript types keep TeaVM's
normal behaviour.

TeaVM discovers the plugin automatically from the `mockatcha-dom` dependency;
tests do not need to register it.

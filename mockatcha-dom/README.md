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

## Set up the test boundary

`Dom.container()` creates an element for the test and attaches it to the
document. Pass that element to the application or framework that renders the
UI:

```java
timesheet.renderInto(Dom.container());

expect(findByRole("heading", "July timesheet")).toBeVisible();
```

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
promise or framework work updates the DOM. There is no need to uninstall the
clock between those operations.

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

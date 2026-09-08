package io.instanto.mockatcha.dom;

import static io.instanto.mockatcha.dom.Expect.expect;
import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLIFrameElement;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class FramedApplicationIntegrationTest {
  @Rule public DomRule dom = new DomRule();

  private FramedApplication open() {
    return FramedApplication.open("/resources/applications/plain-js/index.html")
        .awaitReady(page -> "true".equals(page.root().getAttribute("data-ready")));
  }

  @Test public void scopesLabelsRolesAndStylesToEachDocument() {
    Dom.render("<span id='field-name'>Parent name</span><input aria-labelledby='field-name'>");
    try (FramedApplication first = open(); FramedApplication second = open()) {
      DomScope child = first.page();
      expect(child.findByRole("textbox", "Child name")).toHaveAccessibleName("Child name");
      expect(child.findByLabelText("Child name")).toHaveValue("");
      expect(child.findByRole("button", "Toggle")).toHaveStyle("color", "rgb(12, 34, 56)");
      Dom.click(child.findByRole("button", "Toggle"));
      expect(child.findByRole("button", "Toggle")).toHaveAttribute("aria-pressed", "true");
      expect(second.page().findByRole("button", "Toggle")).toHaveAttribute("aria-pressed", "false");
      expect(Dom.findByRole("textbox", "Parent name")).toHaveValue("");
      assertThrows(IllegalArgumentException.class, () -> child.within(second.page().root()));
    }
  }

  @Test public void supportsArbitraryDocumentScopesAndChildFocus() {
    try (FramedApplication app = open()) {
      DomScope scope = Dom.within(app.page().root());
      Dom.focus(scope.findByLabelText("Child name"));
      expect(scope.findByLabelText("Child name")).toHaveFocus();
      expect(scope.tab()).toHaveAccessibleName("Toggle");
      expect(scope.shiftTab()).toHaveAccessibleName("Child name");
    }
  }

  @Test public void createsInputAndSelectionEventsInTheChildWindow() {
    try (FramedApplication app = open()) {
      DomScope scope = app.page();
      Dom.type(scope.findByLabelText("Child name"), "Ada");
      expect(scope.findByRole("status")).toHaveText("Typed Ada");
      Dom.select(scope.findByLabelText("Choice"), "B");
      expect(scope.findByRole("status")).toHaveText("Selected B");
    }
  }

  @Test public void preservesCheckedDomCastsIncludingCollectionResults() {
    try (FramedApplication app = open()) {
      HTMLElement input = app.page().findAll("input").get(0);
      assertTrue(input instanceof HTMLInputElement);
      assertFalse(input instanceof HTMLButtonElement);
      HTMLInputElement narrowed = (HTMLInputElement) input;
      assertEquals("", narrowed.getValue());
      assertThrows(ClassCastException.class, () -> {
        HTMLButtonElement invalid = (HTMLButtonElement) input;
        invalid.getValue();
      });
    }
  }

  @Test public void scopesIntoANestedFrameAndUsesItsViewport() {
    try (FramedApplication app = open()) {
      app.resize(375, 640);
      Dom.waitFor(() -> expect(app.page().findByRole("button", "Toggle"))
          .toHaveStyle("color", "rgb(65, 43, 21)"));
      HTMLElement body = app.page().root();
      HTMLIFrameElement nested = (HTMLIFrameElement) body.getOwnerDocument().createElement("iframe");
      nested.setSourceDocument("<!doctype html><html><head></head><body>"
          + "<span id='field-name'>Nested name</span><input aria-labelledby='field-name'></body></html>");
      body.appendChild(nested);
      Dom.waitFor(() -> {
        HTMLDocument document = nested.getContentDocument();
        assertNotNull(document);
        assertNotNull(document.getBody());
        expect(Dom.within(document.getBody()).findByRole("textbox", "Nested name"))
            .toHaveAccessibleName("Nested name");
      });
    }
  }

  @Test public void allowsAScopeOutsideTheRunnerContainer() {
    HTMLElement section = HTMLDocument.current().createElement("section");
    section.setInnerHTML("<button>Outside container</button>");
    HTMLDocument.current().getBody().appendChild(section);
    try {
      expect(Dom.within(section).findByRole("button", "Outside container")).toHaveText("Outside container");
      assertThrows(IllegalArgumentException.class, () -> Dom.page().within(section));
    } finally {
      section.getParentNode().removeChild(section);
    }
  }

  @Test public void rejectsCrossOriginUrlsBeforeCreatingAFrame() {
    assertThrows(IllegalArgumentException.class, () -> FramedApplication.open("https://example.invalid/"));
    assertEquals(0, Dom.findAll("iframe").size());
  }

  @Test public void reportsCrossOriginNavigationAndStillRemovesTheFrame() {
    FramedApplication app = open();
    Dom.find("iframe").setAttribute("src", "data:text/html,<h1>Different origin</h1>");
    Dom.waitFor(() -> assertThrows(IllegalStateException.class, app::assertHealthy));
    assertThrows(IllegalStateException.class, app::close);
    assertEquals(0, Dom.findAll("iframe").size());
  }

  @Test public void closingRemovesTheFrameAndPreventsFurtherAccess() {
    FramedApplication app = open();
    app.close();
    app.close();
    assertEquals(0, Dom.findAll("iframe").size());
    assertThrows(IllegalStateException.class, app::page);
  }

  @Test public void reportsReadinessTimeoutWithApplicationUrl() {
    try (FramedApplication app = open()) {
      AssertionError failure = assertThrows(AssertionError.class, () -> app.awaitReady(page -> false, 20));
      assertTrue(failure.getMessage(), failure.getMessage().contains("plain-js/index.html"));
    }
  }

  @Test public void capturesExceptionsFromTheFirstApplicationScript() {
    assertBrokenApplication("failure", "Startup exploded");
  }

  @Test public void capturesUnhandledRejections() {
    assertBrokenApplication("rejection", "Startup rejected");
  }

  @Test public void capturesMissingApplicationResources() {
    assertBrokenApplication("missing", "does-not-exist.js");
  }

  private void assertBrokenApplication(String path, String message) {
    FramedApplication app = FramedApplication.open("/resources/applications/" + path + "/index.html");
    try {
      IllegalStateException failure = assertThrows(IllegalStateException.class,
          () -> app.awaitReady(page -> false, 3000));
      assertTrue(failure.getMessage(), failure.getMessage().contains(message));
    } finally {
      assertThrows(IllegalStateException.class, app::close);
    }
    assertEquals(0, Dom.findAll("iframe").size());
  }
}

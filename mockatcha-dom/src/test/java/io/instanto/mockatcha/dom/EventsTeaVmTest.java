package io.instanto.mockatcha.dom;

import static io.instanto.mockatcha.dom.Dom.click;
import static io.instanto.mockatcha.dom.Dom.blur;
import static io.instanto.mockatcha.dom.Dom.check;
import static io.instanto.mockatcha.dom.Dom.clear;
import static io.instanto.mockatcha.dom.Dom.find;
import static io.instanto.mockatcha.dom.Dom.fire;
import static io.instanto.mockatcha.dom.Dom.focus;
import static io.instanto.mockatcha.dom.Dom.hover;
import static io.instanto.mockatcha.dom.Dom.press;
import static io.instanto.mockatcha.dom.Dom.render;
import static io.instanto.mockatcha.dom.Dom.select;
import static io.instanto.mockatcha.dom.Dom.selectOptions;
import static io.instanto.mockatcha.dom.Dom.tab;
import static io.instanto.mockatcha.dom.Dom.tabBack;
import static io.instanto.mockatcha.dom.Dom.type;
import static io.instanto.mockatcha.dom.Dom.uncheck;
import static io.instanto.mockatcha.dom.Dom.unhover;
import static io.instanto.mockatcha.dom.Expect.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Driving a component the way someone using it would. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class EventsTeaVmTest {

  @Rule
  public DomRule dom = new DomRule();

  private final List<String> seen = new ArrayList<>();

  @Test
  public void clickReachesAListener() {
    render("<button>Save</button>");
    listen(find("button"), "click");

    click("button");

    assertEquals(List.of("click"), seen);
  }

  @Test
  public void typingSetsTheValueAndReportsIt() {
    render("<input id='term'>");
    listen(find("#term"), "input");
    listen(find("#term"), "change");

    type(find("#term"), "A-17");

    expect(find("#term")).toHaveValue("A-17");
    assertEquals(List.of("input", "input", "input", "input", "change"), seen);
  }

  @Test
  public void typingTreatsAUnicodeCodePointAsOneCharacter() {
    render("<input id='term'>");
    listen(find("#term"), "input");

    type(find("#term"), "A😀");

    expect(find("#term")).toHaveValue("A😀");
    assertEquals(List.of("input", "input"), seen);
  }

  @Test
  public void typingKeepsTheFullDecimalValueInANumberInput() {
    render("<input id='hours' type='number'>");

    type(find("#hours"), "1.5");

    expect(find("#hours")).toHaveValue("1.5");
  }

  @Test
  public void cancellingBeforeInputPreventsTheEditButStillReleasesTheKey() {
    render("<input id='term'>");
    find("#term").addEventListener("beforeinput", (EventListener<Event>) Event::preventDefault);
    listen(find("#term"), "keyup");

    type(find("#term"), "A");

    expect(find("#term")).toHaveValue("");
    assertEquals(List.of("keyup"), seen);
  }

  @Test
  public void keysArriveWithTheirName() {
    render("<input id='term'>");
    find("#term")
        .addEventListener(
            "keydown", (EventListener<Event>) event -> seen.add("keydown:" + keyOf(event)));

    press(find("#term"), "Enter");

    assertEquals(List.of("keydown:Enter"), seen);
  }

  @Test
  public void eventsBubbleToAnAncestor() {
    render("<form id='outer'><button>Save</button></form>");
    listen(find("#outer"), "click");

    click("button");

    assertEquals(List.of("click"), seen);
  }

  @Test
  public void anyEventCanBeDispatched() {
    render("<form id='outer'></form>");
    listen(find("#outer"), "submit");

    fire(find("#outer"), "submit");

    assertEquals(List.of("submit"), seen);
  }

  @Test
  public void focusAndBlurUseTheBrowsersActiveElement() {
    render("<input id='term'>");

    focus("#term");
    expect(find("#term")).toHaveFocus();

    blur("#term");
    expect(find("#term")).not().toHaveFocus();
  }

  @Test
  public void selectingAnOptionChangesItsValueAndReportsBothEvents() {
    render("<select id='status'><option value='open'>Open</option>"
        + "<option value='closed'>Closed</option></select>");
    listen(find("#status"), "input");
    listen(find("#status"), "change");

    select("#status", "closed");

    expect(find("#status")).toHaveValue("closed");
    assertEquals(List.of("input", "change"), seen);
  }

  @Test
  public void clearingReportsTheEmptyValue() {
    render("<input id='term' value='A-17'>");
    listen(find("#term"), "input");
    listen(find("#term"), "change");

    clear("#term");

    expect(find("#term")).toHaveValue("");
    assertEquals(List.of("input", "change"), seen);
  }

  @Test
  public void checksAndUnchecksACheckbox() {
    render("<input type='checkbox' id='agree'>");

    check("#agree");
    expect(find("#agree")).toBeChecked();

    uncheck("#agree");
    expect(find("#agree")).not().toBeChecked();
  }

  @Test
  public void tabsThroughFocusableElementsInDocumentOrder() {
    render("<button id='first'>First</button><input id='second'><a id='third' href='#'>Third</a>");

    tab();
    expect(find("#first")).toHaveFocus();
    tab();
    expect(find("#second")).toHaveFocus();
    tabBack();
    expect(find("#first")).toHaveFocus();
  }

  @Test
  public void hoverReportsPointerEntryAndExit() {
    render("<button id='details'>Details</button>");
    listen(find("#details"), "mouseover");
    listen(find("#details"), "mouseenter");
    listen(find("#details"), "mouseout");
    listen(find("#details"), "mouseleave");

    hover("#details");
    unhover("#details");

    assertEquals(List.of("mouseover", "mouseenter", "mouseout", "mouseleave"), seen);
  }

  @Test
  public void selectsSeveralOptions() {
    render("<select id='teams' multiple>"
        + "<option value='ops'>Operations</option>"
        + "<option value='analytics'>Analytics</option>"
        + "<option value='airports'>Airports</option></select>");

    selectOptions("#teams", "ops", "airports");

    expect(find("option[value=ops]")).toBeSelected();
    expect(find("option[value=analytics]")).not().toBeSelected();
    expect(find("option[value=airports]")).toBeSelected();
  }

  @Test
  public void aMissingOptionDoesNotChangeTheSelection() {
    render("<select id='status'><option value='open'>Open</option>"
        + "<option value='closed'>Closed</option></select>");

    assertThrows(AssertionError.class, () -> selectOptions("#status", "missing"));

    expect(find("#status")).toHaveValue("open");
  }

  @Test
  public void aSingleSelectRejectsSeveralValues() {
    render("<select id='status'><option value='open'>Open</option>"
        + "<option value='closed'>Closed</option></select>");

    assertThrows(
        IllegalArgumentException.class, () -> selectOptions("#status", "open", "closed"));
  }

  private void listen(org.teavm.jso.dom.html.HTMLElement element, String type) {
    element.addEventListener(type, (EventListener<Event>) event -> seen.add(type));
  }

  private static String keyOf(Event event) {
    return KeyReader.key(event);
  }

  static final class KeyReader {
    @org.teavm.jso.JSBody(params = {"event"}, script = "return event.key;")
    static native String key(Event event);
  }
}

package io.instanto.mockatcha.dom;

import static io.instanto.mockatcha.dom.Dom.clear;
import static io.instanto.mockatcha.dom.Dom.find;
import static io.instanto.mockatcha.dom.Dom.press;
import static io.instanto.mockatcha.dom.Dom.render;
import static io.instanto.mockatcha.dom.Dom.type;
import static io.instanto.mockatcha.dom.Expect.expect;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Typing and keyboard behavior for editable controls. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class TextInputEventsTeaVmTest {

  @Rule
  public DomRule dom = new DomRule();

  private final List<String> seen = new ArrayList<>();

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
            "keydown", (EventListener<Event>) event -> seen.add("keydown:" + KeyReader.key(event)));

    press(find("#term"), "Enter");

    assertEquals(List.of("keydown:Enter"), seen);
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

  private void listen(org.teavm.jso.dom.html.HTMLElement element, String type) {
    element.addEventListener(type, (EventListener<Event>) event -> seen.add(type));
  }

  static final class KeyReader {
    @org.teavm.jso.JSBody(params = {"event"}, script = "return event.key;")
    static native String key(Event event);
  }
}

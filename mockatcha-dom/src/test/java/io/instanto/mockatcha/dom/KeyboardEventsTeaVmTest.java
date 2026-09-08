package io.instanto.mockatcha.dom;

import static io.instanto.mockatcha.dom.Dom.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class KeyboardEventsTeaVmTest {
  @Rule public DomRule dom = new DomRule();

  private final List<LegacyKeyboardEvent> events = new ArrayList<>();

  interface LegacyKeyboardEvent extends KeyboardEvent {
    @JSProperty int getWhich();
  }

  private void listen() {
    render("<div id='parent'><input id='key'></div>");
    for (String type : List.of("keydown", "keypress", "keyup")) {
      find("#parent").addEventListener(type,
          (EventListener<Event>) event -> events.add((LegacyKeyboardEvent) event));
    }
  }

  private void assertPress(String input, String key, String code, int legacy) {
    events.clear();
    press(find("#key"), input);
    assertEquals(2, events.size());
    assertEquals("keydown", events.get(0).getType());
    assertEquals("keyup", events.get(1).getType());
    for (LegacyKeyboardEvent event : events) {
      assertEquals(key, event.getKey());
      assertEquals(code, event.getCode());
      assertEquals(legacy, event.getKeyCode());
      assertEquals(legacy, event.getWhich());
      assertEquals(0, event.getCharCode());
      assertTrue(event.isBubbles());
      assertTrue(event.isCancelable());
    }
  }

  @Test public void arrowKeysReachModernAndLegacyListeners() {
    listen();
    assertPress("ArrowLeft", "ArrowLeft", "ArrowLeft", 37);
    assertPress("ArrowUp", "ArrowUp", "ArrowUp", 38);
    assertPress("ArrowRight", "ArrowRight", "ArrowRight", 39);
    assertPress("ArrowDown", "ArrowDown", "ArrowDown", 40);
  }

  @Test public void escapeEnterSpaceAndTabHaveLegacyCodes() {
    listen();
    assertPress("Escape", "Escape", "Escape", 27);
    assertPress("Enter", "Enter", "Enter", 13);
    assertPress(" ", " ", "Space", 32);
    assertPress("Tab", "Tab", "Tab", 9);
  }

  @Test public void navigationAndEditingKeysHaveLegacyCodes() {
    listen();
    assertPress("Backspace", "Backspace", "Backspace", 8);
    assertPress("Delete", "Delete", "Delete", 46);
    assertPress("Insert", "Insert", "Insert", 45);
    assertPress("Home", "Home", "Home", 36);
    assertPress("End", "End", "End", 35);
    assertPress("PageUp", "PageUp", "PageUp", 33);
    assertPress("PageDown", "PageDown", "PageDown", 34);
  }

  @Test public void aliasesAreNormalized() {
    listen();
    assertPress("Esc", "Escape", "Escape", 27);
    assertPress("Left", "ArrowLeft", "ArrowLeft", 37);
    assertPress("Spacebar", " ", "Space", 32);
    assertPress("Del", "Delete", "Delete", 46);
  }

  @Test public void asciiAndFunctionKeysUseConsistentCodes() {
    listen();
    assertPress("a", "a", "KeyA", 65);
    assertPress("A", "A", "KeyA", 65);
    assertPress("7", "7", "Digit7", 55);
    assertPress("&", "&", "Digit7", 55);
    assertPress(";", ";", "Semicolon", 186);
    assertPress("?", "?", "Slash", 191);
    assertPress("F1", "F1", "F1", 112);
    assertPress("F12", "F12", "F12", 123);
    assertPress("F24", "F24", "F24", 135);
  }

  @Test public void unknownPhysicalKeysAreNotInvented() {
    listen();
    assertPress("Unidentified", "Unidentified", "", 0);
    assertPress("\uD83D\uDE00", "\uD83D\uDE00", "", 0);
  }

  @Test public void typingUsesCharacterCodesOnlyForKeypress() {
    listen();
    type(find("#key"), "a");
    assertEquals(3, events.size());
    assertEquals("keypress", events.get(1).getType());
    for (int i = 0; i < events.size(); i++) {
      LegacyKeyboardEvent event = events.get(i);
      assertEquals("a", event.getKey());
      assertEquals("KeyA", event.getCode());
      assertEquals(i == 1 ? 97 : 65, event.getKeyCode());
      assertEquals(i == 1 ? 97 : 65, event.getWhich());
      assertEquals(i == 1 ? 97 : 0, event.getCharCode());
    }
  }

  @Test public void legacyKeydownHandlerCanCancelTypingAndStillSeeKeyup() {
    listen();
    find("#key").addEventListener("keydown", (EventListener<KeyboardEvent>) event -> {
      if (event.getKeyCode() == 65) event.preventDefault();
    });
    type(find("#key"), "a");
    assertEquals(2, events.size());
    assertEquals("keyup", events.get(1).getType());
    assertEquals(65, events.get(1).getWhich());
    Expect.expect(find("#key")).toHaveValue("");
  }

  @Test public void cancelledKeypressPreventsEditingAndStillReleasesKey() {
    listen();
    find("#key").addEventListener("keypress", (EventListener<Event>) Event::preventDefault);
    type(find("#key"), "a");
    assertEquals(3, events.size());
    assertEquals("keyup", events.get(2).getType());
    Expect.expect(find("#key")).toHaveValue("");
  }
}

/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.dom;

import static io.instanto.mockatcha.dom.Dom.check;
import static io.instanto.mockatcha.dom.Dom.find;
import static io.instanto.mockatcha.dom.Dom.render;
import static io.instanto.mockatcha.dom.Dom.select;
import static io.instanto.mockatcha.dom.Dom.selectOptions;
import static io.instanto.mockatcha.dom.Dom.uncheck;
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

/** Checkbox and select-control behavior. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class SelectionEventsTeaVmTest {

  @Rule
  public DomRule dom = new DomRule();

  private final List<String> seen = new ArrayList<>();

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
  public void checksAndUnchecksACheckbox() {
    render("<input type='checkbox' id='agree'>");

    check("#agree");
    expect(find("#agree")).toBeChecked();

    uncheck("#agree");
    expect(find("#agree")).not().toBeChecked();
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
}

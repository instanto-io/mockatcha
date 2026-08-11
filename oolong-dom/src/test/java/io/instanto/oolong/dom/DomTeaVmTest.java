package io.instanto.oolong.dom;

import static io.instanto.oolong.dom.Dom.find;
import static io.instanto.oolong.dom.Dom.findAll;
import static io.instanto.oolong.dom.Dom.findByText;
import static io.instanto.oolong.dom.Dom.render;
import static io.instanto.oolong.dom.DomAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/** Rendering into a container, finding things in it, and asserting on what is there. */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class DomTeaVmTest {

  @Rule
  public DomRule dom = new DomRule();

  @Test
  public void findsAnElementBySelector() {
    render("<h1 class='title'>Hello Ada</h1>");

    assertThat(find("h1")).hasText("Hello Ada").hasClass("title");
  }

  @Test
  public void findsEveryMatchingElement() {
    render("<ul><li>one</li><li>two</li><li>three</li></ul>");

    assertEquals(3, findAll("li").size());
    assertThat(findAll("li").get(1)).hasText("two");
  }

  @Test
  public void findsAnElementByItsText() {
    render("<div><button id='save'>Save</button><button>Cancel</button></div>");

    assertThat(findByText("Save")).hasAttribute("id", "save");
  }

  @Test
  public void saysWhatWasThereWhenNothingMatches() {
    render("<p>only a paragraph</p>");

    AssertionError failure = assertThrows(AssertionError.class, () -> find(".missing"));

    assertTrue(failure.getMessage(), failure.getMessage().contains("No element matching"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("only a paragraph"));
  }

  @Test
  public void showsTheElementWhenAnAssertionFails() {
    render("<h1 class='title'>Hello Ada</h1>");

    AssertionError failure =
        assertThrows(AssertionError.class, () -> assertThat(find("h1")).hasText("Goodbye"));

    assertTrue(failure.getMessage(), failure.getMessage().contains("Hello Ada"));
    assertTrue(failure.getMessage(), failure.getMessage().contains("<h1"));
  }

  @Test
  public void assertsOnAttributesClassesAndState() {
    render("<input id='name' value='A-17' class='field wide' disabled>");

    assertThat(find("#name"))
        .hasValue("A-17")
        .hasClass("wide")
        .doesNotHaveClass("narrow")
        .hasAttribute("id", "name")
        .isDisabled();
  }

  @Test
  public void assertsOnVisibility() {
    render("<p id='shown'>here</p><p id='gone' style='display:none'>hidden</p>");

    assertThat(find("#shown")).isVisible();
    assertThat(find("#gone")).isHidden();
  }

  @Test
  public void doesNotSeeMarkupFromAnotherTest() {
    // The rule removed the previous test's container, so nothing here is left over.
    assertNull(Dom.findOrNull("#name"));
    assertNull(Dom.findOrNull("h1"));
  }
}

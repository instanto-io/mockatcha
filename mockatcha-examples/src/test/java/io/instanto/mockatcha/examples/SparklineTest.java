/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.examples;

import static io.instanto.mockatcha.ArgumentMatchers.anyInt;
import static io.instanto.mockatcha.Mockatcha.mock;
import static io.instanto.mockatcha.Mockatcha.verify;
import static io.instanto.mockatcha.Mockatcha.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * A component that draws on a real canvas, with the readings it draws mocked.
 *
 * <p>There is no JVM equivalent of this test. The canvas is a browser object, the pixels are read
 * back from it, and the only part replaced is where the numbers came from.
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class SparklineTest {

  private static final int WIDTH = 100;
  private static final int HEIGHT = 100;

  @Test
  public void drawsTheReadingsItIsGiven() {
    ReadingSource readings = mock(ReadingSource.class);
    when(readings.recent(anyInt())).thenReturn(List.of(0, 100, 0));

    CanvasRenderingContext2D canvas = newCanvas();
    new Sparkline(readings).draw(canvas, WIDTH, HEIGHT);

    assertTrue("nothing was drawn", inkCount(canvas) > 0);
    verify(readings).recent(anyInt());
  }

  @Test
  public void drawsNothingWhenThereIsNothingToDraw() {
    ReadingSource readings = mock(ReadingSource.class);
    when(readings.recent(anyInt())).thenReturn(List.of());

    CanvasRenderingContext2D canvas = newCanvas();
    new Sparkline(readings).draw(canvas, WIDTH, HEIGHT);

    assertEquals(0, inkCount(canvas));
  }

  @Test
  public void putsALowReadingAtTheBottomAndAHighOneAtTheTop() {
    ReadingSource readings = mock(ReadingSource.class);
    when(readings.recent(anyInt())).thenReturn(List.of(0, 0));

    CanvasRenderingContext2D canvas = newCanvas();
    new Sparkline(readings).draw(canvas, WIDTH, HEIGHT);

    assertTrue("a reading of 0 should sit at the bottom", hasInkInRows(canvas, 90, 100));
    assertFalse("a reading of 0 should leave the top clear", hasInkInRows(canvas, 0, 10));
  }

  @Test
  public void asksForAsManyReadingsAsItCanFit() {
    ReadingSource readings = mock(ReadingSource.class);
    when(readings.recent(anyInt())).thenReturn(List.of(10, 20));

    new Sparkline(readings).draw(newCanvas(), WIDTH, HEIGHT);

    verify(readings).recent(WIDTH / 10);
  }

  private static CanvasRenderingContext2D newCanvas() {
    HTMLCanvasElement element =
        (HTMLCanvasElement) HTMLDocument.current().createElement("canvas");
    element.setWidth(WIDTH);
    element.setHeight(HEIGHT);
    return (CanvasRenderingContext2D) element.getContext("2d");
  }

  /** How many pixels the component actually painted, read back from the canvas. */
  private static int inkCount(CanvasRenderingContext2D canvas) {
    return countInk(canvas, 0, HEIGHT);
  }

  private static boolean hasInkInRows(CanvasRenderingContext2D canvas, int fromRow, int toRow) {
    return countInk(canvas, fromRow, toRow) > 0;
  }

  private static int countInk(CanvasRenderingContext2D canvas, int fromRow, int toRow) {
    ImageData pixels = canvas.getImageData(0, fromRow, WIDTH, toRow - fromRow);
    int painted = 0;
    for (int index = 3; index < pixels.getData().getLength(); index += 4) {
      if (pixels.getData().get(index) != 0) {
        painted++;
      }
    }
    return painted;
  }
}

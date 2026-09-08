/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.examples;

import java.util.List;
import org.teavm.jso.canvas.CanvasRenderingContext2D;

/** Draws recent readings as a line across a canvas. */
public final class Sparkline {

  private final ReadingSource readings;

  public Sparkline(ReadingSource readings) {
    this.readings = readings;
  }

  /** Draws one point per reading, spread evenly across the width. */
  public void draw(CanvasRenderingContext2D canvas, int width, int height) {
    List<Integer> values = readings.recent(width / 10);
    if (values.size() < 2) {
      return;
    }

    canvas.setLineWidth(2);
    canvas.setStrokeStyle("#000000");
    canvas.beginPath();
    double step = (double) width / (values.size() - 1);
    for (int index = 0; index < values.size(); index++) {
      double x = index * step;
      double y = height - values.get(index) * height / 100.0;
      if (index == 0) {
        canvas.moveTo(x, y);
      } else {
        canvas.lineTo(x, y);
      }
    }
    canvas.stroke();
  }
}

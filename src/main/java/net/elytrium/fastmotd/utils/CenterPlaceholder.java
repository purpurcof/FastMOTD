/*
 * Copyright (C) 2022 - 2025 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.fastmotd.utils;

import net.elytrium.fastmotd.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.lenni0451.mcstructs.text.utils.TextWidthUtils;

public final class CenterPlaceholder {

  private static final String PLACEHOLDER = "<center>";

  private CenterPlaceholder() {}

  public static boolean contains(String input) {
    return input.contains(PLACEHOLDER);
  }

  public static Component deserialize(ComponentSerializer<Component, Component, String> serializer, String input) {
    String normalized = input.replace("{NL}", "\n");
    if (!normalized.contains(PLACEHOLDER)) {
      return serializer.deserialize(normalized);
    }

    String[] lines = normalized.split("\n", 2);
    boolean[] centeredLines = new boolean[2];

    for (int i = 0; i < lines.length; i++) {
      if (lines[i].startsWith(PLACEHOLDER)) {
        centeredLines[i] = true;
        lines[i] = lines[i].substring(PLACEHOLDER.length());
      }
    }

    Component component = serializer.deserialize(String.join("\n", lines));
    return centerText(component, centeredLines);
  }

  private static Component centerText(Component component, boolean[] centeredLines) {
    if (!hasCenteredLines(centeredLines)) {
      return component;
    }

    float[] widths = new float[centeredLines.length];
    propagateWidths(component, false, new int[] {0}, widths);
    float lineWidth = (float) Settings.IMP.MAIN.CENTER_LINE_LENGTH * TextWidthUtils.getCharWidth('A', 1, false);
    float normalSpaceWidth = TextWidthUtils.getCharWidth(' ', 1, false);
    float boldSpaceWidth = TextWidthUtils.getCharWidth(' ', 1, true);
    Style normalSpaceStyle = Style.style().decoration(TextDecoration.BOLD, false).build();
    Style boldSpaceStyle = Style.style().decoration(TextDecoration.BOLD, true).build();

    for (int i = 0; i < widths.length; i++) {
      if (!centeredLines[i]) {
        continue;
      }

      int[] leftPaddingSpaces = getLeftPadding(widths[i], normalSpaceWidth, boldSpaceWidth, lineWidth);
      Component normalPadding = Component.text(" ".repeat(leftPaddingSpaces[0])).style(normalSpaceStyle);
      Component boldPadding = Component.text(" ".repeat(leftPaddingSpaces[1])).style(boldSpaceStyle);

      if (i == 0) {
        component = normalPadding.append(boldPadding).append(component);
      } else {
        component = prependToLine(component, i, normalPadding, boldPadding);
      }
    }

    return component;
  }

  private static Component prependToLine(Component component, int targetLine, Component normalPadding, Component boldPadding) {
    if (targetLine != 1) {
      return component;
    }

    return component.replaceText(config -> config
        .matchLiteral("\n")
        .once()
        .replacement(Component.newline().append(normalPadding).append(boldPadding)));
  }

  private static void propagateWidths(Component component, boolean isParentBold, int[] currentLine, float[] widths) {
    boolean currentBold = isBold(component.style(), isParentBold);
    String stringContent = component instanceof TextComponent textComponent ? textComponent.content() : "";

    for (int i = 0; i < stringContent.length(); i++) {
      char currentChar = stringContent.charAt(i);
      if (currentChar == '\n') {
        currentLine[0]++;
        if (currentLine[0] >= widths.length) {
          return;
        }

        continue;
      }

      if (currentLine[0] < widths.length) {
        widths[currentLine[0]] += TextWidthUtils.getCharWidth(currentChar, 1, currentBold);
      }
    }

    for (Component child : component.children()) {
      propagateWidths(child, currentBold, currentLine, widths);
    }
  }

  private static boolean hasCenteredLines(boolean[] centeredLines) {
    for (boolean centeredLine : centeredLines) {
      if (centeredLine) {
        return true;
      }
    }

    return false;
  }

  private static boolean isBold(Style style, boolean parentBold) {
    TextDecoration.State boldState = style.decoration(TextDecoration.BOLD);
    if (boldState == TextDecoration.State.TRUE) {
      return true;
    }

    if (boldState == TextDecoration.State.FALSE) {
      return false;
    }

    return parentBold;
  }

  private static int[] getLeftPadding(float textWidth, float spaceWidth, float spaceBoldWidth, float lineLength) {
    float totalPaddingWidth = lineLength - textWidth;
    if (totalPaddingWidth <= 0) {
      return new int[] {0, 0};
    }

    float leftPaddingWidth = totalPaddingWidth / 2.0f;
    return findClosestPadding(spaceWidth, spaceBoldWidth, leftPaddingWidth);
  }

  private static int[] findClosestPadding(float normalSpaceWidth, float boldSpaceWidth, float targetWidth) {
    int bestNormal = 0;
    int bestBold = 0;
    float minDifference = Float.MAX_VALUE;
    int maxBoldSpaces = (int) (targetWidth / boldSpaceWidth);

    for (int bold = 0; bold <= maxBoldSpaces; bold++) {
      float remainingWidth = targetWidth - boldSpaceWidth * bold;
      int normal = Math.max(0, Math.round(remainingWidth / normalSpaceWidth));
      float value = normalSpaceWidth * normal + boldSpaceWidth * bold;
      float difference = Math.abs(value - targetWidth);
      if (difference < minDifference) {
        minDifference = difference;
        bestNormal = normal;
        bestBold = bold;
      }
    }

    return new int[] {bestNormal, bestBold};
  }
}

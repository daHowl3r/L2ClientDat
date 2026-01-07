/*
 * This file is part of the L2ClientDat project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.forms;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/**
 * Code editor area with cleanup helpers for the main editor pane.
 */
public class EditorCodeArea extends CodeArea
{
	private static final Pattern TOKEN_PATTERN = Pattern.compile("([^=\\s]+)=(\\[[^\\]]*\\]|\\{[^}]*\\}|[^\\s]+)?");

	/**
	 * Creates a code area that applies syntax highlighting for key/value tokens.
	 */
	public EditorCodeArea()
	{
		textProperty().addListener((observable, oldValue, newValue) -> setStyleSpans(0, buildHighlighting(newValue)));
	}

	/**
	 * Clears the current editor text and undo history.
	 */
	public void cleanUp()
	{
		clear();
		discardAllEdits();
	}

	/**
	 * Discards tracked edits from the undo manager.
	 */
	public void discardAllEdits()
	{
		getUndoManager().forgetHistory();
	}

	private StyleSpans<java.util.Collection<String>> buildHighlighting(String text)
	{
		final String value = text == null ? "" : text;
		final StyleSpansBuilder<java.util.Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		final Matcher matcher = TOKEN_PATTERN.matcher(value);
		int lastEnd = 0;
		while (matcher.find())
		{
			if (matcher.start() > lastEnd)
			{
				spansBuilder.add(Collections.singleton("rich-text-normal"), matcher.start() - lastEnd);
			}
			final String key = matcher.group(1);
			final String valueToken = matcher.group(2);
			spansBuilder.add(Collections.singleton("rich-text-key"), key.length());
			spansBuilder.add(Collections.singleton("rich-text-separator"), 1);
			if (valueToken != null && !valueToken.isEmpty())
			{
				final int valueStart = matcher.start(2);
				final int valueEnd = matcher.end(2);
				if ((valueEnd - valueStart) >= 2 && ((valueToken.startsWith("[") && valueToken.endsWith("]")) || (valueToken.startsWith("{") && valueToken.endsWith("}"))))
				{
					spansBuilder.add(Collections.singleton("rich-text-separator"), 1);
					spansBuilder.add(Collections.singleton("rich-text-value"), valueToken.length() - 2);
					spansBuilder.add(Collections.singleton("rich-text-separator"), 1);
				}
				else
				{
					spansBuilder.add(Collections.singleton("rich-text-value"), valueToken.length());
				}
			}
			lastEnd = matcher.end();
		}
		if (lastEnd < value.length())
		{
			spansBuilder.add(Collections.singleton("rich-text-normal"), value.length() - lastEnd);
		}
		return spansBuilder.create();
	}
}
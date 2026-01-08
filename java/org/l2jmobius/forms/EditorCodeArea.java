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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.scene.control.IndexRange;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/**
 * Code editor area with cleanup helpers for the main editor pane.
 */
public class EditorCodeArea extends CodeArea
{
	private static final Pattern TOKEN_PATTERN = Pattern.compile("([^=\\s]+)=(\\[[^\\]]*\\]|\\{[^}]*\\}|[^\\s]+)?");
	private List<IndexRange> _errorRanges = Collections.emptyList();

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

	/**
	 * Updates the current error ranges used for styling.
	 * @param ranges the error ranges to apply, or empty to clear
	 */
	public void setErrorRanges(List<IndexRange> ranges)
	{
		_errorRanges = (ranges == null) ? Collections.emptyList() : new ArrayList<>(ranges);
		setStyleSpans(0, buildHighlighting(getText()));
	}

	private StyleSpans<Collection<String>> buildHighlighting(String text)
	{
		final String value = text == null ? "" : text;
		final StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
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
		return applyErrorRanges(spansBuilder.create(), _errorRanges, value.length());
	}

	private StyleSpans<Collection<String>> applyErrorRanges(StyleSpans<Collection<String>> spans, List<IndexRange> ranges, int textLength)
	{
		final List<IndexRange> normalizedRanges = normalizeErrorRanges(ranges, textLength);
		if (normalizedRanges.isEmpty())
		{
			return spans;
		}
		
		final StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
		int position = 0;
		int rangeIndex = 0;
		IndexRange currentRange = normalizedRanges.get(rangeIndex);
		for (StyleSpan<Collection<String>> span : spans)
		{
			final int spanLength = span.getLength();
			int spanStart = position;
			final int spanEnd = position + spanLength;
			while (spanStart < spanEnd)
			{
				if (currentRange == null || spanEnd <= currentRange.getStart())
				{
					builder.add(span.getStyle(), spanEnd - spanStart);
					spanStart = spanEnd;
					continue;
				}
				
				if (spanStart >= currentRange.getEnd())
				{
					currentRange = (++rangeIndex < normalizedRanges.size()) ? normalizedRanges.get(rangeIndex) : null;
					continue;
				}
				
				final int nonErrorEnd = Math.min(spanEnd, currentRange.getStart());
				if (spanStart < nonErrorEnd)
				{
					builder.add(span.getStyle(), nonErrorEnd - spanStart);
					spanStart = nonErrorEnd;
					continue;
				}
				
				final int errorEnd = Math.min(spanEnd, currentRange.getEnd());
				builder.add(addErrorStyle(span.getStyle()), errorEnd - spanStart);
				spanStart = errorEnd;
				if (currentRange != null && spanStart >= currentRange.getEnd())
				{
					currentRange = (++rangeIndex < normalizedRanges.size()) ? normalizedRanges.get(rangeIndex) : null;
				}
			}
			position += spanLength;
		}
		return builder.create();
	}

	private List<IndexRange> normalizeErrorRanges(List<IndexRange> ranges, int textLength)
	{
		if (ranges == null || ranges.isEmpty() || textLength <= 0)
		{
			return Collections.emptyList();
		}
		
		final List<IndexRange> normalized = new ArrayList<>();
		for (IndexRange range : ranges)
		{
			final int start = Math.max(0, Math.min(textLength, range.getStart()));
			final int end = Math.max(0, Math.min(textLength, range.getEnd()));
			if (end > start)
			{
				normalized.add(new IndexRange(start, end));
			}
		}
		normalized.sort(Comparator.comparingInt(IndexRange::getStart));
		return normalized;
	}

	private Collection<String> addErrorStyle(Collection<String> styles)
	{
		final ArrayList<String> combined = new ArrayList<>(styles);
		if (!combined.contains("rich-text-error"))
		{
			combined.add("rich-text-error");
		}
		return combined;
	}
}

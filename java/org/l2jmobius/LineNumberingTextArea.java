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
package org.l2jmobius;

import javafx.scene.control.TextArea;

/**
 * Text area that mirrors line numbers for the editor pane.
 */
public class LineNumberingTextArea extends TextArea
{
	private int lastLines = 0;
	
	/**
	 * Clears the line numbers and resets tracking state.
	 */
	public void cleanUp()
	{
		setText("");
		lastLines = 0;
	}
	
	/**
	 * Updates line numbers to match the provided text content.
	 * @param text the editor text
	 */
	public void updateText(String text)
	{
		final int length = countLines(text);
		if (length == lastLines)
		{
			return;
		}
		
		lastLines = length;
		final StringBuilder lineNumbersTextBuilder = new StringBuilder();
		lineNumbersTextBuilder.append("1").append(System.lineSeparator());
		for (int line = 2; line <= length; ++line)
		{
			lineNumbersTextBuilder.append(line).append(System.lineSeparator());
		}
		setText(lineNumbersTextBuilder.toString());
	}
	
	private int countLines(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return 1;
		}
		
		int count = 1;
		int index = text.indexOf('\n');
		while (index != -1)
		{
			count++;
			index = text.indexOf('\n', index + 1);
		}
		return count;
	}
}

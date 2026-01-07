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

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class JPopupTextArea extends TextArea
{
	private static final String COPY = "Copy (Ctrl + C)";
	private static final String CUT = "Cut (Ctrl + X)";
	private static final String PASTE = "Paste (Ctrl + V)";
	private static final String DELETE = "Delete";
	private static final String SELECT_ALL = "Select all (Ctrl + A)";
	private static final String GOTO = "Go to (Ctrl + G)";
	private static final String SEARCH = "Search (Ctrl + F)";
	
	public JPopupTextArea()
	{
		setContextMenu(createContextMenu());
		addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
	}
	
	public void discardAllEdits()
	{
	}
	
	public void cleanUp()
	{
		clear();
		discardAllEdits();
	}
	
	private ContextMenu createContextMenu()
	{
		final ContextMenu menu = new ContextMenu();
		final MenuItem copyItem = new MenuItem(COPY);
		copyItem.setOnAction(event -> copy());
		final MenuItem cutItem = new MenuItem(CUT);
		cutItem.setOnAction(event -> cut());
		final MenuItem pasteItem = new MenuItem(PASTE);
		pasteItem.setOnAction(event -> paste());
		final MenuItem deleteItem = new MenuItem(DELETE);
		deleteItem.setOnAction(event -> replaceSelection(""));
		final MenuItem selectAllItem = new MenuItem(SELECT_ALL);
		selectAllItem.setOnAction(event -> selectAll());
		final MenuItem selectLine = new MenuItem(GOTO);
		selectLine.setOnAction(event -> goToLine());
		final MenuItem selectFind = new MenuItem(SEARCH);
		selectFind.setOnAction(event -> searchString());
		
		menu.getItems().addAll(copyItem, cutItem, pasteItem, deleteItem, new SeparatorMenuItem(), selectAllItem, selectLine, selectFind);
		return menu;
	}
	
	protected void goToLine()
	{
		final TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle(GOTO);
		dialog.setHeaderText(null);
		dialog.setContentText("Line number:");
		final Optional<String> result = dialog.showAndWait();
		if (result.isEmpty())
		{
			return;
		}
		
		int lineNumber = 0;
		try
		{
			lineNumber = Integer.parseInt(result.get());
		}
		catch (NumberFormatException ex)
		{
			showError("Enter a valid line number");
			return;
		}
		
		if (lineNumber <= 0)
		{
			showError("Enter a valid line number");
			return;
		}
		
		final int lineStart = findLineStart(getText(), lineNumber);
		if (lineStart < 0)
		{
			showError("Line number does not exist");
			return;
		}
		
		requestFocus();
		positionCaret(lineStart);
	}
	
	protected void searchString()
	{
		final TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle(SEARCH);
		dialog.setHeaderText(null);
		dialog.setContentText("Search string:");
		final Optional<String> result = dialog.showAndWait();
		if (result.isEmpty() || result.get().isEmpty())
		{
			showError("Enter a search string");
			return;
		}
		
		requestFocus();
		final String editorText = getText();
		final String search = result.get();
		final int start = editorText.indexOf(search, getSelection().getEnd());
		if (start != -1)
		{
			selectRange(start, start + search.length());
		}
	}
	
	private void handleShortcut(KeyEvent event)
	{
		if (!event.isControlDown())
		{
			return;
		}
		
		if (event.getCode() == KeyCode.G)
		{
			goToLine();
			event.consume();
		}
		else if (event.getCode() == KeyCode.F)
		{
			searchString();
			event.consume();
		}
	}
	
	private int findLineStart(String text, int lineNumber)
	{
		int line = 1;
		int index = 0;
		while (line < lineNumber)
		{
			final int next = text.indexOf('\n', index);
			if (next < 0)
			{
				return -1;
			}
			
			index = next + 1;
			line++;
		}
		return index <= text.length() ? index : -1;
	}
	
	private void showError(String message)
	{
		final Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle("Error");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
}

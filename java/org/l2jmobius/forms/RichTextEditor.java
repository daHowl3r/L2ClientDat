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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Rich text editor that styles key/value tokens while delegating input to a hidden text area.
 */
public class RichTextEditor extends StackPane
{
	private static final Pattern TOKEN_PATTERN = Pattern.compile("([^=\\s]+)=\\[([^\\]]*)\\]");
	private static final String DEFAULT_SELECTION_STYLE = "-fx-control-inner-background: transparent; -fx-text-fill: transparent; -fx-highlight-fill: rgba(114, 135, 140, 0.6); -fx-highlight-text-fill: transparent; -fx-caret-color: white;";
	private static final String DEFAULT_BACKGROUND = "#293134";
	private final JPopupTextArea inputArea = new JPopupTextArea();
	private final TextFlow displayFlow = new TextFlow();
	private final ScrollPane displayScroll = new ScrollPane(displayFlow);
	private boolean scrollsBound = false;

	public RichTextEditor()
	{
		displayFlow.getStyleClass().add("rich-text-display");
		displayScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		displayScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		displayScroll.setFitToWidth(true);
		displayScroll.setFitToHeight(true);
		displayScroll.setMouseTransparent(true);
		displayScroll.setFocusTraversable(false);
		setEditorStyle("-fx-control-inner-background: " + DEFAULT_BACKGROUND + ";");
		inputArea.textProperty().addListener((observable, oldValue, newValue) -> updateStyles(newValue));
		inputArea.skinProperty().addListener((observable, oldValue, newValue) -> bindScrollBars());
		displayFlow.prefWidthProperty().bind(inputArea.widthProperty());
		getChildren().addAll(displayScroll, inputArea);
		updateStyles("");
	}

	public StringProperty textProperty()
	{
		return inputArea.textProperty();
	}

	public String getText()
	{
		return inputArea.getText();
	}

	public void setText(String text)
	{
		inputArea.setText(text);
	}

	public void appendText(String text)
	{
		inputArea.appendText(text);
	}

	public void discardAllEdits()
	{
		inputArea.discardAllEdits();
	}

	public void cleanUp()
	{
		inputArea.cleanUp();
	}

	public void setFont(Font font)
	{
		inputArea.setFont(font);
		final String style = String.format("-fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-font-weight: %s;",
				font.getFamily(),
				font.getSize(),
				font.getStyle().toLowerCase().contains("bold") ? "bold" : "normal");
		displayFlow.setStyle(style);
	}

	public void setEditorStyle(String style)
	{
		final String background = extractBackground(style);
		if (background != null)
		{
			displayScroll.setStyle("-fx-background-color: " + background + "; -fx-background: " + background + ";");
		}
		else
		{
			displayScroll.setStyle(style);
		}
		inputArea.setStyle(DEFAULT_SELECTION_STYLE);
	}

	public JPopupTextArea getInputArea()
	{
		return inputArea;
	}

	private void updateStyles(String text)
	{
		final String value = text == null ? "" : text;
		final List<Text> parts = new ArrayList<>();
		final Matcher matcher = TOKEN_PATTERN.matcher(value);
		int lastEnd = 0;
		while (matcher.find())
		{
			if (matcher.start() > lastEnd)
			{
				parts.add(styledText(value.substring(lastEnd, matcher.start()), "rich-text-normal"));
			}
			parts.add(styledText(matcher.group(1), "rich-text-key"));
			parts.add(styledText("=[", "rich-text-separator"));
			parts.add(styledText(matcher.group(2), "rich-text-value"));
			parts.add(styledText("]", "rich-text-separator"));
			lastEnd = matcher.end();
		}
		if (lastEnd < value.length())
		{
			parts.add(styledText(value.substring(lastEnd), "rich-text-normal"));
		}
		displayFlow.getChildren().setAll(parts);
	}

	private Text styledText(String text, String styleClass)
	{
		final Text node = new Text(text);
		node.getStyleClass().add(styleClass);
		return node;
	}

	private void bindScrollBars()
	{
		if (scrollsBound)
		{
			return;
		}
		scrollsBound = true;
		Platform.runLater(() ->
		{
			final ScrollBar vertical = (ScrollBar) inputArea.lookup(".scroll-bar:vertical");
			final ScrollBar horizontal = (ScrollBar) inputArea.lookup(".scroll-bar:horizontal");
			if (vertical != null)
			{
				displayScroll.vvalueProperty().bindBidirectional(vertical.valueProperty());
			}
			if (horizontal != null)
			{
				displayScroll.hvalueProperty().bindBidirectional(horizontal.valueProperty());
			}
		});
	}

	private String extractBackground(String style)
	{
		if ((style == null) || style.isBlank())
		{
			return null;
		}
		final Matcher matcher = Pattern.compile("-fx-control-inner-background\\s*:\\s*([^;]+)").matcher(style);
		return matcher.find() ? matcher.group(1).trim() : null;
	}
}

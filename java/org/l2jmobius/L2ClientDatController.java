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

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.HBox;
import org.l2jmobius.forms.JPopupTextArea;

/**
 * Controller for the L2ClientDat JavaFX layout.
 */
public class L2ClientDatController
{
	private L2ClientDat _application;
	
	@FXML
	private ComboBox<String> jComboBoxChronicle;
	@FXML
	private ComboBox<String> jComboBoxEncrypt;
	@FXML
	private ComboBox<String> jComboBoxFormatter;
	@FXML
	private Button saveTxtButton;
	@FXML
	private Button saveDatButton;
	@FXML
	private Button abortTaskButton;
	@FXML
	private Button openButton;
	@FXML
	private Button massTxtUnpackButton;
	@FXML
	private Button massTxtPackButton;
	@FXML
	private Button massRecryptButton;
	@FXML
	private ProgressBar progressBar;
	@FXML
	private LineNumberingTextArea lineNumberingTextArea;
	@FXML
	private JPopupTextArea textPaneLog;
	@FXML
	private HBox editorPane;
	@FXML
	private HBox buttonPane2;
	@FXML
	private HBox buttonPane3;
	@FXML
	private HBox progressPane;
	@FXML
	private SplitPane splitPane;
	
	/**
	 * Assigns the main application so handlers can delegate back to it.
	 * @param application the application instance
	 */
	public void setApplication(L2ClientDat application)
	{
		_application = application;
	}
	
	@FXML
	private void openSelectFileWindow(ActionEvent event)
	{
		if (_application != null)
		{
			_application.openSelectFileWindow(event);
		}
	}
	
	@FXML
	private void saveTxtActionPerformed(ActionEvent event)
	{
		if (_application != null)
		{
			_application.saveTxtActionPerformed(event);
		}
	}
	
	@FXML
	private void saveDatActionPerformed(ActionEvent event)
	{
		if (_application != null)
		{
			_application.saveDatActionPerformed(event);
		}
	}
	
	@FXML
	private void massTxtUnpackActionPerformed(ActionEvent event)
	{
		if (_application != null)
		{
			_application.massTxtUnpackActionPerformed(event);
		}
	}
	
	@FXML
	private void massTxtPackActionPerformed(ActionEvent event)
	{
		if (_application != null)
		{
			_application.massTxtPackActionPerformed(event);
		}
	}
	
	@FXML
	private void massRecryptActionPerformed(ActionEvent event)
	{
		if (_application != null)
		{
			_application.massRecryptActionPerformed(event);
		}
	}
	
	@FXML
	private void abortActionPerformed(ActionEvent event)
	{
		if (_application != null)
		{
			_application.abortActionPerformed(event);
		}
	}
	
	ComboBox<String> getChronicleComboBox()
	{
		return jComboBoxChronicle;
	}
	
	ComboBox<String> getEncryptComboBox()
	{
		return jComboBoxEncrypt;
	}
	
	ComboBox<String> getFormatterComboBox()
	{
		return jComboBoxFormatter;
	}
	
	Button getSaveTxtButton()
	{
		return saveTxtButton;
	}
	
	Button getSaveDatButton()
	{
		return saveDatButton;
	}
	
	Button getAbortTaskButton()
	{
		return abortTaskButton;
	}
	
	ProgressBar getProgressBar()
	{
		return progressBar;
	}
	
	LineNumberingTextArea getLineNumberingTextArea()
	{
		return lineNumberingTextArea;
	}
	
	JPopupTextArea getTextPaneLog()
	{
		return textPaneLog;
	}

	HBox getEditorPane()
	{
		return editorPane;
	}
	
	HBox getButtonPane2()
	{
		return buttonPane2;
	}
	
	HBox getButtonPane3()
	{
		return buttonPane3;
	}
	
	HBox getProgressPane()
	{
		return progressPane;
	}
	
	SplitPane getSplitPane()
	{
		return splitPane;
	}
}

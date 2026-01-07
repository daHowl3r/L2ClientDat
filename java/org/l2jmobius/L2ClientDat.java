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

import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.l2jmobius.actions.ActionTask;
import org.l2jmobius.actions.MassRecryptor;
import org.l2jmobius.actions.MassTxtPacker;
import org.l2jmobius.actions.MassTxtUnpacker;
import org.l2jmobius.actions.OpenDat;
import org.l2jmobius.actions.SaveDat;
import org.l2jmobius.actions.SaveTxt;
import org.l2jmobius.clientcryptor.crypt.DatCrypter;
import org.l2jmobius.config.ConfigDebug;
import org.l2jmobius.config.ConfigWindow;
import org.l2jmobius.forms.JPopupTextArea;
import org.l2jmobius.util.Util;
import org.l2jmobius.xml.CryptVersionParser;
import org.l2jmobius.xml.DescriptorParser;

public class L2ClientDat extends Application
{
	private static final Logger LOGGER = Logger.getLogger(L2ClientDat.class.getName());
	
	private static final String DAT_STRUCTURE_STR = "Structures";
	public static final String ENABLED_STR = "Enabled";
	public static final String DISABLED_STR = "Disabled";
	private static final String OPEN_BTN_STR = "Open";
	private static final String SAVE_TXT_BTN_STR = "Save (TXT)";
	private static final String SAVE_DAT_BTN_STR = "Save (DAT)";
	private static final String DECRYPT_ALL_BTN_STR = "Unpack all";
	private static final String ENCRYPT_ALL_BTN_STR = "Pack all";
	private static final String PATCH_ALL_BTN_STR = "Patch all";
	private static final String SELECT_BTN_STR = "Select";
	private static final String FILE_SELECT_BTN_STR = "File select";
	private static final String ABORT_BTN_STR = "Abort";
	private static final String SOURCE_ENCRYPT_TYPE_STR = "Source";
	public static final String DETECT_STR = "Detect";
	public static final String NO_TRANSLATE_STR = "No translate";
	
	public static boolean DEV_MODE = false;
	
	private static TextArea _textPaneLog;
	private final ExecutorService _executorService = Executors.newCachedThreadPool();
	private final JPopupTextArea _textPaneMain = new JPopupTextArea();
	private final LineNumberingTextArea _lineNumberingTextArea = new LineNumberingTextArea();
	private final ComboBox<String> _jComboBoxChronicle = new ComboBox<>();
	private final ComboBox<String> _jComboBoxEncrypt = new ComboBox<>();
	private final ComboBox<String> _jComboBoxFormatter = new ComboBox<>();
	private final ArrayList<Pane> _actionPanels = new ArrayList<>();
	private final Button _saveTxtButton = new Button();
	private final Button _saveDatButton = new Button();
	private final Button _abortTaskButton = new Button();
	private final ProgressBar _progressBar = new ProgressBar(0);
	private Scene _scene;
	
	private File _currentFileWindow = null;
	private ActionTask _progressTask = null;
	
	public static void main(String[] args)
	{
		final File logFolder = new File(".", "log");
		logFolder.mkdir();
		
		try (InputStream is = new FileInputStream(new File("./config/log.cfg")))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, null, e);
		}
		
		DEV_MODE = Util.contains((Object[]) args, (Object) "-dev");
		
		ConfigWindow.load();
		ConfigDebug.load();
		
		CryptVersionParser.getInstance().parse();
		DescriptorParser.getInstance().parse();
		
		launch(args);
	}
	
	@Override
	public void start(Stage stage)
	{
		stage.setTitle("L2ClientDat Editor - L2jMobius Edition");
		stage.setMinWidth(1000);
		stage.setMinHeight(600);
		stage.setWidth(ConfigWindow.WINDOW_WIDTH);
		stage.setHeight(ConfigWindow.WINDOW_HEIGHT);
		stage.setOnCloseRequest(event ->
		{
			ConfigWindow.save("WINDOW_HEIGHT", String.valueOf(stage.getHeight()));
			ConfigWindow.save("WINDOW_WIDTH", String.valueOf(stage.getWidth()));
			_executorService.shutdownNow();
			Platform.exit();
		});
		
		final VBox buttonPane = new VBox(8.0);
		buttonPane.setPadding(new Insets(8.0));
		
		final HBox buttonPane2 = new HBox(8.0);
		final Label structureLabel = new Label(DAT_STRUCTURE_STR);
		buttonPane2.getChildren().add(structureLabel);
		final String[] chronicles = DescriptorParser.getInstance().getChronicleNames().toArray(new String[0]);
		_jComboBoxChronicle.setItems(FXCollections.observableArrayList(chronicles));
		_jComboBoxChronicle.setValue(Util.contains((Object[]) chronicles, (Object) ConfigWindow.CURRENT_CHRONICLE) ? ConfigWindow.CURRENT_CHRONICLE : chronicles[chronicles.length - 1]);
		_jComboBoxChronicle.valueProperty().addListener((observable, oldValue, newValue) -> saveComboBox(_jComboBoxChronicle, "CURRENT_CHRONICLE"));
		buttonPane2.getChildren().add(_jComboBoxChronicle);
		
		final Label encryptLabel = new Label("Encrypt:");
		buttonPane2.getChildren().add(encryptLabel);
		final List<String> encryptChoices = new ArrayList<>(CryptVersionParser.getInstance().getEncryptKey().keySet());
		encryptChoices.add(0, SOURCE_ENCRYPT_TYPE_STR);
		_jComboBoxEncrypt.setItems(FXCollections.observableArrayList(encryptChoices));
		_jComboBoxEncrypt.setValue(ConfigWindow.CURRENT_ENCRYPT);
		_jComboBoxEncrypt.valueProperty().addListener((observable, oldValue, newValue) -> saveComboBox(_jComboBoxEncrypt, "CURRENT_ENCRYPT"));
		buttonPane2.getChildren().add(_jComboBoxEncrypt);
		
		final Label inputFormatterLabel = new Label("Formatter:");
		buttonPane2.getChildren().add(inputFormatterLabel);
		_jComboBoxFormatter.setItems(FXCollections.observableArrayList(ENABLED_STR, DISABLED_STR));
		_jComboBoxFormatter.setValue(ConfigWindow.CURRENT_FORMATTER);
		_jComboBoxFormatter.valueProperty().addListener((observable, oldValue, newValue) -> saveComboBox(_jComboBoxFormatter, "CURRENT_FORMATTER"));
		buttonPane2.getChildren().add(_jComboBoxFormatter);
		
		buttonPane.getChildren().add(buttonPane2);
		_actionPanels.add(buttonPane2);
		
		final HBox buttonPane3 = new HBox(8.0);
		final Button open = new Button(OPEN_BTN_STR);
		open.setOnAction(this::openSelectFileWindow);
		buttonPane3.getChildren().add(open);
		_saveTxtButton.setText(SAVE_TXT_BTN_STR);
		_saveTxtButton.setOnAction(this::saveTxtActionPerformed);
		_saveTxtButton.setDisable(true);
		buttonPane3.getChildren().add(_saveTxtButton);
		_saveDatButton.setText(SAVE_DAT_BTN_STR);
		_saveDatButton.setOnAction(this::saveDatActionPerformed);
		_saveDatButton.setDisable(true);
		buttonPane3.getChildren().add(_saveDatButton);
		final Button massTxtUnpack = new Button(DECRYPT_ALL_BTN_STR);
		massTxtUnpack.setOnAction(this::massTxtUnpackActionPerformed);
		buttonPane3.getChildren().add(massTxtUnpack);
		final Button massTxtPack = new Button(ENCRYPT_ALL_BTN_STR);
		massTxtPack.setOnAction(this::massTxtPackActionPerformed);
		buttonPane3.getChildren().add(massTxtPack);
		final Button massRecrypt = new Button(PATCH_ALL_BTN_STR);
		massRecrypt.setOnAction(this::massRecryptActionPerformed);
		buttonPane3.getChildren().add(massRecrypt);
		buttonPane.getChildren().add(buttonPane3);
		_actionPanels.add(buttonPane3);
		
		final HBox progressPane = new HBox(8.0);
		_progressBar.setPrefWidth(300.0);
		_progressBar.setProgress(0.0);
		progressPane.getChildren().add(_progressBar);
		
		_abortTaskButton.setText(ABORT_BTN_STR);
		_abortTaskButton.setOnAction(this::abortActionPerformed);
		_abortTaskButton.setDisable(true);
		progressPane.getChildren().add(_abortTaskButton);
		buttonPane.getChildren().add(progressPane);
		_actionPanels.add(progressPane);
		
		final SplitPane splitPane = new SplitPane();
		splitPane.setOrientation(Orientation.VERTICAL);
		splitPane.setDividerPositions(0.7);
		final Font font = Font.font(new Label().getFont().getName(), FontWeight.BOLD, 13.0);
		_textPaneMain.setStyle("-fx-control-inner-background: #293134; -fx-text-fill: white;");
		_textPaneMain.setFont(font);
		_textPaneMain.textProperty().addListener((observable, oldValue, newValue) -> _lineNumberingTextArea.updateText(newValue));
		_lineNumberingTextArea.updateText(_textPaneMain.getText());
		
		_lineNumberingTextArea.setFont(Font.font(font.getName(), FontWeight.NORMAL, 12.0));
		_lineNumberingTextArea.setStyle("-fx-control-inner-background: #4d4d4d; -fx-text-fill: #d3d3d3;");
		_lineNumberingTextArea.setEditable(false);
		_lineNumberingTextArea.setFocusTraversable(false);
		_lineNumberingTextArea.setMouseTransparent(true);
		_lineNumberingTextArea.setPrefWidth(50.0);
		
		final HBox editorPane = new HBox();
		HBox.setHgrow(_textPaneMain, Priority.ALWAYS);
		editorPane.getChildren().addAll(_lineNumberingTextArea, _textPaneMain);
		
		_textPaneLog = new JPopupTextArea();
		_textPaneLog.setStyle("-fx-control-inner-background: #293134; -fx-text-fill: cyan;");
		_textPaneLog.setEditable(false);
		
		splitPane.getItems().addAll(editorPane, _textPaneLog);
		
		final BorderPane root = new BorderPane();
		root.setTop(buttonPane);
		root.setCenter(splitPane);
		
		_scene = new Scene(root);
		stage.setScene(_scene);
		
		setIcons(stage);
		stage.show();
		
		Platform.runLater(() -> syncScrollBars(_textPaneMain, _lineNumberingTextArea));
	}
	
	@Override
	public void stop()
	{
		_executorService.shutdownNow();
	}
	
	public JPopupTextArea getTextPaneMain()
	{
		return _textPaneMain;
	}
	
	public static void addLogConsole(String log, boolean isLog)
	{
		if (isLog)
		{
			LOGGER.info(log);
		}
		
		if (_textPaneLog == null)
		{
			return;
		}
		
		Platform.runLater(() -> _textPaneLog.appendText(log + System.lineSeparator()));
	}
	
	public void setEditorText(String text)
	{
		_lineNumberingTextArea.cleanUp();
		Platform.runLater(() -> _textPaneMain.setText(text));
	}
	
	private void massTxtPackActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
		{
			return;
		}
		
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		final File packDirectory = getExistingDirectory(ConfigWindow.FILE_OPEN_CURRENT_DIRECTORY_PACK);
		if (packDirectory != null)
		{
			directoryChooser.setInitialDirectory(packDirectory);
		}
		final File selectedDirectory = directoryChooser.showDialog(_scene.getWindow());
		if (selectedDirectory != null)
		{
			_currentFileWindow = selectedDirectory;
			ConfigWindow.save("FILE_OPEN_CURRENT_DIRECTORY_PACK", _currentFileWindow.getPath());
			addLogConsole("---------------------------------------", true);
			addLogConsole("Selected folder: " + _currentFileWindow.getPath(), true);
			_progressTask = new MassTxtPacker(this, String.valueOf(_jComboBoxChronicle.getValue()), _currentFileWindow.getPath());
			_executorService.execute(_progressTask);
		}
	}
	
	private void massTxtUnpackActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
		{
			return;
		}
		
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		final File unpackDirectory = getExistingDirectory(ConfigWindow.FILE_OPEN_CURRENT_DIRECTORY_UNPACK);
		if (unpackDirectory != null)
		{
			directoryChooser.setInitialDirectory(unpackDirectory);
		}
		final File selectedDirectory = directoryChooser.showDialog(_scene.getWindow());
		if (selectedDirectory != null)
		{
			_currentFileWindow = selectedDirectory;
			ConfigWindow.save("FILE_OPEN_CURRENT_DIRECTORY_UNPACK", _currentFileWindow.getPath());
			addLogConsole("---------------------------------------", true);
			addLogConsole("selected folder: " + _currentFileWindow.getPath(), true);
			_progressTask = new MassTxtUnpacker(this, String.valueOf(_jComboBoxChronicle.getValue()), _currentFileWindow.getPath());
			_executorService.execute(_progressTask);
		}
	}
	
	private void massRecryptActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
		{
			return;
		}
		
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		final File recryptDirectory = getExistingDirectory(ConfigWindow.FILE_OPEN_CURRENT_DIRECTORY);
		if (recryptDirectory != null)
		{
			directoryChooser.setInitialDirectory(recryptDirectory);
		}
		final File selectedDirectory = directoryChooser.showDialog(_scene.getWindow());
		if (selectedDirectory != null)
		{
			_currentFileWindow = selectedDirectory;
			ConfigWindow.save("FILE_OPEN_CURRENT_DIRECTORY", _currentFileWindow.getPath());
			addLogConsole("---------------------------------------", true);
			addLogConsole("Selected folder: " + _currentFileWindow.getPath(), true);
			_progressTask = new MassRecryptor(this, _currentFileWindow.getPath());
			_executorService.execute(_progressTask);
		}
	}
	
	private void openSelectFileWindow(ActionEvent evt)
	{
		if (_progressTask != null)
		{
			return;
		}
		
		final FileChooser fileChooser = new FileChooser();
		final File lastSelected = new File(ConfigWindow.LAST_FILE_SELECTED);
		final File initialDirectory = lastSelected.exists() ? lastSelected.getParentFile() : null;
		if (initialDirectory != null)
		{
			fileChooser.setInitialDirectory(initialDirectory);
		}
		fileChooser.getExtensionFilters().addAll(
			new FileChooser.ExtensionFilter(".dat", "*.dat"),
			new FileChooser.ExtensionFilter(".ini", "*.ini"),
			new FileChooser.ExtensionFilter(".txt", "*.txt"),
			new FileChooser.ExtensionFilter(".htm", "*.htm"),
			new FileChooser.ExtensionFilter(".dat, .ini, .txt, .htm", "*.dat", "*.ini", "*.txt", "*.htm")
		);
		final File selectedFile = fileChooser.showOpenDialog(_scene.getWindow());
		if (selectedFile != null)
		{
			_currentFileWindow = selectedFile;
			ConfigWindow.save("LAST_FILE_SELECTED", _currentFileWindow.getAbsolutePath());
			addLogConsole("---------------------------------------", true);
			addLogConsole("Open file: " + _currentFileWindow.getName(), true);
			_progressTask = new OpenDat(this, String.valueOf(_jComboBoxChronicle.getValue()), _currentFileWindow);
			_executorService.execute(_progressTask);
		}
	}
	
	private void saveTxtActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
		{
			return;
		}
		
		final FileChooser fileSave = new FileChooser();
		final File saveDirectory = getExistingDirectory(ConfigWindow.FILE_SAVE_CURRENT_DIRECTORY);
		if (saveDirectory != null)
		{
			fileSave.setInitialDirectory(saveDirectory);
		}
		if (_currentFileWindow != null)
		{
			fileSave.setInitialFileName(_currentFileWindow.getName().split("\\.")[0] + ".txt");
			fileSave.getExtensionFilters().add(new FileChooser.ExtensionFilter(".txt", "*.txt"));
			final File selectedFile = fileSave.showSaveDialog(_scene.getWindow());
			if (selectedFile != null)
			{
				_progressTask = new SaveTxt(this, selectedFile);
				_executorService.execute(_progressTask);
			}
		}
		else
		{
			addLogConsole("No open file!", true);
		}
	}
	
	private void saveDatActionPerformed(ActionEvent evt)
	{
		if (_progressTask != null)
		{
			return;
		}
		
		if (_currentFileWindow != null)
		{
			_progressTask = new SaveDat(this, _currentFileWindow, String.valueOf(_jComboBoxChronicle.getValue()));
			_executorService.execute(_progressTask);
		}
		else
		{
			addLogConsole("Error saving dat. No file name.", true);
		}
	}
	
	private void abortActionPerformed(ActionEvent evt)
	{
		if (_progressTask == null)
		{
			return;
		}
		
		_progressTask.abort();
		addLogConsole("---------------------------------------", true);
		addLogConsole("Progress aborted.", true);
	}
	
	public DatCrypter getEncryptor(File file)
	{
		DatCrypter crypter = null;
		final String encryptorName = ConfigWindow.CURRENT_ENCRYPT;
		if (encryptorName.equalsIgnoreCase(".") || encryptorName.equalsIgnoreCase(SOURCE_ENCRYPT_TYPE_STR) || encryptorName.trim().isEmpty())
		{
			final DatCrypter lastDatDecryptor = OpenDat.getLastDatCrypter(file);
			if (lastDatDecryptor != null)
			{
				crypter = CryptVersionParser.getInstance().getEncryptKey(lastDatDecryptor.getName());
				if (crypter == null)
				{
					addLogConsole("Not found " + lastDatDecryptor.getName() + " encryptor of the file: " + _currentFileWindow.getName(), true);
				}
			}
		}
		else
		{
			crypter = CryptVersionParser.getInstance().getEncryptKey(encryptorName);
			if (crypter == null)
			{
				addLogConsole("Not found " + encryptorName + " encryptor of the file: " + _currentFileWindow.getName(), true);
			}
		}
		return crypter;
	}
	
	private void saveComboBox(ComboBox<String> comboBox, String param)
	{
		ConfigWindow.save(param, String.valueOf(comboBox.getValue()));
	}
	
	public void onStartTask()
	{
		if (_scene != null)
		{
			_scene.setCursor(Cursor.WAIT);
		}
		_progressBar.setProgress(0.0);
		checkButtons();
	}
	
	public void onProgressTask(int val)
	{
		_progressBar.setProgress(val / 100.0);
	}
	
	public void onStopTask()
	{
		_progressTask = null;
		_progressBar.setProgress(1.0);
		checkButtons();
		Toolkit.getDefaultToolkit().beep();
		if (_scene != null)
		{
			_scene.setCursor(Cursor.DEFAULT);
		}
	}
	
	public void onAbortTask()
	{
		if (_progressTask == null)
		{
			return;
		}
		
		_progressTask = null;
		if (_scene != null)
		{
			_scene.setCursor(Cursor.DEFAULT);
		}
		checkButtons();
	}
	
	private void checkButtons()
	{
		if (_progressTask != null)
		{
			_actionPanels.forEach(p -> p.getChildren().forEach(child -> child.setDisable(true)));
			_abortTaskButton.setDisable(false);
		}
		else
		{
			_actionPanels.forEach(p -> p.getChildren().forEach(child ->
			{
				if (child == _saveTxtButton)
				{
					child.setDisable(_currentFileWindow == null);
				}
				else if (child == _saveDatButton)
				{
					child.setDisable(_currentFileWindow == null);
				}
				else
				{
					child.setDisable(false);
				}
			}));
			
			_abortTaskButton.setDisable(true);
		}
	}
	
	private File getExistingDirectory(String path)
	{
		if (path == null)
		{
			return null;
		}
		final File directory = new File(path);
		return directory.isDirectory() ? directory : null;
	}
	
	private void setIcons(Stage stage)
	{
		final List<Image> icons = new ArrayList<>();
		icons.add(loadIcon("l2jmobius_16x16.png"));
		icons.add(loadIcon("l2jmobius_32x32.png"));
		icons.add(loadIcon("l2jmobius_64x64.png"));
		icons.add(loadIcon("l2jmobius_128x128.png"));
		icons.removeIf(icon -> icon == null);
		stage.getIcons().addAll(icons);
	}
	
	private Image loadIcon(String fileName)
	{
		final File file = new File("." + File.separator + "images" + File.separator + fileName);
		if (!file.exists())
		{
			return null;
		}
		return new Image(file.toURI().toString());
	}
	
	private void syncScrollBars(TextArea main, TextArea lineNumbers)
	{
		final ScrollBar mainBar = (ScrollBar) main.lookup(".scroll-bar:vertical");
		final ScrollBar lineBar = (ScrollBar) lineNumbers.lookup(".scroll-bar:vertical");
		if ((mainBar == null) || (lineBar == null))
		{
			return;
		}
		lineBar.valueProperty().bindBidirectional(mainBar.valueProperty());
	}
	
	private static class LineNumberingTextArea extends TextArea
	{
		private int lastLines = 0;
		
		public void cleanUp()
		{
			setText("");
			lastLines = 0;
		}
		
		private void updateText(String text)
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
}

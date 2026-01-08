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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.Cursor;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
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
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.l2jmobius.forms.EditorCodeArea;
import org.l2jmobius.forms.JPopupTextArea;
import org.l2jmobius.util.Util;
import org.l2jmobius.xml.CryptVersionParser;
import org.l2jmobius.xml.Descriptor;
import org.l2jmobius.xml.DescriptorParser;
import org.l2jmobius.xml.DescriptorWriter;

public class L2ClientDat extends Application
{
	private static final Logger LOGGER = Logger.getLogger(L2ClientDat.class.getName());
	
	public static final String ENABLED_STR = "Enabled";
	public static final String DISABLED_STR = "Disabled";
	private static final String SOURCE_ENCRYPT_TYPE_STR = "Source";
	public static final String DETECT_STR = "Detect";
	public static final String NO_TRANSLATE_STR = "No translate";
	
	public static boolean DEV_MODE = false;
	
	private static JPopupTextArea _textPaneLog;
	private final ExecutorService _executorService = Executors.newCachedThreadPool();
	private final ArrayList<Pane> _actionPanels = new ArrayList<>();
	private EditorCodeArea _textPaneMain;
	private VirtualizedScrollPane<EditorCodeArea> _editorScrollPane;
	private LineNumberingTextArea _lineNumberingTextArea;
	private ComboBox<String> _jComboBoxChronicle;
	private ComboBox<String> _jComboBoxEncrypt;
	private ComboBox<String> _jComboBoxFormatter;
	private Button _saveTxtButton;
	private Button _saveDatButton;
	private Button _abortTaskButton;
	private ProgressBar _progressBar;
	private Scene _scene;
	private PauseTransition _validationDebounce;
	private final AtomicInteger _validationSequence = new AtomicInteger();
	private String _lastValidationMessage = null;
	private boolean _isValidationValid = true;
	
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
		
		final Parent root;
		try
		{
			final FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/l2jmobius/L2ClientDat.fxml"));
			root = loader.load();
			final L2ClientDatController controller = loader.getController();
			controller.setApplication(this);
			bindController(controller);
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, "Unable to load UI layout.", e);
			Platform.exit();
			return;
		}
		
		configureUi();
		
		_scene = new Scene(root);
		_scene.getStylesheets().add(getClass().getResource("/org/l2jmobius/editor.css").toExternalForm());
		stage.setScene(_scene);
		
		setIcons(stage);
		stage.show();
		
		Platform.runLater(() -> syncScrollBars(_editorScrollPane, _lineNumberingTextArea));
	}
	
	@Override
	public void stop()
	{
		_executorService.shutdownNow();
	}
	
	public EditorCodeArea getTextPaneMain()
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
		Platform.runLater(() -> _textPaneMain.replaceText(text));
	}
	
	void massTxtPackActionPerformed(ActionEvent evt)
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
	
	void massTxtUnpackActionPerformed(ActionEvent evt)
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
	
	void massRecryptActionPerformed(ActionEvent evt)
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
	
	void openSelectFileWindow(ActionEvent evt)
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
	
	void saveTxtActionPerformed(ActionEvent evt)
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
	
	void saveDatActionPerformed(ActionEvent evt)
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
	
	void abortActionPerformed(ActionEvent evt)
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
					child.setDisable((_currentFileWindow == null) || !_isValidationValid);
				}
				else if (child == _saveDatButton)
				{
					child.setDisable((_currentFileWindow == null) || !_isValidationValid);
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
	
	private void syncScrollBars(VirtualizedScrollPane<EditorCodeArea> main, TextArea lineNumbers)
	{
		final ScrollBar mainBar = (ScrollBar) main.lookup(".scroll-bar:vertical");
		final ScrollBar lineBar = (ScrollBar) lineNumbers.lookup(".scroll-bar:vertical");
		if ((mainBar == null) || (lineBar == null))
		{
			return;
		}
		lineBar.valueProperty().bindBidirectional(mainBar.valueProperty());
	}

	private void scheduleValidation(String text)
	{
		if (_validationDebounce == null)
		{
			return;
		}
		
		final File currentFile = _currentFileWindow;
		final String chronicle = String.valueOf(_jComboBoxChronicle.getValue());
		final int sequence = _validationSequence.incrementAndGet();
		_validationDebounce.stop();
		_validationDebounce.setOnFinished(event -> runValidationAsync(text, sequence, currentFile, chronicle));
		_validationDebounce.playFromStart();
	}
	
	private void runValidationAsync(String text, int sequence, File currentFile, String chronicle)
	{
		if ((currentFile == null) || (chronicle == null))
		{
			Platform.runLater(() -> applyValidationResult(null, text));
			clearValidationMessage();
			return;
		}
		
		_executorService.execute(() ->
		{
			if (sequence != _validationSequence.get())
			{
				return;
			}
			
			final String fileName = resolveDescriptorFileName(currentFile.getName());
			final Descriptor desc = DescriptorParser.getInstance().findDescriptorForFile(chronicle, fileName);
			if (desc == null)
			{
				Platform.runLater(() -> applyValidationResult(null, text));
				clearValidationMessage();
				return;
			}
			
			final DatCrypter crypter = getEncryptor(currentFile);
			final DescriptorWriter.ValidationResult result;
			try
			{
				result = DescriptorWriter.validateData(new ValidationActionTask(this), 100.0, currentFile, crypter, desc, text, true);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Validation failed to complete.", e);
				return;
			}
			
			if (sequence != _validationSequence.get())
			{
				return;
			}
			
			if (result.isValid())
			{
				clearValidationMessage();
			}
			else
			{
				logValidationError(result);
			}
			
			Platform.runLater(() -> applyValidationResult(result, text));
		});
	}
	
	private String resolveDescriptorFileName(String fileName)
	{
		if (fileName.endsWith(".txt"))
		{
			return fileName.replace(".txt", ".dat");
		}
		return fileName;
	}
	
	private void logValidationError(DescriptorWriter.ValidationResult result)
	{
		final String message = formatValidationMessage(result);
		if ((message == null) || message.equals(_lastValidationMessage))
		{
			return;
		}
		_lastValidationMessage = message;
		addLogConsole(message, true);
	}
	
	private void clearValidationMessage()
	{
		if (_lastValidationMessage != null)
		{
			_lastValidationMessage = null;
			addLogConsole("Validation passed.", true);
		}
	}
	
	private void applyValidationResult(DescriptorWriter.ValidationResult result, String text)
	{
		if ((result == null) || result.isValid())
		{
			setValidationState(true, Collections.emptyList());
			return;
		}
		
		setValidationState(false, buildErrorRanges(result, text));
	}
	
	private void setValidationState(boolean isValid, List<IndexRange> ranges)
	{
		_isValidationValid = isValid;
		_textPaneMain.setErrorRanges(ranges);
		checkButtons();
	}
	
	private List<IndexRange> buildErrorRanges(DescriptorWriter.ValidationResult result, String text)
	{
		if ((result == null) || (text == null))
		{
			return Collections.emptyList();
		}
		
		final int offset = result.getOffset();
		if (offset < 1)
		{
			return Collections.emptyList();
		}
		
		final int startIndex = Math.min(offset - 1, text.length());
		int endIndex = text.length();
		for (int i = startIndex; i < text.length(); i++)
		{
			final char ch = text.charAt(i);
			if ((ch == '\r') || (ch == '\n'))
			{
				endIndex = i;
				break;
			}
		}
		if (endIndex <= startIndex)
		{
			return Collections.emptyList();
		}
		return Collections.singletonList(new IndexRange(startIndex, endIndex));
	}
	
	private String formatValidationMessage(DescriptorWriter.ValidationResult result)
	{
		if (result == null)
		{
			return null;
		}
		
		final StringBuilder builder = new StringBuilder("Validation error");
		if (result.getLine() > 0)
		{
			builder.append(" at line ").append(result.getLine());
		}
		if (result.getOffset() > 0)
		{
			builder.append(" (offset ").append(result.getOffset()).append(")");
		}
		if (result.getMessage() != null)
		{
			builder.append(": ").append(result.getMessage());
		}
		return builder.toString();
	}
	
	private void bindController(L2ClientDatController controller)
	{
		_lineNumberingTextArea = controller.getLineNumberingTextArea();
		_textPaneMain = new EditorCodeArea();
		_editorScrollPane = new VirtualizedScrollPane<>(_textPaneMain);
		final HBox editorPane = controller.getEditorPane();
		editorPane.getChildren().setAll(_lineNumberingTextArea, _editorScrollPane);
		HBox.setHgrow(_editorScrollPane, Priority.ALWAYS);
		_jComboBoxChronicle = controller.getChronicleComboBox();
		_jComboBoxEncrypt = controller.getEncryptComboBox();
		_jComboBoxFormatter = controller.getFormatterComboBox();
		_saveTxtButton = controller.getSaveTxtButton();
		_saveDatButton = controller.getSaveDatButton();
		_abortTaskButton = controller.getAbortTaskButton();
		_progressBar = controller.getProgressBar();
		_textPaneLog = controller.getTextPaneLog();
		_actionPanels.clear();
		_actionPanels.add(controller.getButtonPane2());
		_actionPanels.add(controller.getButtonPane3());
		_actionPanels.add(controller.getProgressPane());
	}
	
	private void configureUi()
	{
		final String[] chronicles = DescriptorParser.getInstance().getChronicleNames().toArray(new String[0]);
		_jComboBoxChronicle.setItems(FXCollections.observableArrayList(chronicles));
		_jComboBoxChronicle.setValue(Util.contains((Object[]) chronicles, (Object) ConfigWindow.CURRENT_CHRONICLE) ? ConfigWindow.CURRENT_CHRONICLE : chronicles[chronicles.length - 1]);
		_jComboBoxChronicle.valueProperty().addListener((observable, oldValue, newValue) -> saveComboBox(_jComboBoxChronicle, "CURRENT_CHRONICLE"));
		
		final List<String> encryptChoices = new ArrayList<>(CryptVersionParser.getInstance().getEncryptKey().keySet());
		encryptChoices.add(0, SOURCE_ENCRYPT_TYPE_STR);
		_jComboBoxEncrypt.setItems(FXCollections.observableArrayList(encryptChoices));
		_jComboBoxEncrypt.setValue(ConfigWindow.CURRENT_ENCRYPT);
		_jComboBoxEncrypt.valueProperty().addListener((observable, oldValue, newValue) -> saveComboBox(_jComboBoxEncrypt, "CURRENT_ENCRYPT"));
		
		_jComboBoxFormatter.setItems(FXCollections.observableArrayList(ENABLED_STR, DISABLED_STR));
		_jComboBoxFormatter.setValue(ConfigWindow.CURRENT_FORMATTER);
		_jComboBoxFormatter.valueProperty().addListener((observable, oldValue, newValue) -> saveComboBox(_jComboBoxFormatter, "CURRENT_FORMATTER"));
		
		_saveTxtButton.setDisable(true);
		_saveDatButton.setDisable(true);
		_abortTaskButton.setDisable(true);
		_progressBar.setProgress(0.0);
		_validationDebounce = new PauseTransition(Duration.millis(300.0));
		
		final Font font = Font.font(new Label().getFont().getName(), FontWeight.BOLD, 13.0);
		_textPaneMain.setStyle(String.format("-fx-background-color: #293134; -fx-text-fill: white; -fx-font-family: '%s'; -fx-font-size: %.1fpx; -fx-font-weight: bold;",
				font.getFamily(),
				font.getSize()));
		_textPaneMain.textProperty().addListener((observable, oldValue, newValue) ->
		{
			_lineNumberingTextArea.updateText(newValue);
			scheduleValidation(newValue);
		});
		_lineNumberingTextArea.updateText(_textPaneMain.getText());
		
		_lineNumberingTextArea.setFont(Font.font(font.getName(), FontWeight.NORMAL, 12.0));
		_lineNumberingTextArea.setStyle("-fx-control-inner-background: #4d4d4d; -fx-text-fill: #d3d3d3;");
		_lineNumberingTextArea.setEditable(false);
		_lineNumberingTextArea.setFocusTraversable(false);
		_lineNumberingTextArea.setMouseTransparent(true);
		
		_textPaneLog.setStyle("-fx-control-inner-background: #293134; -fx-text-fill: cyan;");
		_textPaneLog.setEditable(false);
	}
	
	private static final class ValidationActionTask extends ActionTask
	{
		ValidationActionTask(L2ClientDat l2clientdat)
		{
			super(l2clientdat);
		}
		
		@Override
		protected void action()
		{
		}
		
		@Override
		public void changeProgress(double value)
		{
		}
	}
}

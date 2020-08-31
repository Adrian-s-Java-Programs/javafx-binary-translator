package javafxbinarytranslator;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue; 
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import java.io.File;

/*
 * This class creates the graphical interface of an application where a user can convert a text to binary and decode it back.
 * The user can specify an input text file, an output text file and choose an operation (encode to binary or decode from binary).
 * This application is developed using JavaFX 8.
 */
public class BinaryTranslator extends Application {

  /* setting default output file name */
  private static final String defaultFileName = "output.txt";

  private String inputFileName;
  private String outputFileName;
  private String operation;

  /* variable used for displaying status messages */
  Label result = new Label();

  /* 
   * TextField was chosen over Label because the text from a TextField can be copied by the user
   * and also because if the text is too long to be displayed on a single line inside the window,
   * the user can scroll through the text. 
   */
  TextField inputFileField = new TextField();  /* used as non-editable field, to display the name of the input file given by the user */
  TextField outputFileField = new TextField(); /* used as non-editable field, to display the name of the output file given by the user */

  /* ComboBox used so that the user can choose between encode and decode operations */
  ComboBox<String> operationType; 

  /* method used to display a status message */
  private void showMessage(String message) {
    result.setText(message);
  }

  /* method used to clear a status message */
  private void clearMessage() {
    result.setText("");
  }

  /* creating event handler for the button used to select the input file */
  private class BrowseInputFileButtonListener implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent e) {
      clearMessage();
      /* creating a FileChooser and adding a filter to set which files can be chosen in the open file dialog - text files, in this case */
      FileChooser fileChooser = new FileChooser();
      FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
      fileChooser.getExtensionFilters().add(extFilter);

      /* open dialog for input file selection */
      File selectedFile = fileChooser.showOpenDialog(null);

      if (selectedFile != null) {
        try{
          /* getting the full file name and storing it to a variable */
          inputFileName = selectedFile.getCanonicalPath();
          /* displaying file name */
          inputFileField.setText(inputFileName);
        }
        catch (Exception ex) {
          showMessage("Error encountered when getting input file name.");
        }
      }
      else {
        showMessage("Input file selection cancelled.");
      }
    }
  }

  /* creating event handler for the button used to select the output file */
  private class BrowseOutputFileButtonListener implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent e) {
      clearMessage();
      /* creating a FileChooser and adding a filter to set which files can be chosen in the save file dialog - text files, in this case */
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Select output file");
      fileChooser.setInitialFileName(defaultFileName);
      FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
      fileChooser.getExtensionFilters().add(extFilter);

      /* open dialog for output file selection */
      File savedFile = fileChooser.showSaveDialog(null);

      if (savedFile != null) {
        try{
          /* getting the full file name and storing it to a variable */
          outputFileName = savedFile.getCanonicalPath();
          /* displaying file name */
          outputFileField.setText(outputFileName);
        }
        catch (Exception ex){
          showMessage("Error encountered when getting output file name.");
        }

      }
      else {
        showMessage("Output file selection cancelled.");
      }

    }

  }

  /* creating event handler for the button used to clear the input form */
  private class ClearButtonListener implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent e) {
      clearMessage();

      operationType.setValue(null); /* this will also set variable 'operation' to null */

      inputFileName=null;
      inputFileField.setText(null);

      outputFileName=null;
      outputFileField.setText(null);
    }
  }

  /* creating event handler for the button used to run the conversion */
  private class ExecuteButtonListener implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent e) {
      String message = "";

      if ((operation != null) & (inputFileName != null) & (outputFileName != null)) {

        /* it means that all the required data was entered */

        /* we need a TextFileConverter, which contains the methods to be invoked for the conversion */
        TextFileConverter tfc = new TextFileConverter();

        /* variable used for retrieving the conversion result */
        String conversionResult = "";

        if (operation.compareTo("Encode")==0){
          /* encoding to binary */
          conversionResult = tfc.encodeToBinary(inputFileName, outputFileName);
          if (conversionResult.compareTo("success")==0){
            showMessage("The text from the input file was successfully encoded to binary.");
          }
          else{
            showMessage(conversionResult);
          }
        }

        else{
          if (operation.compareTo("Decode")==0){
            /* decoding from binary */
            conversionResult = tfc.decodeFromBinary(inputFileName, outputFileName);
            if (conversionResult.compareTo("success")==0){
              showMessage("The text from the input file was successfully decoded from binary.");
            }
            else{
              showMessage(conversionResult);
            }
          }
        }

      }

      else{

        /* constructing a message about missing user input */
        if (inputFileName == null) message = "Please specify the input file."+System.lineSeparator();
        if (outputFileName == null) message += "Please specify the output file."+System.lineSeparator();
        if (operation == null) message += "Please specify the operation to be executed.";

        /* displaying message to the user */
        showMessage(message);

      }

    }

  }

  public static void main(String[] args) {
    Application.launch(args);
  }

  @Override
  public void start(Stage stage) {

    String textFont = "Arial";

    result.setFont(Font.font(textFont, FontWeight.BOLD, 12));

    Label inputMessage = new Label ("Text \u2194 Binary Translator");
    inputMessage.setTextFill(Color.DARKBLUE);
    inputMessage.setFont(Font.font(textFont, FontWeight.BOLD, 30));

    /* we make the TextField look like a Label that will be made visible when needing to display the name of the input file that was chosen by the user */
    inputFileField.setStyle("-fx-background-color:transparent; text-align: 0px; -fx-padding: 0;");
    inputFileField.setEditable(false);

    /* we make the TextField look like a Label that will be made visible when needing to display the name of the output file that was chosen by the user */
    outputFileField.setStyle("-fx-background-color:transparent; text-align: 0px; -fx-padding: 0;");
    outputFileField.setEditable(false);

    /* creating a Browse button for the input file */
    Button browseInputFileButton = new Button("Select input file");
    /* adding EventHandler to the button */
    browseInputFileButton.setOnAction(new BrowseInputFileButtonListener());

    /* creating a Browse button for the output file */
    Button browseOutputFileButton = new Button("Select output file");
    /* adding EventHandler to the button */
    browseOutputFileButton.setOnAction(new BrowseOutputFileButtonListener());

    /* creating a GridPane */
    GridPane gPane = new GridPane();
    gPane.setHgap(5);
    gPane.setVgap(5);
    gPane.addRow(1, browseInputFileButton, inputFileField);
    gPane.addRow(2, browseOutputFileButton, outputFileField);
    /* 
     * we want the TextFields next to the browse buttons to adjust their size if the file names written inside them 
     * are too long and the user wants to enlarge the window to see the entire file names
     */
    GridPane.setHgrow(inputFileField, Priority.SOMETIMES);
    GridPane.setHgrow(outputFileField, Priority.SOMETIMES);

    /* creating a ComboBox for selecting the operation type */
    operationType = new ComboBox<>();
    /* adding items to the ComboBox */
    operationType.getItems().addAll("Encode", "Decode");
    operationType.setPromptText("Operation type");
    /* adding a ChangeListener to the ComboBox */
    operationType.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() { 
      public void changed(ObservableValue<? extends String> ov, final String oldValue, final String newValue) {
        clearMessage();
        operation = newValue;
      }
    });

    /* creating a button that will execute the conversion if all the conditions are met */
    Button executeButton = new Button("Execute");
    /* adding EventHandler to the button */
    executeButton.setOnAction(new ExecuteButtonListener());

    /* creating a Clear button */
    Button clearButton = new Button("Clear");
    /* adding EventHandler to the button */
    clearButton.setOnAction(new ClearButtonListener());

    /* creating an HBox */
    HBox buttonBox = new HBox();
    /* adding children to the HBox */
    buttonBox.getChildren().addAll(executeButton, clearButton);
    /* setting the horizontal spacing between children to 5px */
    buttonBox.setSpacing(5);

    /* creating a VBox */
    VBox root = new VBox();
    /* adding the children to the VBox */
    root.getChildren().addAll(inputMessage, gPane, operationType, buttonBox, result);
    /* setting the vertical spacing between children to 5px */
    root.setSpacing(5);
    /* setting the minimum Size of the VBox */
    root.setMinSize(700, 300);

    /* setting the style for the VBox */
    root.setStyle("-fx-padding: 10;"
                + "-fx-border-width: 2;"
                + "-fx-border-insets: 5;"
                + "-fx-border-radius: 5;"
                + "-fx-border-color: #1E90FF;");

    /* creating the Scene */
    Scene scene = new Scene(root);

    /* adding the scene to the Stage */
    stage.setScene(scene);

    /* setting the title of the Stage */
    stage.setTitle("Binary Translator");

    /* showing the Stage */
    stage.show();

  }


}

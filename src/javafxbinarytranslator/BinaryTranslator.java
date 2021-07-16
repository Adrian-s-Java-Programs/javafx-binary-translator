package javafxbinarytranslator;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
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
import java.util.Optional;

/*
 * This class creates the graphical interface of an application where a user can convert a text to binary and decode it back.
 * The user can specify an input text file, an output text file and choose an operation (encode to binary or decode from binary).
 * This application is developed using JavaFX 8.
 */
public class BinaryTranslator extends Application {

  private Stage mainStage;

  /* setting default output file name */
  private static final String defaultFileName = "output.txt";

  private String inputFileName;
  private String outputFileName;
  private String operation;

  /* variables used for identifying output file confirmed for overwriting */
  private long outputFileSize = -1;
  private long outputFileLastModified = -1;

  /* variable used for displaying status messages */
  private Label result = new Label();

  /* 
   * TextField was chosen over Label because the text from a TextField can be copied by the user
   * and also because if the text is too long to be displayed on a single line inside the window,
   * the user can scroll through the text. 
   */
  private TextField inputFileField = new TextField();  /* used as non-editable field, to display the name of the input file given by the user */
  private TextField outputFileField = new TextField(); /* used as non-editable field, to display the name of the output file given by the user */

  /* ComboBox used so that the user can choose between encode and decode operations */
  private ComboBox<String> operationType; 

  /* method used to display a status message */
  private void showMessage(String message) {
    result.setText(message);
  }

  /* method used to clear a status message */
  private void clearMessage() {
    result.setText("");
  }

  /* method used to explicitly ask for output file overwrite confirmation */
  public String requestFileOverwriteConfirmation(String fileName){

    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("Confirm Save As");

    /* creating a Tooltip, so that user would see full file name when hovering with the mouse over the question regarding file overwrite confirmation */
    Tooltip tp = new Tooltip(fileName);
    tp.setMaxWidth(600);
    tp.setWrapText(true);

    Label l = new Label("Output file already exists."+System.getProperty("line.separator")+"Do you want to overwrite it?");
    l.setWrapText(true);
    Tooltip.install(l, tp);

    alert.setHeaderText(null);
    alert.getDialogPane().setContent(l);

    ButtonType yesButton = new ButtonType("Yes", ButtonData.OK_DONE);
    ButtonType noButton = new ButtonType("No", ButtonData.CANCEL_CLOSE);

    alert.getButtonTypes().setAll(yesButton, noButton);

    Optional<ButtonType> result = alert.showAndWait();
    if (result.get() == yesButton){
      /* user chose "Yes" as answer */
      return "YES";
    }
    else{
      /* user chose "No" as answer or user closed this confirmation window */
      return "NO";
    }

  }

  /* creating event handler for the button used to select the input file */
  private class BrowseInputFileButtonListener implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent e) {
      clearMessage();
      /* creating a FileChooser and adding a filter to set which files can be chosen in the open file dialog - text files, in this case */
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Select input file");
      FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
      fileChooser.getExtensionFilters().add(extFilter);

      /* open dialog for input file selection */
      File selectedFile = fileChooser.showOpenDialog(mainStage);

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
      File savedFile = fileChooser.showSaveDialog(mainStage);

      if (savedFile != null) {
        try{

          /* getting the full file name and storing it to a variable */
          outputFileName = savedFile.getCanonicalPath();
          /* displaying file name */
          outputFileField.setText(outputFileName);

          /*
           * "showSaveDialog" is a native method relying on operating system for choosing output file.
           * File overwrite confirmation dialog appears automatically, if it is the case.
           * We want to remember that overwrite confirmation was given for this specific file, so that no additional confirmation is asked later.
           * If we got to this point and we have a valid File object and a corresponding physical file on disk for this File object,
           * we know that overwrite confirmation was given when selecting the file using native "showSaveDialog" method.  
           */
          TextFileConverter tfc = new TextFileConverter();
          if (tfc.fileExists(outputFileName)){
            /* we store identification data for the file that user agreed to overwrite */
            outputFileSize = savedFile.length();
            outputFileLastModified = savedFile.lastModified();
          }
          else{
            /*
             *  User chose as output a file that does not yet exist on disk.
             *  We reset information about output file confirmed for overwriting, to clear data that might have been set in the past, regarding another file.
             */
            outputFileSize = -1;
            outputFileLastModified = -1;
          }

        }
        catch (Exception ex){
          showMessage("Error encountered when getting output file name.");
        }

      }
      else{
        showMessage("Output file selection cancelled.");
      }

    }

  }

  /* creating event handler for the button used to clear the input form */
  private class ClearButtonListener implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent e) {
      clearMessage();

      operationType.setValue(null); /* this will also set "operation" variable to null, because of ChangeListener added to "operationType" */

      inputFileName=null;
      inputFileField.setText(null);

      outputFileName=null;
      outputFileField.setText(null);
      
      outputFileSize = -1;
      outputFileLastModified = -1;
    }
  }

  /* creating event handler for the button used to run the conversion */
  private class ExecuteButtonListener implements EventHandler<ActionEvent> {
    @Override
    public void handle(ActionEvent e) {

      /* status message to be displayed */
      String message = "";

      /* clearing status message that might be shown due to previous execution */
      showMessage("");

      if ((operation != null) & (inputFileName != null) & (outputFileName != null)) {

        /* it means that all the required data was entered */

        /* we need a TextFileConverter, which contains the methods to be invoked for the conversion */
        TextFileConverter tfc = new TextFileConverter();

        boolean proceedWithConversion = false;

        /* 
         * Checking if a file with the same name as the chosen output file already exists on disk.
         * If it exists, we check if overwrite confirmation was already given for that file or we explicitly ask for overwrite confirmation.
         */
        if (tfc.fileExists(outputFileName)) {

          /* on disk, there already is a file with the same name as the chosen output file */

          /* getting disk file properties */
          File f = new File(outputFileName);
          long existingFileSize = f.length();
          long existingFileLastModified = f.lastModified();

          /* checking if there was a previous overwrite confirmation for the file on disk */
          if ((outputFileSize == existingFileSize) & (outputFileLastModified == existingFileLastModified)){
            /* it means that file on disk is already confirmed for overwriting, during file selection process */
            proceedWithConversion = true;
          }
          else{
            /* 
             * No overwrite confirmation was given for the file currently on disk.
             * We explicitly ask for file overwrite confirmation.
             */

            String answer = requestFileOverwriteConfirmation(outputFileName);

            if (answer == "YES"){
              /* user agreed to overwrite file that exists on disk */
              proceedWithConversion = true;
            }
            else{
              /*
               *  User clicked on "No" button or closed the confirmation window without answering.
               *  We do nothing here ("proceedWithConversion" variable remains false).
               */
            }

          }

        }
        else {
          /* output file does not yet exist on disk */
          proceedWithConversion = true;
        }

        if (proceedWithConversion == true){

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

          /*
           * Conversion methods run their own validations, which may result in stopping execution before generating output files.
           * We only reset variables used for identifying confirmed for overwriting output file if conversion resulted in generating another output file.
           * Otherwise, we keep that information, so that users are not asked for output file overwrite confirmation if they already confirmed that in the past
           * and nothing happened to the output file in the mean time.
           */
          if (tfc.fileExists(outputFileName)) {

            /* getting disk file properties */
            File f = new File(outputFileName);

            if ((outputFileSize == f.length()) & (outputFileLastModified == f.lastModified())){
              /*
               * We have a previously selected and confirmed for overwriting output file ("outputFileSize" and 
               * "outputFileLastModified" variables have other values than their default "-1" ones, because 
               * they are identical to disk file properties, which cannot be "-1") and after the execution 
               * we have a file on disk which is identical to the previously chosen one.
               * That means no new output file was generated, so we keep the variables used for identifying
               * the previously chosen file. As a result, we do nothing here.  
               */
            }
            else {
              /*
               * A new output file was generated (whether a file with the same name previously existed on disk or not).
               * We reset data that might have stored properties of previously confirmed for overwriting output file.
               */
              outputFileSize = -1;
              outputFileLastModified = -1;
            }
          }
          else{
            /* No output file was generated. We do nothing here. */
          }

        }
        else {
          showMessage("Not allowed to overwrite");
        }

      } /* closing IF statement which checks if the user selected an input file, an output file and an operation type */

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

    mainStage = stage;

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

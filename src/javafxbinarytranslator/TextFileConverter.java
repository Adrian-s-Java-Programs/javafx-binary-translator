package javafxbinarytranslator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class TextFileConverter {

  private boolean encodingExceptionCaught = false;
  private boolean decodingExceptionCaught = false;

  /* 
   * UTF-8 charset was chosen because it is standardized, it covers many characters 
   * and a UTF-8 file is also backwards compatible with basic ASCII files 
   */
  static final Charset chosenCharset = StandardCharsets.UTF_8;

  /* method used to check if a file already exists */
  private boolean fileExists(String fileName){
    Path path = Paths.get(fileName);

    if(Files.exists(path) && !Files.isDirectory(path)){
      return true;
    }

    return false;
  }


  /* method used for deleting a file;
   * it is called because the methods used for converting to and from binary write to the output file by appending lines,
   * which would result in undesired effect if the output file already exists;
   * to avoid that, the output file is deleted if it already exists before starting the conversion
   */
  private void deleteFileIfExists(String fileName) throws IOException{

    if(fileExists(fileName)) {
      Path path = Paths.get(fileName);    
      Files.delete(path);
    }
  }

  /* This method is used to check if an input file is empty.
   * 
   * The input file can be a plain-text file (with basic ASCII characters) or a UTF-8 file.
   * 
   * Most UTF-8 files start with a BOM (byte-order mark), but some text editors can be configured not to write a BOM.
   * The BOM is a character at the beginning of the file and has a numerical value of 65279.
   * 
   * This method considers a file to be empty if it contains nothing or only a new line character, 
   * or if it contains a BOM and nothing else or a BOM followed by a new line and nothing else after that.    
   */
  private boolean isEmptyFile(String fileName) throws IOException {

    boolean result = false;
    final int BOM = 65279;

    Path path = Paths.get(fileName);

    String firstLine, secondLine;

    try(BufferedReader reader = Files.newBufferedReader(path, chosenCharset)){

      firstLine = reader.readLine();

      if (firstLine == null){
        /* completely empty simple file */
        result = true;
      }

      else{

        secondLine = reader.readLine();

        if (firstLine.isEmpty()&&secondLine==null){
          /* simple file containing only one new line */
          result = true;
        }

        if(!firstLine.isEmpty()){
          if ((firstLine.codePointAt(0) == BOM)&(firstLine.length()==1)&(secondLine==null)){
            /* empty UTF-8 file with BOM */
            result = true;
          }
        }

      }

    }

    return result;

  }

  /* 
   * This method is used to make sure the user does not write to an output file with the same name and path as the input file.
   * Canonical file names are used, to compare the real full file names, in case relative paths were passed as arguments.
   * 
   * Unfortunately, Java NIO does not come with a method to get a file's canonical path, so Java IO was used.
   * However, Java NIO comes with a method called 'isSameFile', which in some cases (when the paths to be checked are different)
   * checks the files on disk and can throw exceptions if the files to be checked are not found.
   * 
   * The following method is designed to be used when preventing the creation of an output file that would overwrite an input file.
   * If the output file does not exist (which makes it different from the existing input file), it will not throw an exception, 
   * like 'isSameFile' method from Java NIO would.
   */
  private boolean sameCanonicalFileNames(String firstFileName, String secondFileName) throws IOException{

    File firstFile = new File(firstFileName);
    File secondFile = new File(secondFileName);

    if(firstFile.getCanonicalPath().equalsIgnoreCase(secondFile.getCanonicalPath())){
      return true;
    }

    else {
      return false;
    }

  }

  /* This method performs initial checks and cleanup; it is called in the methods responsible for converting to/from binary */
  private String performPreliminaryOperations(String inputFile, String outputFile){

    String result = "checks successful";

    try{
      if (!fileExists(inputFile)){
        result = "Input file does not exist.";
        return result;
      }
    }
    catch(Exception e){ 
      result = "Error when checking if input file exists.";
      return result;
    }


    try{
      if (isEmptyFile(inputFile)){
        result = "Input file is empty.";
        return result;
      }
    }
    catch(NoSuchFileException e){
      result = "Error: could not find input file.";
      return result;
    }
    catch(MalformedInputException e){
      result = "Input file must be encoded as UTF-8 or must be plain text with basic ASCII characters.";
      return result;
    }
    catch(Exception e){
      result = "Error when checking if input file is empty.";
      return result;
    }


    try{
      if(sameCanonicalFileNames(inputFile,outputFile)){
        result = "Input file and output file must not be the same.";
        return result;
      }
    }
    catch (Exception e){
      result = "Exception when checking if output file is same as input file.";
      return result;
    }


    try{
      deleteFileIfExists(outputFile);
    }
    catch (Exception e){
      result = "Error: could not delete pre-existing output file."; 
      return result;
    }

    return result;

  }


  /* This method takes an input file and converts it to binary */
  String encodeToBinary(String inputFileName, String outputFileName){

    String result = "success";
    String preliminaryChecks = "";

    encodingExceptionCaught = false;

    preliminaryChecks = performPreliminaryOperations(inputFileName, outputFileName);

    if (preliminaryChecks.compareTo("checks successful")!=0){
      return preliminaryChecks;
    }

    /* we read the input file and create a Stream where each element is linked to a file line */
    Stream<String> stream = Stream.empty();
    try{
      stream = Files.lines(Paths.get(inputFileName), chosenCharset);
    }
    catch(Exception e){
      result = "An error occurred when reading the input file.";
      stream.close();
      return result;
    }

    /*
     * For each Stream element (which is an input file line) we call a method which converts to binary all characters
     * from that line and writes them to the output file as a line;
     * Empty input file lines are written to the output the way they are, as this program is intended to keep paragraphs in a visual way. 
     */
    try{
      stream.forEach(s -> encodeLine(s,outputFileName));
    }
    catch (Exception e){
      result = "Error: "+e.getMessage();
      stream.close();
      return result;
    }

    stream.close();

    if(encodingExceptionCaught == true) {
      /* 
       * This application is designed to try its best when converting files.
       * That means if a character conversion fails, the program keeps running and tries to convert the rest of characters.
       * In the worst case scenario, the user would get an empty output file.
       * 
       * In practice, it is hard to generate encoding errors.
       * That would require illegal input characters (but preliminary checks will report incompatible input such as 
       * non-UTF-8 file) or some coding bug.  
       */
      result = "Problems have occurred, but an output file was generated.";
    }

    return result;

  }

  /* 
   * This method receives a String that represents a line from the input file,
   * converts to binary each character from the string and writes the encoded line to the output file. 
   */
  private void encodeLine(String s, String outputFileName) {

    /* we create a Stream of int elements, where each element is the numerical value of each character in the input string */
    IntStream intStream = s.chars();

    /* 
     * We convert to binary each int element from the Stream and then we join the binary results to a single String
     * where binary strings are separated by spaces. The conversion is done in parallel for each element,
     * and the final result will keep the initial order, as the 'collect' method is thread-safe.
     */
    String encodedString = intStream
                                    .parallel()
                                    .boxed()
                                    .map(x -> getBinaryAsString(x))
                                    .collect(Collectors.joining(" "));

    intStream.close();

    encodedString+=System.lineSeparator();

    /* We write the result (a whole input file line converted to binary) to the output file */
    try{
      Files.write(Paths.get(outputFileName), encodedString.getBytes(chosenCharset), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    catch (AccessDeniedException ade){
      throw new RuntimeException("Cannot write output file (access denied). Please try saving to a different location.");
    }
    catch(Exception e){
      throw new RuntimeException("A problem occurred when writing to output file.");
    }

  }


  /* 
   * This method converts a number to binary and returns the result as a String.
   * 
   * String was chosen for the result because it can hold long binary values that would not fit into types such as Integer.
   * 
   * Using Integer for the result will lead to RuntimeExceptions when trying to store a long binary number as Integer.
   * And some UTF-8 characters result in long binary numbers. 
   */
  private String getBinaryAsString(Integer i){
    /* 
     * Normally, RuntimeExceptions should not occur here.
     * However, for defensive purpose, exception-handling logic is present here.
     * 
     * This application is a best-effort converter and is designed to continue 
     * with the conversion even if errors occur for some characters.
     */
    try {
      return Integer.toBinaryString(i);
    }
    catch (Exception e) {
      /* 
       * We mark that a problem has occurred. 
       * No other actions are taken here, because we want the program 
       * to continue to try to convert the rest of characters.
       */
      encodingExceptionCaught = true; /* based on this variable, the user will be informed that there were issues */
      return "";
    }

  }

  /* This method takes as input a file containing text written in binary and converts the binary text to normal text */
  String decodeFromBinary(String inputFileName, String outputFileName){

    String result = "success";
    String preliminaryChecks = "";

    decodingExceptionCaught = false;

    preliminaryChecks = performPreliminaryOperations(inputFileName, outputFileName);

    if (preliminaryChecks.compareTo("checks successful")!=0){
      return preliminaryChecks;
    }

    /* we read the input file and create a Stream where each element is linked to a file line */
    Stream<String> stream = Stream.empty();
    try{
      stream = Files.lines(Paths.get(inputFileName), chosenCharset);
    }
    catch(Exception e){
      result = "An error occurred when reading the input file.";
      stream.close();
      return result;
    }

    /* 
     * For each Stream element (which is an input file line) we call a method which decodes from binary 
     * all characters from that line and writes them to the output file as a line.
     */
    try{
      stream.forEach(s -> decodeLine(s,outputFileName));
    }
    catch (Exception e){
      result = "Error: "+e.getMessage(); 
      stream.close();
      return result;
    }

    stream.close();

    if(decodingExceptionCaught == true) {
      /* 
       * This application is designed to try its best when converting files.
       * That means if a character conversion fails, the program keeps running and tries to convert the rest of characters.
       * 
       * In practice, decoding errors can occur because of illegal characters.
       * For example, if the user manually edited a file with binary numbers and wrote non-binary characters inside the file,
       * the decode operation will skip the illegal characters (Java exceptions will be thrown during the conversion, but the 
       * program will deal with them and the resulted decoded character will be an empty string, hence the skipping effect).
       * Also, decoding errors will occur if the user mistakes the file given as input. In this case, the output file could 
       * be empty, or with new lines, or with some junk characters (depending on whether the input file contains some text 
       * with something that could be considered binary).
       */
      result = "Problems have occurred, but an output file was generated.";
    }

    return result;

  }

  /* 
   * This method receives a String that represents a line from the input file.
   * The line contains binary strings separated by spaces. 
   * This method converts each binary token from the file line to its
   * character representation and writes the decoded line to the output file.
   */
  void decodeLine(String fileLine, String outputFileName){

    /* 
     * We create a Stream of String elements, where each element is a binary string representing a character;
     * Normally, the elements should be separated by single spaces (if the encoding was done using this application),
     * but the application will also accept multiple spaces or tabs as separators. 
     */
    Stream<String> st = Stream.of(fileLine.split("\\s+"));

    /* 
     * We convert to normal text each binary element from the Stream and then we join the results to a single String.
     * The conversion is done in parallel for each element, and the final result will keep the initial order,
     * as the 'collect' method is thread-safe.
     */
    String decodedString = st
                             .parallel()
                             .map(x -> decodeCharacter(x))
                             .collect(Collectors.joining(""));

    st.close();

    decodedString+=System.lineSeparator();

    try{
      Files.write(Paths.get(outputFileName), decodedString.getBytes(chosenCharset), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    catch (AccessDeniedException ade){
      throw new RuntimeException("Cannot write output file (access denied). Please try saving to a different location.");
    }
    catch(Exception e){
      throw new RuntimeException("A problem occurred when writing to output file.");
    }

  }

  /* method used for converting from binary to int */
  private int getDecimal(String binaryString){
    return Integer.parseInt(binaryString,2);
  }

  /* 
   * This method is used for converting a character from its numerical representation to normal text.
   * It returns a character stored as a String, to work with it while operating on a Stream of String elements. 
   */
  private String getChar(int i){
    return String.valueOf((char)i);
  }

  /* method used for decoding from binary while dealing with exceptions */
  private String decodeCharacter(String binaryString){

    String result = "";

    if (binaryString.isEmpty()){ 
      /* 
       * This can happen if an empty line was read from the input file, which resulted in a single empty string after that line was split into tokens.
       * In this case, we return an empty string which will receive a line separator in the calling method, which will result in an empty line being
       * written to the output file, which is what we want. 
       */
      return "";
    }

    /* 
     * This application is a best-effort converter and is designed to continue 
     * with the conversion even if errors occur for some characters.
     * Errors can occur if the input file does not contain only binary strings.
     */
    try{
      result = getChar(getDecimal(binaryString));
    }
    catch (Exception e){
      /* 
       * We mark that a problem has occurred. 
       * No other actions are taken here, because we want the program 
       * to continue to try to convert the rest of characters.
       */
      decodingExceptionCaught = true; /* based on this variable, the user will be informed that there were issues */
      return ""; /* alternately, a dummy character could be returned, to be used as a flag for failed conversions */
    }
  return result;
  }


}

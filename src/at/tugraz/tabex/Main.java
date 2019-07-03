package at.tugraz.tabex;

import at.tugraz.tabex.Constants.ExtractionType;
import at.tugraz.tabex.Constants.Workflow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class Main {
    private static int extractionType = ExtractionType.ALL;
    private static int fileIndex = 0;
    private static String workflow = Workflow.NATIVE;
    private static File input_path = null;
    private static String input_path_string = "";
    private static String output_path = "";

    public static void main(String[] args) {

        boolean isValidInput = handleInput(args);
        if (!isValidInput) {
            return;
        }

        File[] listOfFiles = input_path.listFiles();

        if (listOfFiles == null || listOfFiles.length == 0) {
            System.out.println("No Files in Input Folder...");
            return;
        }

        TabExHandler handler = new TabExHandler(workflow, extractionType, fileIndex, listOfFiles, output_path);
        try {
            handler.extract();
        } catch (IOException ex){
            System.out.println("Something went wrong during extraction...");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static boolean handleInput(String[] args) {
        // [0] input_path
        // [1] output_path
        // [2] workflow (SCANNED | NATIVE)
        // [3] fileIndex

        if (args.length == 0) {
            System.out.println("Too few Arguments given. Use -help to get help.");
            return false;
        }
        if (args.length > 4) {
            System.out.println("Too many Arguments given. Use -help to get help.");
            return false;
        }

        input_path_string = args[0];

        if (input_path_string.equals("-help")) {
            String format = "\t %-40s | %-10s | %s%n";
            System.out.printf("Following arguments are accepted:\n");
            System.out.printf(format, "[0] path to source folder", "(required)", "absoulte path");
            System.out.printf(format, "[1] path to output folder", "(required)", "absoulte path");
            System.out.printf(format, "[2] pdf type", "(optional)", "SCANNED | NATIVE");
            System.out.printf(format, "", "", "SCANNED mode requires two subfolders within input folder:");
            System.out.printf(format, "", "", "\t\"\\pdf\" and \"\\excel\"");
            System.out.printf(format, "[3] index of pdf file within folder", "(optional)", "if nothing specified process all files in folder otherwise uses file in folder with given index");
            System.out.printf("Example: \"C:\\temp\\data\\input\" \"C:\\temp\\data\\output\" SCANNED 0");
            return false;
        }

        output_path = args[1];

        input_path = new File(input_path_string);

        if (args.length > 2) {
            workflow = args[2];
        }

        if (args.length > 3) {
            extractionType = ExtractionType.SPECIFIC;
            fileIndex = Integer.parseInt(args[3]);
        }

        if (workflow.equals(Workflow.SCANNED)) {
            input_path_string = Paths.get(input_path.toString(), "pdf").toString();
            input_path = new File(input_path_string);
        }

        return true;
    }
}

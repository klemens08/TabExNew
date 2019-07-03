package at.tugraz.tabex;


import at.tugraz.tabex.Constants.ExtractionType;
import at.tugraz.tabex.Constants.Workflow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;


public class TabExHandler {

    private String workflow;
    private int extractionType;
    private File[] listOfFiles;
    private int fileIndex;
    private String output_path;
    ArrayList<MetaData> allFiles = new ArrayList<>();
    private static String evaluationFileName = "";
    private static String evaluationCheckDataPath = "C:\\Temp\\TabEx\\CheckData.xlsx";
    private static HashMap<String, List<CheckData>> evaluationCheckData;
    private static boolean printSuccessMessages = true;

    private static int correctPagesCount = 0;
    private static int correctDocumentsCount = 0;
    private static int totalPagesCount = 0;
    private static int totalDocumentsCount = 0;
    private static int totalExpectedTableCount = 0;
    private static int totalDetectedTableCount = 0;
    private static int totalCorrectlyDetectedTableCount = 0;

    public TabExHandler(String workflow, int extractionType, int fileIndex, File[] listOfFiles, String output_path) {
        this.workflow = workflow;
        this.extractionType = extractionType;
        this.fileIndex = fileIndex;
        this.listOfFiles = listOfFiles;
        this.output_path = output_path;

        Timestamp currentTimeStamp = new Timestamp(System.currentTimeMillis());
        evaluationFileName = "logging_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(currentTimeStamp) + ".txt";
        try {
            evaluationCheckData = getEvaluationCheckData(evaluationCheckDataPath);
        } catch (IOException ex) {
            System.out.println("Error while reading check data file.");
            evaluationCheckData = new HashMap<>();
        }
    }

    public boolean extract() throws Exception {
        boolean wasExportOkay = false;
        if (workflow.equals(Workflow.SCANNED)) {
            if (extractionType == ExtractionType.SPECIFIC) {
                wasExportOkay = ExtractSingleScanneFile(listOfFiles, fileIndex);
            } else if (extractionType == ExtractionType.ALL) {
                //test all files
                int successFullExtractionCount = ExtractAllScannedFiles(listOfFiles);
                System.out.println("Es wurden " + successFullExtractionCount + " von " + listOfFiles.length + " Dateien ohne Fehler exportiert.");
                wasExportOkay = successFullExtractionCount == listOfFiles.length;
            }
        } else if (workflow.equals(Workflow.NATIVE)) {
            if (extractionType == ExtractionType.SPECIFIC) {
                //test single file
                wasExportOkay = ExtractSingleNativeFile(listOfFiles, fileIndex);
            } else if (extractionType == ExtractionType.ALL) {
                //test all files
                int successFullExtractionCount = ExtractAllNativeFiles(listOfFiles);
                System.out.println("Es wurden " + successFullExtractionCount + " von " + listOfFiles.length + " Dateien ohne Fehler exportiert.");
                wasExportOkay = successFullExtractionCount == listOfFiles.length;
            }
        }

        createSummaryEvaluationLog();
        printAllFilesMetaDataDetails(allFiles);
        return wasExportOkay;
    }

    private boolean ExtractSingleNativeFile(File[] listOfFiles, int fileIndex) throws Exception {
        // just to extract one single file
        String filePath = listOfFiles[fileIndex].getPath();
        String fileName = Paths.get(this.output_path, listOfFiles[fileIndex].getName()).toString();
        boolean wasExtractionSuccessful = ExecuteNativePdfTableExtraction(filePath, fileName);
        return wasExtractionSuccessful;
    }

    private boolean ExtractSingleScanneFile(File[] listOfFiles, int fileIndex) throws IOException {
        // just to extract one single file
        String filePath = listOfFiles[fileIndex].getPath();
        String fileName = Paths.get(output_path, listOfFiles[fileIndex].getName()).toString();
        boolean wasExtractionSuccessful = ExecuteScannedPdfTableExtraction(filePath, fileName);
        return wasExtractionSuccessful;
    }

    private int ExtractAllNativeFiles(File[] listOfFiles) throws Exception {
        //to extract all files in folder
        int successFullExtractionCount = 0;
        for (int fileIndex = 0; fileIndex < listOfFiles.length; fileIndex++) {
            String filePath = listOfFiles[fileIndex].getPath();
            String fileName = Paths.get(output_path, listOfFiles[fileIndex].getName()).toString();
            boolean wasExtractionSuccessful = ExecuteNativePdfTableExtraction(filePath, fileName);
            if (wasExtractionSuccessful) {
                successFullExtractionCount++;
            }
        }
        return successFullExtractionCount;
    }

    private int ExtractAllScannedFiles(File[] listOfFiles) throws IOException {
        //to extract all files in folder
        int successFullExtractionCount = 0;
        for (int fileIndex = 0; fileIndex < listOfFiles.length; fileIndex++) {
            String filePath = listOfFiles[fileIndex].getPath();
            String fileName = Paths.get(output_path, listOfFiles[fileIndex].getName()).toString();
            boolean wasExtractionSuccessful = ExecuteScannedPdfTableExtraction(filePath, fileName);
            if (wasExtractionSuccessful) {
                successFullExtractionCount++;
            }
        }
        return successFullExtractionCount;
    }

    public boolean ExecuteNativePdfTableExtraction(String filePath, String fileName) throws Exception {

        MetaData metaData = new MetaData();
        allFiles.add(metaData);
        metaData.fileName = fileName;

        preprocessFile(filePath, metaData);

        detectTablesFromNativePdfs(metaData);

        extractTablesFromPdfs(metaData);

        for (Table table : metaData.tables) {
            table.pageNumber = table.tableBody.sparseBlockLines.get(0).page;
        }

        detectHeadersAndFooters(metaData);

        Path path = Paths.get(fileName);
        String file = path.getFileName().toString().replace(".pdf", "");
        createEvaluationLog(metaData.tables, file);

        //String fileName = listOfFiles[fileIndex].getName();
        fileName = fileName.replace("pdf", "xls");

        printLineWithDetails(metaData);

        boolean wasExportSuccessful = exportToExcel(metaData.tables, fileName);

        return wasExportSuccessful;
    }


    public boolean ExecuteScannedPdfTableExtraction(String filePath, String fileName) throws IOException {

        MetaData metaData = new MetaData();

        preprocessFile(filePath, metaData);

        return true;
    }

    private void preprocessFile(String filePath, MetaData metaData) throws IOException {
        File pdfFile = new File(filePath);

        //preprocess file (get textpositions, words and lines)
        new Preprocessing(metaData, pdfFile);
    }

    private static void detectTablesFromNativePdfs(MetaData metaData) {
        //perform table detections
        TableDetection tableDetection = new TableDetection(metaData);
        tableDetection.getMinimumSpaceGapThresholdByMedian(true);
        tableDetection.detectSparseLines(); // possible to use MEAN, MEDIAN, OUTLIERS as parameter to overwrite default

        //check if page is multi column (just works for 2 columns)
        double multiColumnPageCount = 0;
        double pagesOfDoc = metaData.pages.size();
        for (Page page : metaData.pages) {
            if (Preprocessing.isPageMultiColumn(page.linesOfPage, page)) {
                multiColumnPageCount++;
                //page.isMultiColumn = true;
            }
        }
        metaData.multiColumnPercentage = (multiColumnPageCount / pagesOfDoc) * 100;
        if (metaData.pages.size() > 1 && metaData.multiColumnPercentage > 40) {
            metaData.isMultiColumnPDF = true;

            ArrayList<Float> endOfLeftColumnList = new ArrayList<>();
            ArrayList<Float> startOfRightColumnList = new ArrayList<>();
            for (Page page : metaData.pages) {
                if (page.endColumnBorder != 0.0f) {
                    endOfLeftColumnList.add(page.endColumnBorder);
                }
                if (page.startColumnBorder != 0.0f) {
                    startOfRightColumnList.add(page.startColumnBorder);
                }

            }
            Collections.sort(endOfLeftColumnList);
            Collections.sort(startOfRightColumnList);

            metaData.startColumnBorder = startOfRightColumnList.get(startOfRightColumnList.size() / 2);
            metaData.endColumnBorder = endOfLeftColumnList.get(endOfLeftColumnList.size() / 2);

            tableDetection.getMinimumSpaceGapThresholdByMedian(false);
            tableDetection.detectSparseLines(); // possible to use MEAN, MEDIAN, OUTLIERS as parameter to overwrite default

            for (Page page : metaData.pages) {
                if (metaData.isMultiColumnPDF) {
                    for (Line line : page.linesOfPage) {
                        if (line.isCenterSparse) {
                            for (WordGroup wordGroup : line.wordGroupsInLine) {
                                if (wordGroup.endX < page.centerOfPage) {
                                    line.wordGroupsInLeftColumn.add(wordGroup);
                                } else if (wordGroup.startX > page.centerOfPage) {
                                    line.wordGroupsInRightColumn.add(wordGroup);
                                }
                            }
                        }
                    }
                }
            }

        }

        /*if (metaData.isMultiColumnPDF) {
            tableDetection.getMinimumSpaceGapThresholdByMedian(false);
            tableDetection.detectSparseLines(); // possible to use MEAN, MEDIAN, OUTLIERS as parameter to overwrite default
        }*/


        //get sparse blocks
        tableDetection.detectSparseBlocks();

        //delete invalid lines of sparse blocks
        tableDetection.reviseSparseBlocks();

        //System.out.println("Current Threshhold = " + metaData.minimumSpaceGapThreshold);
        printTables(metaData);
    }

    private void detectHeadersAndFooters(MetaData metaData) {

        HeaderFooterDetection headerFooterDetection = new HeaderFooterDetection();

        for (Table table : metaData.tables) {

            //headerFooterDetection.detectHeaderByKeyword(table);
            if (table.Header.size() == 0) {
                headerFooterDetection.detectHeaderByLineDistances(table, metaData);
            }
        }

        for (Table table : metaData.tables) {

            //headerFooterDetection.detectFootersByKeyword(table);
            if (table.Footer.size() == 0) {
                headerFooterDetection.detectFooterByLineDistances(table, metaData);
            }
        }
    }

    private static void printTables(MetaData metaData) {
        for (Table table : metaData.tables) {
            System.out.println("\nTable\n");

            System.out.println("Header:");
            for (Line line : table.Header) {
                //System.out.println(line.getLineString());
            }

            System.out.println("\nFooter:");
            for (Line line : table.Footer) {
                //System.out.println(line.getLineString());
            }

            System.out.println("\nTableBody:");
            for (Line line : table.tableBody.sparseBlockLines) {
                //System.out.println(line.getLineString());
                String wordGroupString = "";
                for (WordGroup wordGroup : line.wordGroupsInLine) {
                    wordGroupString += wordGroup.getWordGroupString() + " || ";
                }
                System.out.println(wordGroupString);
            }
        }
    }

    private static void extractTablesFromPdfs(MetaData metaData) throws Exception {
        TableExtraction tableExtraction = new TableExtraction(metaData);
        for (Table table : metaData.tables) {
            try {
                tableExtraction.buildTableFromData(table);
            } catch (Exception ex) {
                throw ex;
                //tablesToDelete.add(table);
            }
        }
        ArrayList<Table> tablesToDelete = new ArrayList<>();
        for (Table table : metaData.tables) {
            if (table.tableColumns.size() < 2 || table.tableColumns.get(0).columnLines.size() < 2) {
                tablesToDelete.add(table);
            }
        }
        metaData.tables.removeAll(tablesToDelete);

        for (Table correctTable : metaData.tables) {
            for (Line line : correctTable.tableBody.sparseBlockLines) {
                line.isTableLine = true;
            }
        }
    }

    public static boolean exportToExcel(List<Table> tables, String fileName) {
        Workbook workbook = new HSSFWorkbook();

        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);

        CellStyle styleCentered = workbook.createCellStyle();
        styleCentered.setAlignment(HorizontalAlignment.CENTER);

        for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
            Table table = tables.get(tableIndex);

            if (table.tableColumns.size() == 0) {
                continue;
            }

            Sheet sheet = workbook.createSheet("Tabelle" + (tableIndex + 1));
            int currentLine = 0;
            if (!table.getHeaderString().equals("")) {
                Row headerRow = sheet.createRow(currentLine++);
                Cell headerCell = headerRow.createCell(0);
                headerCell.setCellValue(table.getHeaderString());

                headerCell.setCellStyle(style);

                sheet.addMergedRegion(new CellRangeAddress(
                        0, //first row (0-based)
                        0, //last row  (0-based)
                        0, //first column (0-based)
                        10  //last column  (0-based)
                ));

                sheet.createRow(currentLine++);
            }
            for (int lineIndex = 0; lineIndex < table.tableColumns.get(0).columnLines.size(); lineIndex++, currentLine++) {
                Row row = sheet.createRow(currentLine);
                for (int columnIndex = 0; columnIndex < table.tableColumns.size(); columnIndex++) {
                    TableColumn column = table.tableColumns.get(columnIndex);
                    String cellValue = column.columnLines.get(lineIndex).getWordsString();
                    Cell cell = row.createCell(columnIndex);

                    /*int colSpan = column.columnLines.get(lineIndex).colSpan;
                    if (colSpan > 1) {
                        System.out.println(fileName + " Table: " + tableIndex);
                        //columnIndex = columnIndex + colSpan;
                        sheet.addMergedRegion(new CellRangeAddress(
                                currentLine, //first row (0-based)
                                currentLine, //last row  (0-based)
                                columnIndex, //first column (0-based)
                                columnIndex + (colSpan - 1)  //last column  (0-based)
                        ));
                        cell.setCellStyle(styleCentered);
                    }*/

                    cell.setCellValue(cellValue);
                }
            }

            sheet.createRow(currentLine++);

            Row footerRow = sheet.createRow(currentLine);
            Cell footerCell = footerRow.createCell(0);
            footerCell.setCellValue(table.getFooterString());


            sheet.addMergedRegion(new CellRangeAddress(
                    footerCell.getRowIndex(), //first row (0-based)
                    footerCell.getRowIndex(), //last row  (0-based)
                    0, //first column (0-based)
                    10  //last column  (0-based)
            ));

            footerCell.setCellStyle(style);


            for (int columnIndex = 0; columnIndex < table.tableColumns.size(); columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }
        }

        try {
            File yourFile = new File(fileName);
            yourFile.createNewFile();
            FileOutputStream fileOut = new FileOutputStream(fileName, false);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            System.out.println("File: \"" + fileName + "\" wurde extrahiert. " + tables.size() + " Tabellen wurden erkannt.");
            return true;
        } catch (IOException ioException) {
            System.out.println(ioException.getMessage());
            return false;
        }
    }


    public static void printLineWithDetails(MetaData metaData) {
        System.out.println("\n-------------------------------\n");

        for (Page page : metaData.pages) {


            for (Line line : page.linesOfPage) {
                System.out.println(line.getLineString());
                String wordsWithDistances = "";
                for (Word word : line.wordsInLine) {
                    wordsWithDistances += word.getWordString();
                    wordsWithDistances += " ";
                    wordsWithDistances += word.distanceToNextWord;
                    wordsWithDistances += " | ";

                    //System.out.println(word.getWordString() + " " + word.distanceToNextWord);
                }
                //System.out.println(wordsWithDistances);
                System.out.println("Upperbound:     " + line.upperBound + " | Lowerbound: " + line.lowerBound + " | Lineheight: " + line.lineHeight + " | DistToNextLine: " + line.distanceToNextLine);
                System.out.println("Sparse areas:   " + line.sparsePointCount + " | " + "IsSparse:   " + line.isSparse + " | IsCritical: " + line.isCritical
                        + " | Page: " + line.page + " | IsLastLineOfPage: " + line.lastLineOfPage + " | IsFirstLineOfPage: " + line.firstLineOfPage);
                System.out.println("Line Leftbound: " + line.getWordsInLine().first().startX + " | Line Rightbound: " + line.getWordsInLine().last().endX + " Line Width: " + line.getWidth() + " WidthToBeCritical: " + metaData.medianWidthOfLines / 2 + " Max: " + metaData.maxWidthOfLines);
                System.out.println("Is Center Sparse: " + line.isCenterSparse + " | Left Column Sparse: " + line.leftColumnSparse + " | Left Column Critical: " + line.leftColumnCritical + " | Right Column Sparse: " + line.rightColumnSparse + " | Right Column Critical: " + line.rightColumnCritical);


                String wordGroups = "";
                for (WordGroup wordGroup : line.getWordGroupsInLine()) {
                    wordGroups += wordGroup.getWordGroupString();
                    wordGroups += " | ";
                }
                System.out.println(wordGroups);
                System.out.println("\n-------------------------------\n");
                /*wordGroups = "";
                for (WordGroup wordGroup : line.wordGroupsInLeftColumn) {
                    wordGroups += wordGroup.getWordGroupString();
                    wordGroups += " | ";
                }
                System.out.println(wordGroups);
                System.out.println("\n-------------------------------\n");
                wordGroups = "";
                for (WordGroup wordGroup : line.wordGroupsInRightColumn) {
                    wordGroups += wordGroup.getWordGroupString();
                    wordGroups += " | ";
                }
                System.out.println(wordGroups);
                System.out.println("\n-------------------------------\n");*/

            }


        }

        /*for (Page page : metaData.pages) {
            System.out.println("Page: " + page.pageNumber);
            System.out.println("Number of lines on Page: " + page.linesOfPage.size());
            System.out.println("Page Height: " + page.height + " | Page Width: " + page.width);
            //System.out.println("First Line of Page: " + page.firstLineOfPage.getLineString());
            //System.out.println("Last Line of Page: " + page.lastLineOfPage.getLineString());
        }
        System.out.println("\n-------------------------------\n");*/
    }

    private void printAllFilesMetaDataDetails(ArrayList<MetaData> allFiles) {

        for (MetaData metaData : allFiles) {

            System.out.println("Filename: " + metaData.fileName + " | MultiColumnPercentage: " + metaData.multiColumnPercentage + "% | isMultiColumn: " + metaData.isMultiColumnPDF);

        }

        System.out.println("");

        int numberOfMCFiles = 0;
        for (MetaData metaData : allFiles) {
            if (metaData.isMultiColumnPDF) {
                numberOfMCFiles++;
                System.out.println(metaData.fileName);
            }
        }
        System.out.println("\nMulti Column Files: " + numberOfMCFiles + " | (Correct: 8" + " | Percentage: " + (numberOfMCFiles / 8 * 100 + ")"));
        System.out.println("Single Column Files: " + (allFiles.size() - numberOfMCFiles) + " | (Correct: 57" + " | Percentage: " + (((allFiles.size() - numberOfMCFiles) / 57) * 100) + ")");
        System.out.println("Number of Files: " + allFiles.size() + " | (Correct: 65" + " | Percentage: " + (allFiles.size() / 65 * 100) + ")");

    }

    private static void createEvaluationLog(List<Table> tables, String fileName){
        String evaluationLogMessage = CreateEvaluationLogMessage(tables, fileName);
        writeEvaluationLog(evaluationLogMessage);
    }

    public static void createSummaryEvaluationLog() {
        Float pagesPercent = correctPagesCount * 100.0f / totalPagesCount;
        Float documentsPercent = correctDocumentsCount * 100.0f / totalDocumentsCount;
        Float precision = totalCorrectlyDetectedTableCount * 100.0f / totalDetectedTableCount;
        Float recall = totalCorrectlyDetectedTableCount * 100.0f / totalExpectedTableCount;

        String message = "\r\n-------------- |SUMMARY| --------------\r\n\r\n" +
                "Documents:\tTotal " + totalDocumentsCount + "\tCorrect Documents " + correctDocumentsCount + "\t-> " + documentsPercent + "%\r\n" +
                "Pages:\t\tTotal " + totalPagesCount + "\tCorrect Pages " + correctPagesCount + "\t-> " + pagesPercent + "%\r\n" +
                "Tables:\t\tExpected " + totalExpectedTableCount + "\tDetected " + totalDetectedTableCount + "\t\tCorrectly Detected " + totalCorrectlyDetectedTableCount + "\r\n" +
                "\r\n" +
                "Precision:\t" + precision + "%\r\n" +
                "Recall:\t\t" + recall + "%\r\n";

        writeEvaluationLog(message);
    }

    private static void writeEvaluationLog(String message){
        String evaluationFolderPath = "C:\\temp\\TabEx\\Evaluation";
        File dir = new File(evaluationFolderPath);
        dir.mkdirs();
        try {
            Path evaluationFilePath = Paths.get(evaluationFolderPath, evaluationFileName);
            Files.write(evaluationFilePath, Arrays.asList(message), StandardCharsets.UTF_8,
                    Files.exists(evaluationFilePath) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
        } catch (final IOException ioe) {
            System.out.println("ERROR: Failed to save Evaluation to Log File");
        }
    }

    private static String CreateEvaluationLogMessage(List<Table> tables, String fileName) {
        List<CheckData> checkDatas = evaluationCheckData.get(fileName);
        if (checkDatas == null) {
            return "File " + fileName + " not found in Validation Data Set!";
        }

        int maxPageNumber = ValidationHelper.GetMaxPageNumber(checkDatas, tables);

        boolean isValid = true;
        isValid = tables.size() == checkDatas.size();
        String validationMessages = tables.size() + " tables of possible " + checkDatas.size() + " tables have been detected.\r\n";
        for(int i = 1; i <= maxPageNumber; i++) {
            List<CheckData> checkDatasOfPage = ValidationHelper.GetCheckDataOfPage(checkDatas, i);
            List<Table> tablesOfPage = ValidationHelper.GetTablesOfPage(tables, i);

            if (checkDatasOfPage.size() <= tablesOfPage.size()) {
                totalCorrectlyDetectedTableCount += checkDatasOfPage.size();
            } else {
                totalCorrectlyDetectedTableCount += tablesOfPage.size();
            }

            if (checkDatasOfPage.size() != tablesOfPage.size()) {
                isValid = false;
                validationMessages += "ERROR! Page " + i + ": Detected: " + tablesOfPage.size() + " | Expected: " + checkDatasOfPage.size() + "\r\n";
            } else if (checkDatasOfPage.size() > 0) {
                correctPagesCount++;
                validationMessages += "Page " + i + ": Correct Number of Tables Detected (" + tablesOfPage.size() + ")\r\n";
            }
            totalPagesCount++;
        }

        if (!printSuccessMessages && isValid) {
            return "";
        }

        if (isValid) {
            correctDocumentsCount++;
        }
        totalDocumentsCount++;
        totalExpectedTableCount += checkDatas.size();
        totalDetectedTableCount += tables.size();

        String resultMessage = "\r\n--------------" + (isValid ? " |SUCCESS| " : " |!ERROR!| ") + "--------------\r\n\r\n" +
                "Validation of Table Detection of File: " + fileName + "\r\n";

        resultMessage += validationMessages;

        return resultMessage;
    }

    private static HashMap<String, List<CheckData>> getEvaluationCheckData(String evaluationCheckDataPath) throws IOException {
        FileInputStream fs = new FileInputStream(new File(evaluationCheckDataPath));
        XSSFWorkbook wb = new XSSFWorkbook(fs);
        XSSFSheet sheet = wb.getSheetAt(0);

        HashMap<String, List<CheckData>> result = new HashMap<String, List<CheckData>>();
        CheckData currentCheckData = null;

        Iterator<Row> rowIt = sheet.iterator();
        if (rowIt.hasNext()) {
            rowIt.next(); // skip first line
        }

        while(rowIt.hasNext()) {
            Row row = rowIt.next();
            Iterator<Cell> cellIterator = row.cellIterator();
            int cellIndex = 0;
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                System.out.print(cell.toString() + ";\r\n");
                if (cellIndex == 0) { // fileName
                    if (!result.containsKey(cell.getStringCellValue())) {
                        result.put(cell.getStringCellValue(), new ArrayList<CheckData>());
                    }
                    currentCheckData = new CheckData();
                    result.get(cell.getStringCellValue()).add(currentCheckData);
                } else if (cellIndex == 1) { // tableNumber
                    currentCheckData.tableNumber = (int) cell.getNumericCellValue();
                } else if (cellIndex == 2) { // pageNumber
                    currentCheckData.pageNumber = (int) cell.getNumericCellValue();
                } else if (cellIndex == 3) { // columnNumber of Page
                    currentCheckData.columnNumber = (int) cell.getNumericCellValue();
                } else if (cellIndex == 4) { // rowAmount of Table
                    currentCheckData.rowAmount = (int) cell.getNumericCellValue();
                } else if (cellIndex == 5) { // columnAmount of Table
                    currentCheckData.columnAmount = (int) cell.getNumericCellValue();
                } else if (cellIndex == 6) { // format of Table
                    currentCheckData.orientation = Orientation.values()[(int) cell.getNumericCellValue()];
                } else if (cellIndex == 7) { // format of Table
                    currentCheckData.isMultiColumn = Boolean.parseBoolean(Integer.toString((int) cell.getNumericCellValue()));
                } else if (cellIndex == 7) { // format of Table
                    currentCheckData.isOnePage = Boolean.parseBoolean(Integer.toString((int) cell.getNumericCellValue()));
                } else if (cellIndex == 7) { // format of Table
                    currentCheckData.hasHeader = Boolean.parseBoolean(Integer.toString((int) cell.getNumericCellValue()));
                } else if (cellIndex == 7) { // format of Table
                    currentCheckData.hasFooter = Boolean.parseBoolean(Integer.toString((int) cell.getNumericCellValue()));
                }
                cellIndex++;
            }
        }

        return result;
    }

}

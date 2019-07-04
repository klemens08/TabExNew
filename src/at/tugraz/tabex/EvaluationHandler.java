package at.tugraz.tabex;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class EvaluationHandler {
    private String evaluationFileName = "";
    private String evaluationCheckDataPath = "C:\\Temp\\TabEx\\CheckData.xlsx";
    private HashMap<String, List<CheckData>> evaluationCheckData;
    private boolean printSuccessMessages = true;
    private boolean printSummaryMessages = true;

    private int correctPagesCount = 0;
    private int correctDocumentsCount = 0;
    private int totalPagesCount = 0;
    private int totalDocumentsCount = 0;
    private int totalExpectedTableCount = 0;
    private int totalDetectedTableCount = 0;
    private int totalCorrectlyDetectedTableCount = 0;

    public EvaluationHandler(boolean printSuccessMessages, boolean printSummaryMessages) {
        this.printSuccessMessages = printSuccessMessages;
        this.printSummaryMessages = printSummaryMessages;

        Timestamp currentTimeStamp = new Timestamp(System.currentTimeMillis());
        evaluationFileName = "logging_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(currentTimeStamp) + ".txt";
        try {
            evaluationCheckData = getEvaluationCheckData(evaluationCheckDataPath);
        } catch (IOException ex) {
            System.out.println("Error while reading check data file.");
            evaluationCheckData = new HashMap<>();
        }
    }

    private HashMap<String, List<CheckData>> getEvaluationCheckData(String evaluationCheckDataPath) throws IOException {
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

    public void createEvaluationLog(List<Table> tables, String fileName){
        String evaluationLogMessage = CreateEvaluationLogMessage(tables, fileName);
        writeEvaluationLog(evaluationLogMessage);
    }

    public void createSummaryEvaluationLog() {
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

    private void writeEvaluationLog(String message){
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

    private String CreateEvaluationLogMessage(List<Table> tables, String fileName) {
        List<CheckData> checkDatas = evaluationCheckData.get(fileName);
        if (checkDatas == null) {
            return "File " + fileName + " not found in Validation Data Set!\r\n";
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
}

package at.tugraz.tabex;

import org.apache.poi.ss.formula.functions.Column;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TableExtraction {

    private MetaData metaDataOfContext;

    public TableExtraction(MetaData metaData) {
        metaDataOfContext = metaData;
    }


    public void buildTableFromData(Table table) throws Exception {

        int sparsePointsOfTable = initializeColumnsOfTable(table);
        //float medianYLineSpace = HelperFunctions.getMedianYLineSpace(table.tableBody.sparseBlockLines);
        //float averageYLineSpace = HelperFunctions.getAverageYLineSpace(table.tableBody.sparseBlockLines);

        HashMap<WordGroup, Integer> criticalWordGroupsMap = new HashMap<WordGroup, Integer>();

        for (int currentLineIndex = 0; currentLineIndex < table.tableBody.sparseBlockLines.size(); currentLineIndex++) {
            Line line = table.tableBody.sparseBlockLines.get(currentLineIndex);

            // if line belongs to previous line use temp index to add words in loops to previous line
            /*int tempLineIndex = currentLineIndex;
            if (currentLineIndex != 0 && line.previous.distanceToNextLine < (medianYLineSpace * 0.7)) {

                tempLineIndex = currentLineIndex - 1;
                while (table.tableBody.sparseBlockLines.get(tempLineIndex).toBeDeleted) {
                    tempLineIndex--;
                }
                line.toBeDeleted = true;
            }*/


            for (int currentWordGroupIndex = 0; currentWordGroupIndex < line.getWordGroupsInLine().size(); currentWordGroupIndex++) {
                WordGroup currentWordGroup = line.getWordGroupsInLine().get(currentWordGroupIndex);

                boolean columnFound = addToColumnIfOverlapping(table, currentWordGroup, currentLineIndex, sparsePointsOfTable);

                if (!columnFound) { //no column found for word
                    currentWordGroup.isUnclassified = true;
                    criticalWordGroupsMap.put(currentWordGroup, currentLineIndex);
                }
            }
        }

        reevaluateNonClassifiedWordGroups(criticalWordGroupsMap, table, sparsePointsOfTable);

        fixWrongColumnAlignments(table);

        //delete lines from columns which are marked as toBeDeleted in sparseBlockLines of table
        for (int lineIndex = table.tableBody.sparseBlockLines.size() - 1; lineIndex >= 0; lineIndex--) {
            if (!table.tableBody.sparseBlockLines.get(lineIndex).toBeDeleted) {
                continue;
            }
            for (TableColumn column : table.tableColumns) {
                column.columnLines.remove(lineIndex);
            }
        }
    }


    public int initializeColumnsOfTable(Table table) throws Exception {

        Line maxSparseTableLine = getMostSparsePointLine(table);

        //initialize Columns of Table with start and end X positions
        ColumnLine currentColumnLine = new ColumnLine();
        for (WordGroup currentWordGroup : maxSparseTableLine.getWordGroupsInLine()) {
            currentColumnLine.startX = currentWordGroup.startX;
            currentColumnLine.endX = currentWordGroup.endX;
            currentColumnLine.words.addAll(currentWordGroup.words);
            TableColumn tableColumn = new TableColumn(currentColumnLine.startX, currentColumnLine.endX, table.tableBody.sparseBlockLines.size());
            table.tableColumns.add(tableColumn);
            currentColumnLine = new ColumnLine();
        }
        return maxSparseTableLine.sparsePointCount;
    }

    public Line getMostSparsePointLine(Table table) throws Exception {

        if (table.tableBody == null || table.tableBody.sparseBlockLines.size() == 0) {
            throw new Exception("No Sparse Point Line");
        }

        int indexOfHighestSparsePointCountLine = 0;

        for (Line currentLine : table.tableBody.sparseBlockLines) {
            if (currentLine.sparsePointCount > table.tableBody.sparseBlockLines.get(indexOfHighestSparsePointCountLine).sparsePointCount) {
                indexOfHighestSparsePointCountLine = table.tableBody.sparseBlockLines.indexOf(currentLine);
            }
        }
        return table.tableBody.sparseBlockLines.get(indexOfHighestSparsePointCountLine);
    }

    private boolean addToColumnIfOverlapping(Table table, WordGroup currentWordGroup, int lineIndex, int sparsePointsOfTable) {

        ArrayList<TableColumn> overlappingColumns = new ArrayList<>();
        for (TableColumn currentTableColumn : table.tableColumns) {

            //break loop if no further column is found
            if (currentWordGroup.endX < currentTableColumn.startX) {
                break;
            }

            //check if current wordGroup overlaps with current column
            if (currentWordGroup.startX <= currentTableColumn.endX && currentTableColumn.startX <= currentWordGroup.endX) {

                ColumnLine columnLine = currentTableColumn.columnLines.get(lineIndex);
                //are there already overlapping columns?
                if (overlappingColumns.size() > 0) {
                    columnLine.isSpanned = true;
                    //set colspan of spanning column to the amount of columns spanned
                    setAmountOfColumnsToSpan(table, currentTableColumn, lineIndex);
                } else if (!columnLine.isSpanned) {
                    columnLine.colSpan++;
                }
                overlappingColumns.add(currentTableColumn);
            }
        }

        if (overlappingColumns.size() > 0) {
            TableColumn firstOverlappingColumn = overlappingColumns.get(0);
            ColumnLine lineInFirstOverlappingColumn = firstOverlappingColumn.columnLines.get(lineIndex);

            if (table.tableBody.sparseBlockLines.get(lineIndex).sparsePointCount == sparsePointsOfTable) {
                if (currentWordGroup.startX < firstOverlappingColumn.startX) {
                    firstOverlappingColumn.startX = currentWordGroup.startX;
                }
                if (currentWordGroup.endX > firstOverlappingColumn.endX && overlappingColumns.size() == 1) {
                    firstOverlappingColumn.endX = currentWordGroup.endX;
                }
            }

            if (lineInFirstOverlappingColumn.isSpanned) {
                combineWordGroupForSpanningColumn(table, firstOverlappingColumn, lineIndex, currentWordGroup);
            } else {
                firstOverlappingColumn.columnLines.get(lineIndex).words.addAll(currentWordGroup.words);
            }
        }
        return (overlappingColumns.size() > 0);
    }

    public void setAmountOfColumnsToSpan(Table table, TableColumn currentTableColumn, int lineIndex) {

        int indexOverlappingColumn = table.tableColumns.indexOf(currentTableColumn);
        while (indexOverlappingColumn > 0) {
            indexOverlappingColumn -= 1;
            if (!table.tableColumns.get(indexOverlappingColumn).columnLines.get(lineIndex).isSpanned) {
                table.tableColumns.get(indexOverlappingColumn).columnLines.get(lineIndex).colSpan++;
                break;
            }
        }
    }

    public void combineWordGroupForSpanningColumn(Table table, TableColumn firstOverlappingColumn, int lineIndex, WordGroup currentWordGroup) {
        int indexOverlappingColumn = table.tableColumns.indexOf(firstOverlappingColumn);
        while (indexOverlappingColumn > 0) {
            indexOverlappingColumn -= 1;
            if (!table.tableColumns.get(indexOverlappingColumn).columnLines.get(lineIndex).isSpanned) {
                table.tableColumns.get(indexOverlappingColumn).columnLines.get(lineIndex).words.addAll(currentWordGroup.words);
                break;
            }
        }
    }

    private void reevaluateNonClassifiedWordGroups(HashMap<WordGroup, Integer> criticalWordGroupsMap, Table table, int sparsePointsOfTable) {
        Iterator it = criticalWordGroupsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            WordGroup currentWordGroup = (WordGroup) pair.getKey();
            int lineIndex = (Integer) pair.getValue();
            boolean columnFound = addToColumnIfOverlapping(table, currentWordGroup, lineIndex, sparsePointsOfTable);

            //no matching column --> create new column
            if (!columnFound) {
                int indexOfNewColumn = table.getIndexOfColumn(currentWordGroup.startX, currentWordGroup.endX);
                TableColumn tableColumn = new TableColumn(currentWordGroup.startX, currentWordGroup.endX, table.tableBody.sparseBlockLines.size());
                tableColumn.columnLines.get(lineIndex).words.addAll(currentWordGroup.words);
                table.tableColumns.add(indexOfNewColumn, tableColumn);
            }

            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    private void fixWrongColumnAlignments(Table table) {
        ArrayList<TableColumn> columnsToRemove = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < table.tableColumns.size(); columnIndex++) {
            TableColumn tableColumn = table.tableColumns.get(columnIndex);
            ArrayList<ColumnLine> filledColumnLines = new ArrayList<>();
            for (ColumnLine columnLine : tableColumn.columnLines) {
                if (columnLine.words.size() != 0 || columnLine.isSpanned) {
                    filledColumnLines.add(columnLine);
                }
            }
            if (filledColumnLines.size() == 1) {
                int columnLineIndex = tableColumn.columnLines.indexOf(filledColumnLines.get(0));
                if (columnIndex > 0 && table.tableColumns.get(columnIndex - 1).columnLines.get(columnLineIndex).words.size() == 0 && !table.tableColumns.get(columnIndex - 1).columnLines.get(columnLineIndex).isSpanned) {

                    table.tableColumns.get(columnIndex - 1).columnLines.get(columnLineIndex).words.addAll(tableColumn.columnLines.get(columnLineIndex).words);
                    tableColumn.columnLines.get(columnLineIndex).words.clear();
                    columnsToRemove.add(table.tableColumns.get(columnIndex));

                } else if (columnIndex < table.tableColumns.size() - 1 && table.tableColumns.get(columnIndex + 1).columnLines.get(columnLineIndex).words.size() == 0 && !table.tableColumns.get(columnIndex + 1).columnLines.get(columnLineIndex).isSpanned) {

                    table.tableColumns.get(columnIndex + 1).columnLines.get(columnLineIndex).words.addAll(tableColumn.columnLines.get(columnLineIndex).words);
                    tableColumn.columnLines.get(columnLineIndex).words.clear();
                    columnsToRemove.add(table.tableColumns.get(columnIndex));

                }
            }
        }

        for (TableColumn tableColumn : columnsToRemove) {
            table.tableColumns.remove(tableColumn);
        }
    }

}

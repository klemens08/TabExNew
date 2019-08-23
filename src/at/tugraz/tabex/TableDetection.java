package at.tugraz.tabex;

import javax.swing.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static at.tugraz.tabex.HelperFunctions.getMedianDistanceBetweenLines;

public class TableDetection {

    public MetaData metaData;

    public TableDetection(MetaData metaData) {
        this.metaData = metaData;
    }

    public void getMinimumSpaceGapThresholdByMedian(boolean singleColumn) {

        List<Float> distancesToNextWord = new ArrayList<>();

        if (singleColumn) {
            distancesToNextWord = getAllDistancesToNextWordSC();
        } else if (!singleColumn) {
            distancesToNextWord = getAllDistancesToNextWordMC();
        }
        Collections.sort(distancesToNextWord);
        //float median = distancesToNextWord.get(distancesToNextWord.size() / 2);
        //better suited for docs with just tables and nearly no text
        float median = distancesToNextWord.get(distancesToNextWord.size() / 2);

        //multiplication value has to be defined
        float multiplicationValue = 1.3f;
        if (!singleColumn) {
            multiplicationValue = 1.5f;
        }
        /*if (metaData.pages.size() == 1) {
            this.metaData.minimumSpaceGapThreshold = this.metaData.spacingTolerance * 5;
            return;
        }*/

        this.metaData.minimumSpaceGapThreshold = median * multiplicationValue;

    }

    public List<Float> getAllDistancesToNextWordSC() {

        ArrayList<Float> distancesToNextWord = new ArrayList<>();

        for (Page page : this.metaData.pages) {
            for (Line line : page.linesOfPage) {
                for (Word word : line.getWordsInLine()) {
                    if (word.distanceToNextWord == null) {
                        continue;
                    }
                    distancesToNextWord.add(word.getDistanceToNextWord());
                }
            }
        }
        return distancesToNextWord;
    }

    public List<Float> getAllDistancesToNextWordMC() {

        ArrayList<Float> distancesToNextWord = new ArrayList<>();

        for (Page page : this.metaData.pages) {
            for (Line line : page.linesOfPage) {
                for (Word word : line.getWordsInLine()) {
                    if (word.distanceToNextWord == null || (word.endX > metaData.startColumnBorder * 0.95 && word.endX < metaData.endColumnBorder * 1.05)) {
                        continue;
                    }
                    distancesToNextWord.add(word.getDistanceToNextWord());
                }
            }
        }
        return distancesToNextWord;
    }

    public void detectSparseLines() {

        //Line lastLineIfWasSparse = null;
        for (Page page : this.metaData.pages) {
            for (Line line : page.linesOfPage) {

                if (isBulletPoint(line.getWordsInLine().first())) {
                    continue;
                }

                //get Space Gap of current and last Word
                line.setSparsePointCount(this.metaData.minimumSpaceGapThreshold, metaData);

                if (metaData.isMultiColumnPDF) {
                    if (line.isCenterSparse) {
                        ArrayList<WordGroup> wordGroupsLeftColumn = new ArrayList<>();
                        ArrayList<WordGroup> wordGroupsRightColumn = new ArrayList<>();
                        for (WordGroup wordGroup : line.wordGroupsInLine) {
                            if (wordGroup.endX < metaData.startColumnBorder * 1.05) {
                                wordGroupsLeftColumn.add(wordGroup);
                            }
                            if (wordGroup.startX > metaData.endColumnBorder * 0.95) {
                                wordGroupsRightColumn.add(wordGroup);
                            }
                        }
                        if (wordGroupsLeftColumn.size() > 1) {
                            line.leftColumnSparse = true;
                        } else if (wordGroupsLeftColumn.size() > 0 && (wordGroupsLeftColumn.get(0).endX - wordGroupsLeftColumn.get(0).startX) < (metaData.medianWidthOfLines / 2) / 1.5) {
                            line.leftColumnCritical = true;
                        }
                        if (wordGroupsRightColumn.size() > 1) {
                            line.rightColumnSparse = true;
                        } else if (wordGroupsRightColumn.size() > 0 && (wordGroupsRightColumn.get(0).endX - wordGroupsRightColumn.get(0).startX) < (metaData.medianWidthOfLines / 2) / 1.5) {
                            line.rightColumnCritical = true;
                        }
                    }
                }
                //test 19.08
                //line.setWordGroupsInLine();

                if (!line.isCenterSparse || !metaData.isMultiColumnPDF) {
                    if (!line.isSparse && line.getWidth() < (metaData.medianWidthOfLines / 1.5)) {
                        line.isCritical = true;
                    }
                }

                //System.out.println("Linetext: " + line.getLineString());
                //System.out.println("IsSparse: " + line.isSparse);
            }
        }
        return;
    }

    public void detectSparseBlocks() {
        SparseBlock currentSparseBlock = new SparseBlock();
        SparseBlock currentMCLeftSparseBlock = new SparseBlock();
        currentMCLeftSparseBlock.isLeftColumn = true;
        SparseBlock currentMCRightSparseBlock = new SparseBlock();
        currentMCRightSparseBlock.isRightColumn = true;

        boolean possibleSCTableInMCFile = false;

        for (Page page : metaData.pages) {

            for (Line line : page.linesOfPage) {

                //if multi column pdf and line iscentersparse
                if (metaData.isMultiColumnPDF && line.isCenterSparse) {

                    //if normal sparse block was filled
                    currentSparseBlock = addSparseBlockToTable(currentSparseBlock);

                    if (line.leftColumnSparse && line.rightColumnSparse && !possibleSCTableInMCFile) {
                        if (currentMCLeftSparseBlock.sparseBlockLines.size() == 0 && currentMCLeftSparseBlock.sparseBlockLines.size() == 0) {
                            possibleSCTableInMCFile = true;
                        }
                    } else if (possibleSCTableInMCFile && !(shouldAddLineToLeftBlock(line, currentMCLeftSparseBlock) && shouldAddLineToRightBlock(line, currentMCRightSparseBlock))) {
                        possibleSCTableInMCFile = false;
                    } else if (possibleSCTableInMCFile && !shouldAddLineToLeftBlock(line, currentMCLeftSparseBlock) && !shouldAddLineToRightBlock(line, currentMCRightSparseBlock)) {
                        //da beide spalten eine Tabelle ergeben wird die linke seite hinzugefügt und initialisiert im funktionsaufruf (ganze zeilen) und die rechte neu initialisiert
                        //da beide bläcke die gesamten zeilen der tabelle beinhalten reicht es einen block hinzuzufügen und zu zeigen das die gesamte zeile zu betrachten ist ("istLeftColumn = false")
                        currentMCLeftSparseBlock.isLeftColumn = false;
                        currentMCLeftSparseBlock = addSparseBlockToTable(currentMCLeftSparseBlock);
                        currentMCRightSparseBlock = new SparseBlock();
                        possibleSCTableInMCFile = false;
                        continue;
                    }
                    if (line.leftColumnSparse) {
                        currentMCLeftSparseBlock.sparseBlockLines.add(line);
                        if (line.sparsePointCount > currentMCLeftSparseBlock.mostSparseFields) {
                            currentMCLeftSparseBlock.mostSparseFields = line.sparsePointCount;
                        }
                    } else if (line.leftColumnCritical && currentMCLeftSparseBlock.sparseBlockLines.size() > 0) {
                        currentMCLeftSparseBlock.sparseBlockLines.add(line);
                    } else {
                        if (currentMCLeftSparseBlock.sparseBlockLines.size() > 0 && line.wordGroupsInLeftColumn.size() > 0) {
                            currentMCLeftSparseBlock = addSparseBlockToTable(currentMCLeftSparseBlock);
                        }
                    }
                    if (line.rightColumnSparse) {
                        currentMCRightSparseBlock.sparseBlockLines.add(line);
                        if (line.sparsePointCount > currentMCRightSparseBlock.mostSparseFields) {
                            currentMCRightSparseBlock.mostSparseFields = line.sparsePointCount;
                        }
                    } else if (line.rightColumnCritical && currentMCRightSparseBlock.sparseBlockLines.size() > 0) {
                        currentMCRightSparseBlock.sparseBlockLines.add(line);
                    } else {
                        if (currentMCRightSparseBlock.sparseBlockLines.size() > 0 && line.wordGroupsInRightColumn.size() > 0) {
                            currentMCRightSparseBlock = addSparseBlockToTable(currentMCRightSparseBlock);
                        }
                    }
                } else {

                    //if left or right mc sparse block was filled
                    if (metaData.isMultiColumnPDF) {
                        if (!possibleSCTableInMCFile) {
                            currentMCLeftSparseBlock = addSparseBlockToTable(currentMCLeftSparseBlock);
                            currentMCRightSparseBlock = addSparseBlockToTable(currentMCRightSparseBlock);
                        } else {
                            currentMCLeftSparseBlock.isLeftColumn = false;
                            currentMCLeftSparseBlock = addSparseBlockToTable(currentMCLeftSparseBlock);
                            currentMCRightSparseBlock = new SparseBlock();
                        }
                        possibleSCTableInMCFile = false;
                    }
                    //single column line
                    if (line.isSparse && (!line.firstLineOfPage || line.firstLineOfPage && (line.next != null && (line.lineHeight == line.next.lineHeight))) && !line.lastLineOfPage && line.previous != null) {
                        currentSparseBlock.sparseBlockLines.add(line);
                        if (line.sparsePointCount > currentSparseBlock.mostSparseFields) {
                            currentSparseBlock.mostSparseFields = line.sparsePointCount;
                        }
                    } else if (line.isCritical && currentSparseBlock.sparseBlockLines.size() > 0 && (!line.firstLineOfPage || line.firstLineOfPage && (line.next != null && (line.lineHeight == line.next.lineHeight))) && !line.lastLineOfPage) {
                        currentSparseBlock.sparseBlockLines.add(line);
                    } else if (line.isCritical && (line.next != null && line.next.isSparse && line.lineHeight == line.next.lineHeight) && (!line.firstLineOfPage || line.firstLineOfPage && (line.next != null && (line.lineHeight == line.next.lineHeight))) && !line.lastLineOfPage) {
                        currentSparseBlock.sparseBlockLines.add(line);
                    } else {
                        currentSparseBlock = addSparseBlockToTable(currentSparseBlock);
                    }
                }
            }
            //if left or right mc sparse block was filled
            if (metaData.isMultiColumnPDF) {
                if (!possibleSCTableInMCFile) {
                    currentMCLeftSparseBlock = addSparseBlockToTable(currentMCLeftSparseBlock);
                    currentMCRightSparseBlock = addSparseBlockToTable(currentMCRightSparseBlock);
                } else {
                    currentMCLeftSparseBlock.isLeftColumn = false;
                    currentMCLeftSparseBlock = addSparseBlockToTable(currentMCLeftSparseBlock);
                }
            }
            currentSparseBlock = addSparseBlockToTable(currentSparseBlock);
        }
    }

    private boolean shouldAddLineToLeftBlock(Line line, SparseBlock currentMCLeftSparseBlock) {
        return line.leftColumnSparse || (line.leftColumnCritical && currentMCLeftSparseBlock.sparseBlockLines.size() > 0);
    }

    private boolean shouldAddLineToRightBlock(Line line, SparseBlock currentMCRightSparseBlock) {
        return line.rightColumnSparse || (line.rightColumnCritical && currentMCRightSparseBlock.sparseBlockLines.size() > 0);
    }

    private SparseBlock addSparseBlockToTable(SparseBlock currentSparseBlock) {

        if (currentSparseBlock.sparseBlockLines.size() > 1) {

            //for MC purposes create new lines
            if (currentSparseBlock.isLeftColumn || currentSparseBlock.isRightColumn) {
                SparseBlock sparseBlockTempMC = new SparseBlock();
                sparseBlockTempMC.isLeftColumn = currentSparseBlock.isLeftColumn;
                sparseBlockTempMC.isRightColumn = currentSparseBlock.isRightColumn;
                ArrayList<Line> linesToAdd = new ArrayList<>();
                for (Line line : currentSparseBlock.sparseBlockLines) {
                    Line newLine = createNewLineFromMCLine(line, currentSparseBlock.isLeftColumn, currentSparseBlock.isRightColumn);
                    linesToAdd.add(newLine);
                }
                currentSparseBlock.sparseBlockLines = linesToAdd;

                int curIndex = 0;
                for (Line line : currentSparseBlock.sparseBlockLines) {
                    //first Line in Sparse Block must point to original prev line
                    if (curIndex == 0) {
                        line.next = currentSparseBlock.sparseBlockLines.get(curIndex + 1);
                        line.setDistanceToNextLine();
                        curIndex++;
                        continue;
                    }
                    if (curIndex == currentSparseBlock.sparseBlockLines.size() - 1) {
                        line.previous = currentSparseBlock.sparseBlockLines.get(curIndex - 1);
                        line.setDistanceToPreviousLine();
                        curIndex++;
                        continue;
                    }
                    line.next = currentSparseBlock.sparseBlockLines.get(curIndex + 1);
                    line.previous = currentSparseBlock.sparseBlockLines.get(curIndex - 1);
                    line.setDistanceToNextLine();
                    line.setDistanceToPreviousLine();
                    curIndex++;
                }
            }


            if (currentSparseBlock.sparseBlockLines.size() == 2) {
                if (!(currentSparseBlock.sparseBlockLines.get(0).sparsePointCount == currentSparseBlock.sparseBlockLines.get(1).sparsePointCount)) {
                    return getSparseBlock(currentSparseBlock);
                }
            }
            if (currentSparseBlock.sparseBlockLines.size() == 3) {
                if (!(currentSparseBlock.sparseBlockLines.get(0).sparsePointCount == currentSparseBlock.sparseBlockLines.get(1).sparsePointCount) || !(currentSparseBlock.sparseBlockLines.get(0).sparsePointCount == currentSparseBlock.sparseBlockLines.get(2).sparsePointCount)) {
                    return getSparseBlock(currentSparseBlock);
                }
            }
            Table table = new Table();
            table.tableBody = currentSparseBlock;
            metaData.tables.add(table);
        }
        return getSparseBlock(currentSparseBlock);
    }

    private SparseBlock getSparseBlock(SparseBlock currentSparseBlock) {
        boolean isLeftColumn = currentSparseBlock.isLeftColumn;
        boolean isRightColumn = currentSparseBlock.isRightColumn;
        currentSparseBlock = new SparseBlock();
        currentSparseBlock.isRightColumn = isRightColumn;
        currentSparseBlock.isLeftColumn = isLeftColumn;
        return currentSparseBlock;
    }

    //create a new MC Line from a centersparse MC Line (left or right half)
    private Line createNewLineFromMCLine(Line originalLine, boolean left, boolean right) {

        Line newLine = new Line();


        if (left) {
            newLine.wordGroupsInLine = originalLine.wordGroupsInLeftColumn;
            newLine.isSparse = originalLine.leftColumnSparse;
            newLine.isCritical = originalLine.leftColumnCritical;
        }
        if (right) {
            newLine.wordGroupsInLine = originalLine.wordGroupsInRightColumn;
            newLine.isSparse = originalLine.rightColumnSparse;
            newLine.isCritical = originalLine.rightColumnCritical;
        }

        newLine.lineNumber = originalLine.lineNumber;
        newLine.page = originalLine.page;
        newLine.previous = originalLine.previous;
        newLine.next = originalLine.next;
        newLine.sparsePointCount = newLine.wordGroupsInLine.size() - 1;
        newLine.firstLineOfPage = originalLine.firstLineOfPage;
        newLine.lastLineOfPage = originalLine.lastLineOfPage;

        Float lineUpperBound = 0.0f;
        Float lineLowerBound = 0.0f;

        for (WordGroup wordGroup : newLine.wordGroupsInLine) {
            for (Word word : wordGroup.words) {
                if (lineLowerBound == 0.0f) {
                    lineLowerBound = word.lowerBound;
                }
                if (lineUpperBound == 0.0f) {
                    lineUpperBound = word.upperBound;
                }
                if (lineLowerBound < word.lowerBound) {
                    lineLowerBound = word.lowerBound;
                }
                if (lineUpperBound > word.upperBound) {
                    lineUpperBound = word.upperBound;
                }
                newLine.wordsInLine.add(word);
            }
        }

        newLine.upperBound = lineUpperBound;
        newLine.lowerBound = lineLowerBound;
        newLine.setLineHeight();

        return newLine;
    }

    public boolean isBulletPoint(Word word) {
        ArrayList<String> bulletPointUnicodes = new ArrayList<String>() {{
            add("\\u2022");
            add("\\u2023");
            add("\\u25E6");
            add("\\u2043");
            add("\\u2219");
            add("\\u2212");
            add("\\u2012");
            add("\\u2013");
            add("\\u25CB");
            add("\\u006f");
            add("\\u002d");
            add("\\u25A0"); // -
        }};
        String unicode = word.lettersOfWord.get(0).unicode;
        if (bulletPointUnicodes.contains(unicode) && word.lettersOfWord.size() == 1) {
            return true;
        }

        // 1) b) 1. a. (a & a.isNextSparse) 1:
        //TODO: pattern for (1) (2) [1] [2]
        Pattern pattern = Pattern.compile("^([^\\(\\s]{1,3}[\\)\\.\\:]){1,5}$");
        Pattern pattern2 = Pattern.compile("^\\[[0-9]{1}\\]");
        Pattern pattern3 = Pattern.compile("^\\[[0-9]{2}\\]");
        Pattern pattern4 = Pattern.compile("^\\[[0-9]{3}\\]");

        Matcher matcher = pattern.matcher(word.getWordString());
        if (matcher.find()) {
            return true;
        }
        Matcher matcher2 = pattern2.matcher(word.getWordString());
        if (matcher2.find()) {
            return true;
        }
        Matcher matcher3 = pattern3.matcher(word.getWordString());
        if (matcher3.find()) {
            return true;
        }
        Matcher matcher4 = pattern4.matcher(word.getWordString());
        if (matcher4.find()) {
            return true;
        }

        // a
        /*if (word.lettersOfWord.size() == 1 && word.isNextSparse) {
            return true;
        }*/

        /*Pattern pattern2 = Pattern.compile("^\\w[^\\S ]+");
        Matcher matcher2 = pattern2.matcher(word.getWordString());
        if (matcher2.find()) {
            return true;
        }*/

        return false;
    }

    public void reviseSparseBlocks() {

        ArrayList<Table> tablesToDelete = new ArrayList<>();

        //delete lines from start and end of sparse block if the size difference is too high
        for (Table table : metaData.tables) {
            trimSparseBlocks(table, tablesToDelete);
        }

        metaData.tables.removeAll(tablesToDelete);

        //split sparse blocks if line distances in between are too high
        HashMap<Table, ArrayList<Table>> splitTableDict = new HashMap<Table, ArrayList<Table>>();
        for (Table table : metaData.tables) {
            ArrayList<Table> newTables = splitSparseBlocks(table);
            if (newTables.size() > 0) {
                splitTableDict.put(table, newTables);
            }
        }

        //delete splitted table and save new tables
        for (Table tableToDelete : splitTableDict.keySet()) {
            int indexOfTableToDelete = metaData.tables.indexOf(tableToDelete);
            metaData.tables.addAll(indexOfTableToDelete, splitTableDict.get(tableToDelete));
            metaData.tables.remove(tableToDelete);
        }

        for (Table table : metaData.tables) {
            if (!evaluateSparseBlockByLines(table)) {
                tablesToDelete.add(table);
            }
        }
        metaData.tables.removeAll(tablesToDelete);

        //delete table if: not the same sparse point count, not similiar line height, 1 sparse point that dont overlap
        for (Table table : metaData.tables) {
            if (table.tableBody.sparseBlockLines.size() == 2) {
                if (table.tableBody.sparseBlockLines.get(0).sparsePointCount != table.tableBody.sparseBlockLines.get(1).sparsePointCount) {
                    tablesToDelete.add(table);
                } else if (table.tableBody.sparseBlockLines.get(0).lineHeight > table.tableBody.sparseBlockLines.get(1).lineHeight * 1.3 || table.tableBody.sparseBlockLines.get(0).lineHeight * 1.3 < table.tableBody.sparseBlockLines.get(1).lineHeight) {
                    tablesToDelete.add(table);
                } else if (table.tableBody.sparseBlockLines.get(0).sparsePointCount == 1 && table.tableBody.sparseBlockLines.get(1).sparsePointCount == 1) {
                    if (table.tableBody.sparseBlockLines.get(0).getWordGroupsInLine().get(0).endX > table.tableBody.sparseBlockLines.get(1).getWordGroupsInLine().get(1).startX
                            || table.tableBody.sparseBlockLines.get(1).getWordGroupsInLine().get(0).endX > table.tableBody.sparseBlockLines.get(0).getWordGroupsInLine().get(1).startX) {
                        tablesToDelete.add(table);
                    }
                }
            }
        }
        metaData.tables.removeAll(tablesToDelete);

        /*//check if at least 50% of table lines have same sparse point count
        for (Table table : metaData.tables) {
            HashMap<Integer, Integer> sparsePointsOfTable = new HashMap<>();
            for (Line line : table.tableBody.sparseBlockLines) {
                if(line.isCritical){
                    continue;
                }
                if (sparsePointsOfTable.containsKey(line.sparsePointCount)) {
                    sparsePointsOfTable.put(line.sparsePointCount, sparsePointsOfTable.get(line.sparsePointCount) + 1);
                } else {
                    sparsePointsOfTable.put(line.sparsePointCount, 1);
                }
            }
            boolean tableToDelete = true;
            List<Line> sparseLinesOfTable = table.tableBody.sparseBlockLines.stream().filter(l -> l.isCritical == false).collect(Collectors.toList());
            for (Iterator it = sparsePointsOfTable.keySet().iterator(); it.hasNext(); ) {
                Object key = it.next();
                Integer value = sparsePointsOfTable.get(key);
                if (value >= (sparseLinesOfTable.size() / 2.0f)) {
                    tableToDelete = false;
                    break;
                }
            }
            if (tableToDelete) {
                tablesToDelete.add(table);
            }
        }
        metaData.tables.removeAll(tablesToDelete);*/

        //if 2 line table does not full overlap for every sparse area
        for (Table table : metaData.tables) {
            if (table.tableBody.sparseBlockLines.size() == 2) {
                if (!fullOverlapOfSparseAreas(table.tableBody.sparseBlockLines.get(0), table.tableBody.sparseBlockLines.get(1))) {
                    tablesToDelete.add(table);
                }
            }
        }
        metaData.tables.removeAll(tablesToDelete);

    }

    private boolean fullOverlapOfSparseAreas(Line firstLine, Line secondLine) {

        int count = 0;
        for (WordGroup wordGroupFirstLine : firstLine.wordGroupsInLine) {
            if (wordGroupFirstLine.startX > secondLine.wordGroupsInLine.get(count).endX || wordGroupFirstLine.endX < secondLine.wordGroupsInLine.get(count).startX) {
                return false;
            }
            count++;
        }
        return true;
    }

    private boolean evaluateSparseBlockByLines(Table table) {

        //remove critical lines at start of sparse block after splitting
        ArrayList<Line> linesToRemove = new ArrayList<>();
        for (Line line : table.tableBody.sparseBlockLines) {
            if (!line.isSparse && !(line.lineHeight == line.next.lineHeight)) {
                linesToRemove.add(line);
            } else {
                break;
            }
        }
        table.tableBody.sparseBlockLines.removeAll(linesToRemove);

        if (table.tableBody.sparseBlockLines.size() < 2) {
            return false;
        }
        if (table.tableBody.sparseBlockLines.size() == 2) {
            if (!(table.tableBody.sparseBlockLines.get(0).sparsePointCount == table.tableBody.sparseBlockLines.get(1).sparsePointCount)) {
                return false;
            }
        }
        if (table.tableBody.sparseBlockLines.size() == 3) {
            if (!(table.tableBody.sparseBlockLines.get(0).sparsePointCount == table.tableBody.sparseBlockLines.get(1).sparsePointCount) || !(table.tableBody.sparseBlockLines.get(0).sparsePointCount == table.tableBody.sparseBlockLines.get(2).sparsePointCount)) {
                return false;
            }
        }
        return true;
    }

    private ArrayList<Table> splitSparseBlocks(Table table) {
        ArrayList<Table> newTables = new ArrayList<>();
        Table currentTable = new Table();

        Float medianDistanceToNextLine = getMedianDistanceBetweenLines(table.tableBody.sparseBlockLines);

        for (Line line : table.tableBody.sparseBlockLines) {
            currentTable.tableBody.sparseBlockLines.add(line);
            if (line.distanceToNextLine > medianDistanceToNextLine * 2 && table.tableBody.sparseBlockLines.indexOf(line) < table.tableBody.sparseBlockLines.size() - 3
                    || line.distanceToNextLine > medianDistanceToNextLine * 2.5) { //value -3 defines the amount of lines that have to be behind a big line distance to avoid unnecessary splits
                if (line.sparsePointCount < line.next.sparsePointCount - 2 || line.sparsePointCount > line.next.sparsePointCount + 2 || line.next.lineHeight != line.lineHeight) {

                    if (currentTable.tableBody.sparseBlockLines.size() > 1) {
                        newTables.add(currentTable);
                    }
                    currentTable = new Table();
                }

            }
        }
        if (newTables.size() > 0 && currentTable.tableBody.sparseBlockLines.size() > 2 || (currentTable.tableBody.sparseBlockLines.size() > 1 && (currentTable.tableBody.sparseBlockLines.get(0).sparsePointCount == currentTable.tableBody.sparseBlockLines.get(0).sparsePointCount))) {
            newTables.add(currentTable);
        }
        return newTables;
    }

    public void trimSparseBlocks(Table table, ArrayList<Table> tablesToDelete) {
        Float medianLineHeight = HelperFunctions.getMedianLineHeight(table.tableBody.sparseBlockLines);
        Float medianDistanceToNextLine = getMedianDistanceBetweenLines(table.tableBody.sparseBlockLines);


        ArrayList<Line> linesToDelete = new ArrayList<>();
        Line currentLine = table.tableBody.sparseBlockLines.get(0);

        //System.out.println(currentLine.getLineString());
        //System.out.println(metaData.fileName);
        while ((currentLine.lineHeight > medianLineHeight * 1.3 || currentLine.distanceToNextLine > medianDistanceToNextLine * 3) && (currentLine.sparsePointCount != currentLine.next.sparsePointCount)) {
            linesToDelete.add(currentLine);
            currentLine = currentLine.next;
        }

        currentLine = table.tableBody.sparseBlockLines.get(table.tableBody.sparseBlockLines.size() - 1);

        while (currentLine.lineHeight > medianLineHeight * 1.3 || currentLine.previous.distanceToNextLine > medianDistanceToNextLine * 2.5 && (currentLine.sparsePointCount != currentLine.previous.sparsePointCount)) {
            linesToDelete.add(currentLine);
            currentLine = currentLine.previous;
        }

        for (Line line : linesToDelete) {
            table.tableBody.sparseBlockLines.remove(line);
        }

        if (table.tableBody.sparseBlockLines.size() < 2 || (table.tableBody.sparseBlockLines.size()) < 3 && (table.tableBody.sparseBlockLines.get(0).sparsePointCount != table.tableBody.sparseBlockLines.get(0).sparsePointCount)) {
            //delete table
            tablesToDelete.add(table);
        }
    }
}

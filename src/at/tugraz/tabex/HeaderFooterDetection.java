package at.tugraz.tabex;

import java.util.ArrayList;

public class HeaderFooterDetection {


    static void detectHeaderByKeyword(Table table) {
        Line firstLine = table.tableBody.sparseBlockLines.get(0);

        if (firstLine.previous == null || firstLine.page != firstLine.previous.page) {
            return;
        }
        if (checkLineIfHeaderByKeyword(firstLine.previous)) {
            table.Header.add(firstLine.previous);
            return;
        }
        if (firstLine.previous.previous == null || firstLine.page != firstLine.previous.previous.page) {
            return;
        }
        if (checkLineIfHeaderByKeyword(firstLine.previous.previous)) {
            table.Header.add(firstLine.previous.previous);
            table.Header.add(firstLine.previous);
            return;
        }
        if (firstLine.previous.previous.previous == null || firstLine.page != firstLine.previous.previous.previous.page) {
            return;
        }
        if (checkLineIfHeaderByKeyword(firstLine.previous.previous.previous)) {
            table.Header.add(firstLine.previous.previous.previous);
            table.Header.add(firstLine.previous.previous);
            table.Header.add(firstLine.previous);
            return;
        }
        if (firstLine.previous.previous.previous.previous == null || firstLine.page != firstLine.previous.previous.previous.previous.page) {
            return;
        }
        if (checkLineIfHeaderByKeyword(firstLine.previous.previous.previous.previous)) {
            table.Header.add(firstLine.previous.previous.previous.previous);
            table.Header.add(firstLine.previous.previous.previous);
            table.Header.add(firstLine.previous.previous);
            return;
        }
        for (Line line : table.Header) {
            line.isHeaderLine = true;
        }
    }


    public static void detectFooterByLineDistances(Table table, MetaData metaData) {

        Line lastLineOfTable = table.tableBody.sparseBlockLines.get(table.tableBody.sparseBlockLines.size() - 1);
        Line currentLineToBeAnalyzed = null;
        int linesAnalyzed = 0;
        ArrayList<Line> alreadyAnalyzedLines = new ArrayList<>();


        //max 6 lines to be footer
        while (linesAnalyzed < 6) {
            //first line after table
            if (currentLineToBeAnalyzed == null && lastLineOfTable.next != null && !lastLineOfTable.next.isTableLine && !lastLineOfTable.next.isHeaderLine && !lastLineOfTable.next.lastLineOfPage) {
                currentLineToBeAnalyzed = lastLineOfTable.next;
            } else if (currentLineToBeAnalyzed == null) {
                break;
            }
            //if line distance is higher than median line distance --> footer found
            else if (currentLineToBeAnalyzed.distanceToNextLine > metaData.medianYLineSpace * 1.3f) {
                alreadyAnalyzedLines.add(currentLineToBeAnalyzed);
                table.Footer.addAll(alreadyAnalyzedLines);
                for (Line line : alreadyAnalyzedLines) {
                    line.isFooterLine = true;
                }
                break;
                //current line not conspicuous
            } else {
                //no line after current one
                if (currentLineToBeAnalyzed.next == null || currentLineToBeAnalyzed.next.isTableLine || currentLineToBeAnalyzed.next.isHeaderLine || currentLineToBeAnalyzed.next.lastLineOfPage) {
                    break;
                }
                alreadyAnalyzedLines.add(currentLineToBeAnalyzed);
                currentLineToBeAnalyzed = currentLineToBeAnalyzed.next;
                linesAnalyzed++;
            }
        }
    }

    //check if the lines before a table have bigger distances from each other than average in the document, if so --> header
    public static void detectHeaderByLineDistances(Table table, MetaData metaData) {

        Line firstLineOfTable = table.tableBody.sparseBlockLines.get(0);
        Line currentLineToBeAnalyzed = null;
        int linesAnalyzed = 0;
        ArrayList<Line> alreadyAnalyzedLines = new ArrayList<>();


        //max 6 lines to be header
        while (linesAnalyzed < 6) {
            //first line before table
            if (currentLineToBeAnalyzed == null && firstLineOfTable.previous != null && !firstLineOfTable.previous.isTableLine && !firstLineOfTable.previous.isFooterLine
                    && !(firstLineOfTable.previous.firstLineOfPage && firstLineOfTable.previous.wordsInLine.size() == 1) && !firstLineOfTable.previous.lastLineOfPage) {
                currentLineToBeAnalyzed = firstLineOfTable.previous;
            } else if (currentLineToBeAnalyzed == null) {
                break;
            }
            //if line distance is higher than median line distance --> header found
            else if (currentLineToBeAnalyzed.previous != null && currentLineToBeAnalyzed.distanceToPreviousLine > metaData.medianYLineSpace * 1.3f || checkLineIfHeaderByKeyword(currentLineToBeAnalyzed) || (currentLineToBeAnalyzed.firstLineOfPage && currentLineToBeAnalyzed.wordsInLine.size() != 1)) {
                alreadyAnalyzedLines.add(currentLineToBeAnalyzed);
                for (Line line : alreadyAnalyzedLines) {
                    table.Header.add(0, line);
                }
                //table.Header.addAll(alreadyAnalyzedLines);
                for (Line line : alreadyAnalyzedLines) {
                    line.isHeaderLine = true;
                }
                break;
                //current line not conspicuous
            } else {
                //no line before current one
                if (currentLineToBeAnalyzed.previous == null || currentLineToBeAnalyzed.previous.isTableLine || currentLineToBeAnalyzed.previous.isFooterLine
                        || (firstLineOfTable.previous.firstLineOfPage && firstLineOfTable.previous.wordsInLine.size() == 1) || currentLineToBeAnalyzed.previous.lastLineOfPage) {
                    break;
                }
                alreadyAnalyzedLines.add(currentLineToBeAnalyzed);
                currentLineToBeAnalyzed = currentLineToBeAnalyzed.previous;
                linesAnalyzed++;
            }
        }
    }

    public static void detectFootersByKeyword(Table table) {
        Line lastLine = table.tableBody.sparseBlockLines.get(table.tableBody.sparseBlockLines.size() - 1);

        if (lastLine.next == null || lastLine.next.page != lastLine.page || lastLine.next.lastLineOfPage == true) {
            return;
        }
        if (checkLineIfFooterByKeyword(lastLine.next) && !lastLine.isHeaderLine) {
            table.Footer.add(lastLine.next);
            return;
        }
        if (lastLine.next.next == null || lastLine.next.next.page != lastLine.page || lastLine.next.next.lastLineOfPage == true) {
            return;
        }
        if (checkLineIfFooterByKeyword(lastLine.next.next) && !lastLine.isHeaderLine) {
            table.Footer.add(lastLine.next.next);
            table.Footer.add(lastLine.next);
            return;
        }
        if (lastLine.next.next.next == null || lastLine.next.next.next.page != lastLine.page || lastLine.next.next.next.lastLineOfPage == true) {
            return;
        }
        if (checkLineIfFooterByKeyword(lastLine.next.next.next) && !lastLine.isHeaderLine) {
            table.Footer.add(lastLine.next.next.next);
            table.Footer.add(lastLine.next.next);
            table.Footer.add(lastLine.next);
            return;
        }
        for (Line line : table.Footer) {
            line.isFooterLine = true;
        }
    }

    static boolean checkLineIfHeaderByKeyword(Line line) {
        if (line.wordsInLine.first().getWordString().toLowerCase().startsWith("table") ||
                line.wordsInLine.first().getWordString().toLowerCase().startsWith("figure") ||
                line.wordsInLine.first().getWordString().toLowerCase().startsWith("source")) {
            if (!line.isFooterLine) {
                return true;
            }
        }
        return false;
    }

    static boolean checkLineIfFooterByKeyword(Line line) {
        if (line.wordsInLine.first().getWordString().toLowerCase().startsWith("table") || line.wordsInLine.first().getWordString().startsWith("figure")
                || line.wordsInLine.first().getWordString().toLowerCase().startsWith("source") || line.wordsInLine.first().getWordString().toLowerCase().startsWith("*")) {
            if (!line.isFooterLine) {
                return true;
            }
        }
        return false;
    }

}

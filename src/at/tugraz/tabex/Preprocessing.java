package at.tugraz.tabex;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Preprocessing extends PDFTextStripper {

    private PDDocument document;

    public MetaData metaData;

    public Preprocessing(MetaData metaData, File inputFile) throws IOException {
        this.metaData = metaData;

        extractPositionOfText(inputFile);
        setPageData();
        setLineNextPreviousLast();
        metaData.setMedianWidthOfLines();
        metaData.setMaxWidthOfLines();
        setDistanceToPreviousAndNextLine();
        metaData.setMedianYLineSpace();

    }

    private void setPageData() {
        for (int key : metaData.letterDict.keySet()) {

            if (metaData.letterDict.get(key).size() == 0) {
                continue;
            }

            ArrayList<Word> wordsOfPage = getWordsOfPage(metaData.letterDict.get(key));
            metaData.getPage(key).setPageBoundaries(wordsOfPage);
            CustomList<Line> linesOfPage = getLinesOfPage(wordsOfPage, key);

            //set first and last line of page
            Line tempLine = new Line();
            tempLine.upperBound = Float.POSITIVE_INFINITY;
            tempLine.lowerBound = Float.NEGATIVE_INFINITY;
            Line firstLine = tempLine;
            Line lastLine = tempLine;
            for (Line line : linesOfPage) {
                line.page = key;
                line.setLineHeight();
                if (line.upperBound < firstLine.upperBound) {
                    firstLine = line;
                }
                if (line.lowerBound > lastLine.lowerBound) {
                    lastLine = line;
                }
            }
            firstLine.firstLineOfPage = true;
            lastLine.lastLineOfPage = true;

            correctWrongWordPositionsInLine(linesOfPage);

            for (Line line : linesOfPage) {
                System.out.println(line.getLineString());
            }

            setWordNextPreviousLast(linesOfPage);

            //linesOfPage.first().firstLineOfPage = true;
            //linesOfPage.last().lastLineOfPage = true;
            this.metaData.getPage(key).linesOfPage.addAll(linesOfPage);

            if (key == 1 && metaData.letterDict.keySet().size() > 1) {
                metaData.getPage(key).next = metaData.getPage(key + 1);
            } else if (key == metaData.letterDict.keySet().size()) {
                metaData.getPage(key).previous = metaData.getPage(key - 1);
            } else {
                metaData.getPage(key).next = metaData.getPage(key + 1);
                metaData.getPage(key).previous = metaData.getPage(key - 1);
            }
        }
    }

    public static boolean isPageMultiColumn(CustomList<Line> linesOfPage, Page page) {

        ArrayList<Line> centerSparseLines = new ArrayList<>();

        Float leftBoundaryOfPage = Float.POSITIVE_INFINITY;
        Float rightBoundaryOfPage = Float.NEGATIVE_INFINITY;
        ArrayList<Float> leftBoundariesOfPage = new ArrayList<>();
        ArrayList<Float> rightBoundariesOfPage = new ArrayList<>();

        for (Line line : linesOfPage) {
            leftBoundariesOfPage.add(line.getStartX());
            rightBoundariesOfPage.add(line.getEndX());
            if (line.getStartX() < leftBoundaryOfPage) {
                leftBoundaryOfPage = line.getStartX();
            }
            if (line.getEndX() > rightBoundaryOfPage) {
                rightBoundaryOfPage = line.getEndX();
            }
        }

        Collections.sort(leftBoundariesOfPage);
        Collections.sort(rightBoundariesOfPage);

        if (leftBoundariesOfPage.size() < 3 || rightBoundariesOfPage.size() < 3) {
            return false;
        }

        leftBoundaryOfPage = leftBoundariesOfPage.get(leftBoundariesOfPage.size() / 10);
        rightBoundaryOfPage = rightBoundariesOfPage.get(rightBoundariesOfPage.size() - rightBoundariesOfPage.size() / 10 - 1);

        Float centerOfPage = rightBoundaryOfPage - ((rightBoundaryOfPage - leftBoundaryOfPage) / 2);

        page.centerOfPage = centerOfPage;

        ArrayList<Float> leftColumnRightBoundaries = new ArrayList<>();
        ArrayList<Float> rightColumnLeftBoundaries = new ArrayList<>();

        for (Line line : linesOfPage) {
            for (WordGroup wordGroup : line.wordGroupsInLine) {

                //check if currentwordgroup ends before center of page and next starts after center of page
                if (wordGroup.next != null && wordGroup.endX < centerOfPage && wordGroup.next.startX > centerOfPage) {

                    //check if either the end of the current wordgroup or the start of the following is in a certain range
                    if (wordGroup.endX > centerOfPage * 0.85 || wordGroup.next.startX < centerOfPage * 1.15) {
                        line.isCenterSparse = true;
                        centerSparseLines.add(line);
                        leftColumnRightBoundaries.add(wordGroup.endX);
                        rightColumnLeftBoundaries.add(wordGroup.next.startX);
                        break;
                    }
                }
            }

            //check if the end of the line is before the center  || or the start of the line is after the center and inside a certain range
            if (!centerSparseLines.contains(line) && (line.getEndX() < centerOfPage || (line.getStartX() > centerOfPage && line.getStartX() < centerOfPage * 1.3))) {
                line.isCenterSparse = true;
                centerSparseLines.add(line);
                if (line.getEndX() < centerOfPage) {
                    leftColumnRightBoundaries.add(line.getEndX());
                } else if ((line.getStartX() > centerOfPage && line.getStartX() < centerOfPage * 1.1)) {
                    rightColumnLeftBoundaries.add(line.getStartX());
                }
            }
        }

        if (centerSparseLines.size() < 2) {
            return false;
        }

        Collections.sort(leftColumnRightBoundaries);
        Collections.sort(rightColumnLeftBoundaries);

        //Median end of right and begin of left column

        if (leftColumnRightBoundaries.size() < 2 || rightColumnLeftBoundaries.size() < 2) {
            return false;
        }

        Float leftColumnRightBoundary = leftColumnRightBoundaries.get(leftColumnRightBoundaries.size() - leftColumnRightBoundaries.size() / 10 - 1);
        Float rightColumnLeftBoundary = rightColumnLeftBoundaries.get(rightColumnLeftBoundaries.size() / 10);
        int fullLeftColumnLines = 0;
        int fullRightColumnLines = 0;

        for (Line line : centerSparseLines) {
            for (WordGroup wordGroup : line.wordGroupsInLine) {
                if ((wordGroup.endX > leftColumnRightBoundary * 0.95 && wordGroup.endX < leftColumnRightBoundary * 1.05)
                        && (line.wordGroupsInLine.get(0).startX > leftBoundaryOfPage * 0.95 && line.wordGroupsInLine.get(0).startX < leftBoundaryOfPage * 1.05)) {
                    fullLeftColumnLines++;
                } else if (wordGroup.startX > rightColumnLeftBoundary * 0.95 && wordGroup.startX < rightColumnLeftBoundary * 1.05) {
                    fullRightColumnLines++;
                }

            }
        }

        if (centerSparseLines.size() > (linesOfPage.size() * 0.6) && (fullLeftColumnLines > (centerSparseLines.size() * 0.3) /*&& fullRightColumnLines > (centerSparseLines.size() * 0.3)*/)) {
            page.startColumnBorder = leftColumnRightBoundary;
            page.endColumnBorder = rightColumnLeftBoundary;
            return true;
        }

        return false;
    }

    private void correctWrongWordPositionsInLine(CustomList<Line> linesOfPage) {
        for (Line line : linesOfPage) {
            Line tempLine = new Line();
            for (Word word : line.getWordsInLine()) {
                //first word in line
                if (tempLine.getWordsInLine().size() == 0) {
                    tempLine.wordsInLine.add(word);
                    continue;
                } //current last word in Line (normal case)
                else if (word.startX > tempLine.wordsInLine.get(tempLine.wordsInLine.size() - 1).endX) {
                    tempLine.wordsInLine.add(word);
                } //current word is positioned anywhere in line
                else {
                    for (Word wordOfTempLine : tempLine.getWordsInLine()) {
                        if (word.endX < wordOfTempLine.startX) {
                            tempLine.wordsInLine.add(tempLine.wordsInLine.indexOf(wordOfTempLine), word);
                            break;
                        }
                    }
                }
            }
            line.wordsInLine.clear();
            line.wordsInLine.addAll(tempLine.wordsInLine);
        }
    }

    private void setWordNextPreviousLast(CustomList<Line> linesOfPage) {
        for (Line line : linesOfPage) {
            int currentIndex = 0;
            for (Word word : line.wordsInLine) {
                //if line has one word
                if (line.wordsInLine.size() == 1 && currentIndex == 0) {
                    word.firstInLine = true;
                    word.lastInLine = true;
                    word.distanceToNextWord = null;
                    break;
                    //first word in line
                } else if (currentIndex == 0) {
                    word.next = line.wordsInLine.get(currentIndex + 1);
                    word.firstInLine = true;
                    word.distanceToNextWord = word.next.startX - word.endX;
                    currentIndex++;
                    continue;
                    //last word in line
                } else if (currentIndex == line.wordsInLine.size() - 1) {
                    word.previous = line.wordsInLine.get(currentIndex - 1);
                    word.lastInLine = true;
                    word.previous.distanceToNextWord = word.startX - word.previous.endX;
                    break;
                    //every other word in line
                } else {
                    word.previous = line.wordsInLine.get(currentIndex - 1);
                    word.next = line.wordsInLine.get(currentIndex + 1);
                    word.distanceToNextWord = word.next.startX - word.endX;
                    word.previous.distanceToNextWord = word.startX - word.previous.endX;
                    currentIndex++;
                }
            }
        }
    }

    //set line previous and next && mark last/first line of page
    public void setLineNextPreviousLast() {

        for (Page page : metaData.pages) {
            int currentIndex = 0;
            for (Line line : page.linesOfPage) {
                //first line of first page
                if (page.pageNumber == 1 && currentIndex == 0) {
                    line.next = page.linesOfPage.get(currentIndex + 1);
                    line.firstLineOfPage = true;
                    currentIndex++;
                    continue;
                    //last line of last page
                } else if (metaData.pages.last().pageNumber == page.pageNumber && currentIndex == page.linesOfPage.size() - 1) {
                    line.previous = page.linesOfPage.get(currentIndex - 1);
                    line.lastLineOfPage = true;
                    break;
                    //first line of any page except first one
                } else if (currentIndex == 0) {
                    line.previous = page.previous.linesOfPage.last();
                    if (page.linesOfPage.size() == currentIndex + 1) {
                        if (page.next != null) {
                            line.next = page.next.firstLineOfPage;
                        } else {
                            line.next = null;
                        }
                        break;
                    }
                    line.next = page.linesOfPage.get(currentIndex + 1);
                    currentIndex++;
                    //last line of any page except last one
                } else if (currentIndex == page.linesOfPage.size() - 1) {
                    line.next = page.next.linesOfPage.first();
                    line.previous = page.linesOfPage.get(currentIndex - 1);
                    page.next.linesOfPage.first().previous = line;
                    //any other line in any page
                } else {
                    line.previous = page.linesOfPage.get(currentIndex - 1);
                    line.next = page.linesOfPage.get(currentIndex + 1);
                    currentIndex++;
                }
                //todo: handle overlapping lines (file 11 table 1)
                /*if (line.upperBound < line.previous.lowerBound && line.lowerBound < line.previous.lowerBound) {
                    line.firstLineOfPage = true;
                    line.previous.lastLineOfPage = true;
                }*/
            }
        }


    }

    private CustomList<Line> getLinesOfPage(ArrayList<Word> words, int pageNumber) {
        Line currentLine = new Line();
        Float pageHeight = metaData.getPage(pageNumber).lowerBound - metaData.getPage(pageNumber).upperBound;
        CustomList<Line> lines = new CustomList<>();

        for (Word word : words) {
            //first Line of Page
            if (currentLine.wordsInLine.size() == 0) {
                currentLine = new Line();
                currentLine.upperBound = word.upperBound;
                currentLine.lowerBound = word.lowerBound;
                currentLine.wordsInLine.add(word);

                //check if lines overlap
            } else if (checkIfOverlaps(currentLine.lowerBound, currentLine.upperBound, word.lowerBound, word.upperBound)) {
                currentLine.wordsInLine.add(word);
                adaptBoundaries(word, currentLine);

                //check if word is above currentLine --> add it to suitable line
            } else if (word.lowerBound < currentLine.upperBound) {
                if (lines.size() == 0) {
                    createNewLineWithBoundaries(lines, word);
                    continue;
                }

                boolean lineFound = false;
                for (Line line : lines) {
                    if (checkIfOverlaps(line.lowerBound, line.upperBound, word.lowerBound, word.upperBound)) {
                        line.wordsInLine.add(word);
                        adaptBoundaries(word, line);
                        updateWordDistances(line, word);
                        lineFound = true;
                        break;
                    }
                    continue;
                }
                //if word is above current line but no suitable line was found --> create new line and add in correct position
                if (!lineFound) {
                    Line prevLine = new Line();
                    for (Line line : lines) {
                        if (lines.size() == 1) {
                            Line newLine = createLine(word);
                            if (word.upperBound > line.lowerBound) {
                                lines.add(newLine);
                                lineFound = true;
                                break;
                            } else {
                                lines.add(lines.indexOf(line), newLine);
                                lineFound = true;
                                break;
                            }
                        }
                        if (prevLine.wordsInLine.size() == 0) {
                            prevLine = line;
                            continue;
                        }
                        if (word.upperBound > prevLine.lowerBound && word.lowerBound < line.upperBound) {
                            Line newLine = createLine(word);
                            lines.add(lines.indexOf(line), newLine);
                            lineFound = true;
                            break;
                        }
                    }
                    if (!lineFound) {
                        createNewLineWithBoundaries(lines, word);
                    }
                    continue;
                }

                //if current word is below currentLine --> add new line to page
            } else {
                currentLine = createNewLine(currentLine, lines, word);
                updateWordDistances(currentLine, word);
            }
        }
        if (currentLine.wordsInLine.size() > 0 && !lines.contains(currentLine)) {
            lines.add(currentLine);
        }
        return lines;
    }

    private Line createLine(Word word) {
        Line newLine = new Line();
        newLine.upperBound = word.upperBound;
        newLine.lowerBound = word.lowerBound;
        newLine.wordsInLine.add(word);
        return newLine;
    }

    private Line createNewLine(Line currentLine, CustomList<Line> lines, Word word) {
        lines.add(currentLine);
        currentLine = new Line();
        currentLine.upperBound = word.upperBound;
        currentLine.lowerBound = word.lowerBound;
        currentLine.wordsInLine.add(word);
        return currentLine;
    }

    private void createNewLineWithBoundaries(CustomList<Line> lines, Word word) {
        Line newLine = createLine(word);
        lines.add(newLine);
    }

    public boolean checkIfOverlaps(float firstLowerBound, float firstUpperBound, float secondLowerBound, float secondUpperBound) {
        return (firstLowerBound > secondUpperBound && secondLowerBound > firstUpperBound);
    }

    public void adaptBoundaries(Word word, Line line) {
        if (word.upperBound < line.upperBound) {
            line.upperBound = word.upperBound;
        }
        if (word.lowerBound > line.lowerBound) {
            line.lowerBound = word.lowerBound;
        }
        return;
    }

    public void updateWordDistances(Line line, Word word) {
        //set word distances
        int currentWordIndex = line.wordsInLine.indexOf(word);
        if (currentWordIndex > 0) {
            line.wordsInLine.get(currentWordIndex - 1).distanceToNextWord =
                    (word.startX - line.wordsInLine.get(currentWordIndex - 1).endX);
        } else {
            word.firstInLine = true;
        }

    }

    public Preprocessing(MetaData metaData, PDDocument document) throws IOException {
        this.metaData = metaData;
        this.document = document;
    }


    public void extractPositionOfText(File pdfFile) throws IOException {
        PDDocument document = null;
        try {
            document = PDDocument.load(pdfFile);
            Preprocessing stripper = new Preprocessing(this.metaData, document);
            stripper.setSortByPosition(true);

            for (int pageNumber = 0; pageNumber < document.getNumberOfPages(); ++pageNumber) {
                stripper.stripPage(pageNumber);
            }
        } catch (InvalidPasswordException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    public void stripPage(int pageNumber) throws IOException {
        Page page = new Page();
        page.pageNumber = this.metaData.pages.size() + 1;
        this.metaData.pages.add(page);
        setStartPage(pageNumber + 1);
        setEndPage(pageNumber + 1);
        Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
        writeText(document, dummy);
    }

    /**
     * Override the default functionality of PDFTextStripper.writeString()
     */
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) {
        Float upperBound = 0.0f;
        Float lowerBound = 0.0f;
        ArrayList<Letter> letters = new ArrayList<>();
        for (TextPosition text : textPositions) {
            /*System.out.println(text.getUnicode() + " [(X=" + text.getXDirAdj() + ",Y=" +
                    text.getYDirAdj() + ") height=" + text.getHeightDir() + " width=" +
                    text.getWidthDirAdj() + "]");*/

            if (upperBound == 0.0f) {
                upperBound = text.getY();
            }
            if (lowerBound == 0.0f) {
                lowerBound = (text.getY() + text.getHeight());
            }
            if (text.getY() > upperBound && upperBound < lowerBound) {
                upperBound = text.getY();
            }
            if ((text.getY() + text.getHeight()) > lowerBound) {
                lowerBound = (text.getY() + text.getHeight());
            }

            Letter letter = new Letter();
            letter.letter = text.toString().charAt(0);
            //letter.x = text.getXDirAdj();
            letter.x = text.getX();
            //letter.upperBound = text.getYDirAdj();
            //letter.upperBound = text.getY();
            //letter.height = text.getHeight();
            //letter.width = text.getWidthDirAdj();
            letter.width = text.getWidth();
            letter.endX = text.getEndX();
            //letter.lowerBound = letter.upperBound + letter.getHeight();
            letter.unicode = "\\u" + Integer.toHexString(letter.letter | 0x10000).substring(1);
            letters.add(letter);
        }

        for (Letter letter : letters) {
            letter.upperBound = upperBound;
            letter.lowerBound = lowerBound;
            letter.height = (lowerBound - upperBound);
        }

        int currentPage = this.metaData.pages.last().pageNumber;
        if (this.metaData.letterDict.get(currentPage) == null) {
            this.metaData.letterDict.put(currentPage, new ArrayList<>());
        }
        this.metaData.letterDict.get(currentPage).addAll(letters);
    }

    public ArrayList<Word> getWordsOfPage(List<Letter> letters) {
        Letter lastLetter = new Letter();
        Word currentWord = new Word();
        Boolean lastLetterWasEmptySpace = false;
        ArrayList<Word> words = new ArrayList<>();
        for (Letter currentLetter : letters) {
            //check if char is space (" ")
            if (currentLetter.letter == ' ' || Character.isSpaceChar(currentLetter.letter)) {
                lastLetterWasEmptySpace = true;
                continue;
            }

            //check if the distance between the end of the preceeding char and the beginning of the following one is bigger than the spacing tolerance (average space size)
            if (lastLetterWasEmptySpace || currentLetter.x - lastLetter.endX > (this.metaData.spacingTolerance * 1.3)
                    || (lastLetter.lowerBound < currentLetter.upperBound) || (currentWord.upperBound > currentLetter.lowerBound) || currentLetter.x < lastLetter.x) {
                if (currentWord.lettersOfWord.size() != 0) {
                    words.add(currentWord);
                }
                currentWord = new Word();
                currentWord.lettersOfWord.add(currentLetter);
                currentWord.startX = currentLetter.x;
                currentWord.upperBound = currentLetter.upperBound;
                currentWord.endX = currentLetter.endX;
                currentWord.lowerBound = currentLetter.lowerBound;
                lastLetterWasEmptySpace = false;
            } else {
                currentWord.lettersOfWord.add(currentLetter);
                currentWord.endX = currentLetter.endX;
                //currentWord.lowerBound = currentLetter.lowerBound;
            }
            if (currentWord.upperBound > currentLetter.upperBound) {
                currentWord.upperBound = currentLetter.upperBound;
            }
            if (currentWord.lowerBound < currentLetter.lowerBound) {
                currentWord.lowerBound = currentLetter.lowerBound;
            }
            lastLetter = currentLetter;
        }
        words.add(currentWord);
        return words;
    }

    public void setDistanceToPreviousAndNextLine() {

        for (Page page : metaData.pages) {
            for (Line line : page.linesOfPage) {
                if (line.previous == null || line.previous.page < line.page) {
                    line.distanceToPreviousLine = 0.0f;
                } else {
                    line.distanceToPreviousLine = (line.upperBound - line.previous.lowerBound);
                }
                if (line.next == null || line.page < line.next.page) {
                    line.distanceToNextLine = 0.0f;
                } else {
                    line.distanceToNextLine = (line.next.upperBound - line.lowerBound);
                }
            }
        }
    }

}

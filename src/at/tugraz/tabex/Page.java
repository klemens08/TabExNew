package at.tugraz.tabex;

import java.util.ArrayList;

public class Page {
    public CustomList<Letter> letters = new CustomList<>();
    int pageNumber;
    Line firstLineOfPage;
    Line lastLineOfPage;
    CustomList<Line> linesOfPage = new CustomList<>();
    float height;
    float width;
    Page next;
    Page previous;
    Float leftBound;
    Float rightBound;
    Float upperBound;
    Float lowerBound;
    int columns = 1;
    boolean isMultiColumn = false;
    public Float startColumnBorder = 0.0f;
    public Float endColumnBorder = 0.0f;
    Float centerOfPage = 0.0f;

    public void setHeightOfPage() {
        this.height = lastLineOfPage.lowerBound - firstLineOfPage.upperBound;
    }

    public void setWidthOfPage() {
        float leftBound = 0.0f;
        float rightBound = 0.0f;

        for (Line line : linesOfPage) {
            if (leftBound == 0.0f || line.getWordsInLine().first().startX < leftBound) {
                leftBound = line.getWordsInLine().first().startX;
            }
            if (rightBound == 0.0f || line.getWordsInLine().last().endX > rightBound) {
                rightBound = line.getWordsInLine().last().endX;
            }
        }
        this.width = rightBound - leftBound;
    }

    public void setPageBoundaries(ArrayList<Word> wordsOfPage) {
        Float leftBound = Float.POSITIVE_INFINITY;
        Float rightBound = Float.NEGATIVE_INFINITY;
        Float upperBound = Float.POSITIVE_INFINITY;
        Float lowerBound = Float.NEGATIVE_INFINITY;

        for (Word word : wordsOfPage) {

            if (word.startX < leftBound) {
                leftBound = word.startX;
            }
            if (word.endX > rightBound) {
                rightBound = word.endX;
            }
            if (word.upperBound < upperBound) {
                upperBound = word.upperBound;
            }
            if (word.lowerBound > lowerBound) {
                lowerBound = word.lowerBound;
            }

        }

        this.leftBound = leftBound;
        this.rightBound = rightBound;
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;

    }
}

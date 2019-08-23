package at.tugraz.tabex;

import java.util.ArrayList;
import java.util.List;

public class Line {

    public float distanceToPreviousLine = 0.0f;
    public float distanceToNextLine = 0.0f;
    float upperBound;
    float lowerBound;
    float lineHeight;
    //float distanceToNextLine;
    CustomList<Word> wordsInLine = new CustomList<Word>();
    List<WordGroup> wordGroupsInLine = new ArrayList<WordGroup>();
    int sparsePointCount = 0;
    boolean isSparse = false;
    boolean isCritical = false;
    boolean containsKeyWord = false;
    int lineNumber = 0;
    boolean toBeDeleted = false;
    public Line previous = null;
    public Line next = null;
    public boolean lastLineOfPage = false;
    public boolean firstLineOfPage = false;
    public boolean isTableLine = false;
    public boolean isHeaderLine = false;
    public boolean isFooterLine = false;
    int page = 0;
    boolean isCenterSparse = false;
    boolean leftColumnSparse = false;
    boolean rightColumnSparse = false;
    boolean leftColumnCritical = false;
    boolean rightColumnCritical = false;
    List<WordGroup> wordGroupsInLeftColumn = new ArrayList<WordGroup>();
    List<WordGroup> wordGroupsInRightColumn = new ArrayList<WordGroup>();

    public Line() {
        this.upperBound = Float.POSITIVE_INFINITY;
        this.lowerBound = Float.NEGATIVE_INFINITY;
    }

    public Line(Line line) {
        this.upperBound = line.upperBound;
        this.lowerBound = line.lowerBound;
        this.lineHeight = line.lineHeight;
        this.wordsInLine = new CustomList<>();
        this.wordGroupsInLine = new ArrayList<>();
        this.sparsePointCount = line.sparsePointCount;
        this.isSparse = line.isSparse;
        this.isCritical = false;
        this.containsKeyWord = line.containsKeyWord;
        this.lineNumber = line.lineNumber;
        this.toBeDeleted = line.toBeDeleted;
        this.previous = line.previous;
        this.next = line.next;
        this.lastLineOfPage = line.lastLineOfPage;
        this.firstLineOfPage = line.firstLineOfPage;
        this.isTableLine = line.isTableLine;
        this.isHeaderLine = line.isHeaderLine;
        this.isFooterLine = line.isFooterLine;
    }

    public float getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(float upperBound) {
        this.upperBound = upperBound;
    }

    public float getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(float lowerBound) {
        this.lowerBound = lowerBound;
    }

    public CustomList<Word> getWordsInLine() {
        return wordsInLine;
    }

    public List<WordGroup> getWordGroupsInLine() {
        return wordGroupsInLine;
    }

    public void setWordGroupsInLine() {
        ArrayList<WordGroup> resultList = new ArrayList<>();
        WordGroup currentWordGroup = new WordGroup();
        WordGroup previousWordGroup = null;
        for (Word currentWord : wordsInLine) {
            if (currentWord.isNextSparse || currentWord.lastInLine) {
                currentWordGroup.words.add(currentWord);
                currentWordGroup.startX = currentWordGroup.words.get(0).startX;
                currentWordGroup.endX = currentWord.endX;
                if (currentWord.lastInLine) {
                    currentWordGroup.lastInLine = true;
                }
                resultList.add(currentWordGroup);
                currentWordGroup.previous = previousWordGroup;

                if (currentWordGroup.previous != null) {
                    currentWordGroup.previous.next = currentWordGroup;
                }

                previousWordGroup = currentWordGroup;
                currentWordGroup = new WordGroup();
            } else {
                currentWordGroup.words.add(currentWord);
            }
        }
        wordGroupsInLine = resultList;
    }

    public String getLineString() {
        String line = "";
        for (Word word : wordsInLine) {
            line = line + " " + word.getWordString();
        }
        return line;
    }

    public String getLineStringTabluar(MetaData metaData) {
        String line = "";
        for (Word word : wordsInLine) {
            if (word.distanceToNextWord != null && word.distanceToNextWord > metaData.spacingTolerance) {
                line = line + " || " + word.getWordString();
            } else {
                line = line + " " + word.getWordString();
            }
        }
        return line;
    }

    public void update(MetaData metaData) {
        for (Word word : wordsInLine) {
            if (this.upperBound > word.upperBound) {
                this.upperBound = word.upperBound;
            }
            if (this.lowerBound < word.lowerBound) {
                this.lowerBound = word.lowerBound;
            }
        }

        this.lineHeight = (this.lowerBound - this.upperBound);
        setSparsePointCount(metaData.minimumSpaceGapThreshold, metaData);
        setWordGroupsInLine();
    }

    public void setSparsePointCount(Float minimumSpaceGapThreshold, MetaData metaData) {
        this.sparsePointCount = 0;
        this.isSparse = false;
        for (Word word : this.getWordsInLine()) {
            word.isNextSparse = false;

            // check if distance to next word classifies this word as sparse
            if ((word.getDistanceToNextWord() != null && word.getDistanceToNextWord() > minimumSpaceGapThreshold)) {
                this.sparsePointCount++;
                this.isSparse = true;
                word.isNextSparse = true;
            }
        }
        this.setWordGroupsInLine();
        if (this.lastLineOfPage && this.sparsePointCount != this.previous.sparsePointCount) {
            this.isSparse = false;
        }

        if (/*metaData.pages.size() != 1 && */ this.isSparse) {
            int realSparse = 0;
            for (WordGroup wordGroup : this.getWordGroupsInLine()) {
                if (wordGroup.getDistanceToNextWordGroup() > (minimumSpaceGapThreshold * 2)) {
                    realSparse++;
                }
            }
            if (realSparse == 0 && !(this.wordsInLine.first().startX > (metaData.getPage(this.page).leftBound + ((metaData.getPage(this.page).rightBound) - metaData.getPage(this.page).leftBound) * 0.05))) {
                this.isSparse = false;
                //this.isSemiSparse = true;
            }
        }


    }

    public float getStartX() {
        return this.getWordsInLine().first().startX;
    }

    public float getEndX() {
        return this.getWordsInLine().last().endX;
    }

    public float getWidth() {
        return getEndX() - getStartX();
    }

    public void setLineHeight() {
        this.lineHeight = (this.lowerBound - this.upperBound);
    }

    public void setDistanceToPreviousLine() {
        if (this.previous != null) {
            this.distanceToPreviousLine = (this.upperBound - this.previous.lowerBound);
        }
    }

    public void setDistanceToNextLine() {
        if (this.next != null) {
            this.distanceToPreviousLine = (this.next.upperBound - this.lowerBound);
        }
    }

}

package at.tugraz.tabex;

import java.util.ArrayList;
import java.util.List;

public class Word {

    public List<Letter> lettersOfWord = new ArrayList<Letter>();
    Float startX;
    Float endX;
    Float upperBound;
    Float lowerBound;
    Boolean firstInLine = false;
    Boolean lastInLine = false;
    public Word previous = null;
    public Word next = null;
    Float distanceToNextWord;
    boolean isNextSparse = false;

    public Boolean getFirstInLine() {
        return firstInLine;
    }

    public void setFirstInLine(Boolean firstInLine) {
        this.firstInLine = firstInLine;
    }

    public Boolean getLastInLine() {
        return lastInLine;
    }

    public void setLastInLine(Boolean lastInLine) {
        this.lastInLine = lastInLine;
    }

    public Float getDistanceToNextWord() {
        return distanceToNextWord;
    }

    public void setDistanceToNextWord(Float distanceToNextWord) {
        this.distanceToNextWord = distanceToNextWord;
    }

    public Float getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(Float upperBound) {
        this.upperBound = upperBound;
    }

    public Float getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(Float lowerBound) {
        this.lowerBound = lowerBound;
    }

    public Word() {
        this.upperBound = Float.POSITIVE_INFINITY;
        this.lowerBound = Float.NEGATIVE_INFINITY;
    }

    public List<Letter> getLettersOfWord() {
        return lettersOfWord;
    }

    public void setLettersOfWord(List<Letter> lettersOfWord) {
        this.lettersOfWord = lettersOfWord;
    }

    public Float getStartX() {
        return startX;
    }

    public void setStartX(Float startX) {
        this.startX = startX;
    }

    public Float getStartY() {
        return upperBound;
    }

    public void setStartY(Float upperBound) {
        this.upperBound = upperBound;
    }

    public Float getEndX() {
        return endX;
    }

    public void setEndX(Float endX) {
        this.endX = endX;
    }


    public String getWordString() {
        String word = "";
        for (Letter letter : lettersOfWord) {
            word += letter.letter;
        }
        return word;
    }
}

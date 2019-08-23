package at.tugraz.tabex;

import java.util.ArrayList;

public class WordGroup {
    public Float startX;
    public Float endX;
    public ArrayList<Word> words = new ArrayList();
    public boolean lastInLine = false;
    public boolean isUnclassified;
    public WordGroup previous = null;
    public WordGroup next = null;


    public String getWordGroupString() {
        String wordGroupString = "";
        for (Word word : this.words) {
            wordGroupString += " " + word.getWordString();
        }
        //System.out.println(wordGroupString);
        return wordGroupString;
    }

    Float getDistanceToNextWordGroup() {

        if (this.next == null) {
            return 0.0f;
        }

        return this.words.get(this.words.size() - 1).distanceToNextWord;
    }
}
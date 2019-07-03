package at.tugraz.tabex;

import java.util.ArrayList;

public class ColumnLine {

    boolean isSpanned;
    ArrayList<Word> words = new ArrayList<>();
    Float startX;
    Float endX;
    int colSpan = 0;

    public String getWordsString() {
        if(words.size() == 0) {
            return "";
        }

        String result = "";
        for(Word word : words) {
            result += word.getWordString() + " ";
        }
        return result;
    }
}

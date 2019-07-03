package at.tugraz.tabex;

import java.util.ArrayList;
import java.util.Collections;

public class HelperFunctions {


    public static float getMedianLineHeight(ArrayList<Line> lines) {

        ArrayList<Float> allLineHeights = new ArrayList<>();

        for (Line line : lines) {
            allLineHeights.add(line.lineHeight);
        }
        Collections.sort(allLineHeights);

        return allLineHeights.get(allLineHeights.size() / 2);
    }


    public static float getMedianDistanceBetweenLines(ArrayList<Line> lines) {

        ArrayList<Float> allDistancesBetweenLines = new ArrayList<>();

        for (Line line : lines) {
            if(lines.indexOf(line) == lines.size() - 1 || line.distanceToNextLine < 0){
                break;
            }
            allDistancesBetweenLines.add(line.distanceToNextLine);
        }
        Collections.sort(allDistancesBetweenLines);

        if(allDistancesBetweenLines.size() > 0){
            return allDistancesBetweenLines.get(allDistancesBetweenLines.size() / 2);
        }
        return 0.0f;
    }

    public static float getAverageYLineSpaceRudimentary(MetaData metaData) {

        ArrayList<Float> allDistancesToNextLine = new ArrayList<>();
        int lineCount = 0;
        for(Page page : metaData.pages){
            for (int lineIndex = 1; lineIndex < page.linesOfPage.size(); lineIndex++) {
                lineCount++;
                Float lineDistance = page.linesOfPage.get(lineIndex).upperBound - page.linesOfPage.get(lineIndex - 1).lowerBound;
                if (lineDistance > 0) {
                    allDistancesToNextLine.add(lineDistance);
                }
            }
        }

        float totalDistance = 0;

        for (Float distance : allDistancesToNextLine) {
            totalDistance += distance;
        }

        float average = totalDistance / lineCount;
        return average;
    }
}

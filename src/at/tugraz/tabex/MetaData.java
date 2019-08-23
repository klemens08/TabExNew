package at.tugraz.tabex;

import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.util.*;


public class MetaData {

    public float minimumSpaceGapThreshold = 0;
    Map<Integer, ArrayList<Letter>> letterDict = new HashMap<Integer, ArrayList<Letter>>();
    public CustomList<Page> pages;
    float medianWidthOfLines = 0.0f;
    float maxWidthOfLines = 0.0f;
    public List<Table> tables;
    private PDFTextStripper pdfTextStripper;
    public float spacingTolerance;
    boolean isMultiColumnPDF = false;
    double multiColumnPercentage = 0;
    public Float startColumnBorder = 0.0f;
    public Float endColumnBorder = 0.0f;
    public String fileName = "";


    public float medianLineStartX;
    public float lineEndX;
    public float sparseLineStartX;
    public float averageYLineSpaceOfDocument;
    public float medianYLineSpace;
    public float medianLineHeight;


    public MetaData() throws IOException {
        pages = new CustomList<>();
        tables = new ArrayList<Table>();
        pdfTextStripper = new PDFTextStripper();
        spacingTolerance = pdfTextStripper.getSpacingTolerance();
    }

    public List<Table> getTables() {
        return tables;
    }

    public void setTables(List<Table> tables) {
        this.tables = tables;
    }

    public Page getPage(int pageNumber) {
        return pages.stream().filter(page -> page.pageNumber == pageNumber).findAny().orElse(null);
    }

    public void setMedianWidthOfLines() {

        ArrayList<Float> widthOfLines = new ArrayList<>();

        for (Page page : pages) {
            for (Line line : page.linesOfPage) {
                float lineWidth = line.getEndX() - line.getStartX();
                widthOfLines.add(lineWidth);
            }
        }
        Collections.sort(widthOfLines);
        this.medianWidthOfLines = widthOfLines.get(widthOfLines.size() / 2);
    }

    public void setMedianYLineSpace() {

        ArrayList<Float> distancesBetweenLines = new ArrayList<>();

        for (Page page : pages) {
            for (Line line : page.linesOfPage) {
                if (line.distanceToNextLine != 0.0f) {
                    distancesBetweenLines.add(line.distanceToNextLine);
                }
            }
        }
        Collections.sort(distancesBetweenLines);
        this.medianYLineSpace = distancesBetweenLines.get(distancesBetweenLines.size() / 2);
    }

    public void setMaxWidthOfLines() {

        Float maxWidth = 0.0f;

        for (Page page : pages) {
            for (Line line : page.linesOfPage) {
                float lineWidth = line.getEndX() - line.getStartX();
                if (lineWidth > maxWidth) {
                    maxWidth = lineWidth;
                }
            }
        }
        this.maxWidthOfLines = maxWidth;
    }

    public void setLineNumbers() {
        for (Page page : this.pages) {
            int lineNumber = 0;
            for (Line line : page.linesOfPage) {
                line.lineNumber = lineNumber;
                lineNumber++;
            }
        }
    }
}

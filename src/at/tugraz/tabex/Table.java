package at.tugraz.tabex;

import java.util.ArrayList;

public class Table {

    ArrayList<Line> Header = new ArrayList<>();
    ArrayList<Line> Footer = new ArrayList<>();
    ArrayList<TableColumn> tableColumns = new ArrayList<>();
    SparseBlock tableBody = new SparseBlock();
    boolean toBeDeleted = false;
    int pageNumber;

    public String getHeaderString() {
        String header = "";
        for (Line line : Header) {
            header = header + " " + line.getLineString();
        }
        return header;
    }

    public String getFooterString() {
        String footer = "";
        for (Line line : Footer) {
            footer = footer + " " + line.getLineString();
        }
        return footer;
    }

    public int getIndexOfColumn(Float startX, Float endX){
        int i = 0;
        while(i < tableColumns.size() - 1 && startX > tableColumns.get(i).endX) {
            i++;
        }
        return i;
    }

    /*public void printTableColumns() {
        for (TableColumn tableColumn : tableColumns) {
            for (ArrayList<Word> arraylist : tableColumn.linesOfColumn) {
                String currentLineOfColumn = "";
                for (Word word : arraylist) {
                    currentLineOfColumn += " " + word.getWordString();
                }
                System.out.println(currentLineOfColumn);
            }
            System.out.println("\n");
        }
    }*/

    public void printBoundaries(){
            Float top = 0.f;
            Float bot = 0.f;
            Float left = 9999.f;
            Float right = 0.f;

            if(this.tableBody.sparseBlockLines.size() != 0){
                top = this.tableBody.sparseBlockLines.get(0).upperBound;
                bot = this.tableBody.sparseBlockLines.get(this.tableBody.sparseBlockLines.size()-1).lowerBound;
            }

            for (Line line : this.tableBody.sparseBlockLines) {
                if (line.getWordsInLine().first().startX < left) {
                    left = line.getWordsInLine().first().startX;
                }
                if (line.getWordsInLine().last().endX > right) {
                    right = line.getWordsInLine().last().endX;
                }
            }


            /*System.out.println("Table Boundaries:");
            System.out.println("Top: " + top);
            System.out.println("Left: " + left);
            System.out.println("Bot: " + bot);
            System.out.println("Right: " + right);*/
    }


    /*public void printTableInTableFormat() {
        ArrayList<String> tableLines = new ArrayList<>();
        for(int i = 0; i < tableBody.sparseBlockLines.size(); i++) {
            String result = "";
            for(TableColumn column : tableColumns) {
                result += column.columnLines.get(i).getWordsString() + "\t";
            }
            tableLines.add(result);
        }
        for(String tableLine : tableLines){
            System.out.println(tableLine);
        }
    }*/

    public void deepEvaluationOfSparseFields() {



    }
}

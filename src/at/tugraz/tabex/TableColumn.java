package at.tugraz.tabex;

import java.util.ArrayList;

public class TableColumn {

    public Float startX;
    public Float endX;
    public ArrayList<ColumnLine> columnLines = new ArrayList();

    public TableColumn(Float startX, Float endX, int numberOfLines) {
        this.startX = startX;
        this.endX = endX;

        for(int i = 0 ; i < numberOfLines ; i++){
            this.columnLines.add(new ColumnLine());
        }
    }

    public TableColumn() {
    }

    public Float getStartX() {
        return startX;
    }

    public void setStartX(Float startX) {
        this.startX = startX;
    }

     public Float getEndX() {
        return endX;
    }



    public void setEndX(Float endX) {
        this.endX = endX;
    }

}

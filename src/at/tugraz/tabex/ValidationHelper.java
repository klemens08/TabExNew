package at.tugraz.tabex;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ValidationHelper {

    public static List<Table> GetTablesOfPage(List<Table> tables, int pageNumber) {
        return tables.stream().filter(t -> t.pageNumber == pageNumber).collect(Collectors.toList());
    }

    public static List<CheckData> GetCheckDataOfPage(List<CheckData> checkData, int pageNumber) {
        return checkData.stream().filter(t -> t.pageNumber == pageNumber).collect(Collectors.toList());
    }

    public static int GetMaxPageNumber(List<CheckData> checkData, List<Table> tables) {
        int checkDataMaxPageNumber = checkData.size() > 0 ? checkData.stream().max(Comparator.comparing(c -> c.pageNumber)).get().pageNumber : 0;
        int tablesMaxPageNumber = tables.size() > 0 ? tables.stream().max(Comparator.comparing(t -> t.pageNumber)).get().pageNumber : 0;
        if (checkDataMaxPageNumber > tablesMaxPageNumber) {
            return checkDataMaxPageNumber;
        }
        return tablesMaxPageNumber;
    }
}

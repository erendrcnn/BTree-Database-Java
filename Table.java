import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Table {
    BTree[] bTreeTable = null;
    String[] columns = null;
    List<Row> rows = null;

    Table(String path) {
        reloadFromFile(path);
    }

    Table() {
        rows = new LinkedList<Row>();
    }

    public static void main(String[] args) {
        Table tb = new Table("C:\\Users\\erend\\IdeaProjects\\src\\BIL212\\odev2\\userLogs.csv");
        // tb.show(); // should print everything
        tb.filter("id==3").project("first_name").show();  // should print Aldon

        //This is suppposed to print Jobling,sjoblingi@w3.org
        tb.filter("id==19 AND ip_address==242.40.106.103").project("last_name,email").show();

        //amathewesg@slideshare.net
        //imathewesdx@ucoz.com
        tb.filter("last_name==Mathewes").project("email").show();

        //We might test with a different csv file with same format but different column count
        //Good luck!!
    }

    void populateHeader(String header) {
        columns = header.split(",");
    }

    void populateData(Scanner myReader) {
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            rows.add(new Row(data));
        }
    }

    Row getRow(int index) {
        return rows.get(index);
    }

    public void reloadFromFile(String path) {
        rows = new LinkedList<Row>(); //resets

        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            if (myReader.hasNextLine()) { //This is supposed to be the header
                populateHeader(myReader.nextLine());
                populateData(myReader);
            }
            myReader.close();
            buildIndexes();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    /***
     *  This will iterate over the columns and build BTree based indexes for all columns
     */

    private void buildIndexes() {
        bTreeTable = new BTree[columns.length];
        for (int i = 0; i < columns.length; i++) {
            bTreeTable[i] = new BTree<String, List<Integer>>();
        }
        for (int i = 0; i < bTreeTable.length; i++) {
            for (int j = 0; j < rows.size(); j++) {
                if (bTreeTable[i].get(rows.get(j).getColumnAtIndex(i)) == null) {
                    ArrayList<Integer> tempArray = new ArrayList<>();
                    tempArray.add(j);
                    bTreeTable[i].put(rows.get(j).getColumnAtIndex(i), tempArray);
                } else {
                    ArrayList<Integer> tempArrayExist = (ArrayList<Integer>) bTreeTable[i].get(rows.get(j).getColumnAtIndex(i));
                    tempArrayExist.add(j);
                    bTreeTable[i].put(rows.get(j).getColumnAtIndex(i), tempArrayExist);
                }

            }
        }
    }

    /***
     *This method is supposed to parse the filtering statement
     * identify which rows will be filtered
     * apply filters using btree indices
     * collect rows from each btree and find the mutual intersection of all.
     * Statement Rules: ColumnName==ColumnValue AND ColumnName==ColumnValue AND ColumnName==ColumnValue
     * Can be chained by any number of "AND"s; 0 or more
     * sample filterStatement: first_name==Roberta AND id=3
     * Return Type: A new Table which consists of Rows that pass the filter test
     */
    public Table filter(String filterStatement) {
        Row[] primary = null;
        String[] data = filterStatement.split(" ");
        int andNum = numberAND(data);
        String[] conditions = new String[andNum + 1];

        for (int i = 0; i < conditions.length; i++) {
            conditions[i] = data[2 * i];
        }

        int x; int y;
        String[] left = new String[andNum + 1];
        String[] right = new String[andNum + 1];

        for (int i = 0; i < andNum + 1; i++) {
            x = separate(conditions[i]);
            y = separateEquation(conditions[i]);
            if (y != -1) {
                left[i] = conditions[i].substring(0, y);
                right[i] = conditions[i].substring(y + 2);
            } else {
                left[i] = conditions[i].substring(0, x);
                right[i] = conditions[i].substring(x + 1);
            }
        }

        int[] indexColumns = new int[columns.length];
        for (String leftData : left) {
            for (int j = 0; j < columns.length; j++) {
                if (leftData.equals(columns[j]))
                    indexColumns[j] = 1;
            }
        }

        int condCount = 0;
        for (int i = 0; i < indexColumns.length; i++) {
            if (indexColumns[i] != 0) {
                ArrayList<Integer> tempList = ((ArrayList<Integer>) bTreeTable[i].get(right[0]));
                if (tempList != null) {
                    for (Integer integer : tempList) {
                        primary = increase(primary);
                        primary[condCount++] = rows.get(integer);
                    }
                }
                break;
            }
        }
        try {
            if (primary == null)
                throw new Exception("Bu tablo icin eleman bulunamadi.");
        } catch (Exception e) {
            Table temp = new Table();
            temp.columns = columns;
            System.out.println(e.getMessage());
            return temp;
        }

        Row[] commonState = null;
        if (andNum > 0) {
            int rightCount = 1;
            int condCount2 = 0;
            for (Row row : primary) {
                for (int j = 0; j < indexColumns.length; j++) {
                    if (indexColumns[j] != 0) {
                        if (row.getColumnAtIndex(j).equals(right[rightCount])) {
                            commonState = increase(commonState);
                            commonState[condCount2] = row;
                        }
                    }
                }

            }
        } else {
            commonState = primary;
        }

        Table newTable = new Table();
        newTable.columns = columns;
        newTable.rows.addAll(Arrays.asList(commonState).subList(0, Objects.requireNonNull(commonState).length));

        return newTable;
    }

    /***
     * This method projects only set of columns from the table and forms a new table including all rows but only selected columns
     * columnsList is comma separated list of columns i.e., "id,email,ip_address"
     */
    public Table project(String columnsList) {
        Row[] outputRow;
        String[] data = columnsList.split(",");
        String[] tempArray = null;
        if (data.length > 1) {
            boolean isFirst = true;
            int rowCounter;
            for (String s : data) {
                rowCounter = 0;
                for (int j = 0; j < columns.length; j++) {
                    if (s.equals(columns[j])) {
                        if (isFirst) {
                            tempArray = resizeString(null);
                            tempArray[0] = "";
                            isFirst = false;
                        }
                        tempArray[0] += rows.get(rowCounter).getColumnAtIndex(j) + ",";
                        rowCounter++;
                    }
                }
            }
            assert tempArray != null;
            tempArray[0] = tempArray[0].substring(0, tempArray[0].length() - 1);

        } else {
            for (int j = 0; j < columns.length; j++) {
                if (Objects.equals(data[0], columns[j])) {
                    for (int k = 0; k < rows.size(); k++) {
                        tempArray = resizeString(tempArray);
                        tempArray[k] = rows.get(k).getColumnAtIndex(j);
                    }
                }
            }

        }

        if (tempArray != null) {
            outputRow = new Row[tempArray.length];
            for (int i = 0; i < tempArray.length; i++) {
                outputRow[i] = new Row(tempArray[i]);
            }
        } else
            outputRow = new Row[0];

        Table newTable = new Table();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < columns.length; j++) {
                if (data[i].equals(columns[j])) {
                    newTable.columns = resizeString(newTable.columns);
                    newTable.columns[i] = data[i];
                }
            }
        }
        Collections.addAll(newTable.rows, outputRow);

        return newTable;
    }

    /***
     *  Print column names in the first line
     *  Print the rest of the table
     */

    public void show() {
        System.out.println(String.join(",", columns) + "\n");
        for (Row rw : rows) {
            System.out.println(rw.toString() + "\n");
        }
    }

    public int separate(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '=')
                return i;
        }
        return -1;
    }

    public int separateEquation(String str) {
        for (int i = 0; i < str.length() - 1; i++) {
            if (str.startsWith("==", i))
                return i;
        }
        return -1;
    }

    public int numberAND(String[] tempArray) {
        int count = 0;
        for (String s : tempArray) {
            if (s.equals("AND"))
                count++;
        }
        return count;
    }

    public Row[] increase(Row[] primary) {
        Row[] newTables;
        if (primary == null) { newTables = new Row[1]; }
        else {
            newTables = new Row[primary.length + 1];
            System.arraycopy(primary, 0, newTables, 0, primary.length);

        }
        return newTables;
    }

    public String[] resizeString(String[] primary) {
        String[] newTables;
        if (primary == null) { newTables = new String[1]; }
        else {
            newTables = new String[primary.length + 1];
            System.arraycopy(primary, 0, newTables, 0, primary.length);
        }
        return newTables;
    }
}

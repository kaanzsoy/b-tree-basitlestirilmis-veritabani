/*
 * Veriler tabloya yerlestirildikten sonra INDEXING ISLEMI UZUN SUREBILIYOR
 * (Ornek olarak verilen 20.000 satirlik csv dosyasi icin yaklasik 18 saniye)
 */

import java.io.File; // Import the File class
import java.io.FileNotFoundException; // Import this class to handle errors
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner; // Import the Scanner class to read text files

import hw99.Row;

public class Table {
    List<Row> rows = null; // tablonun satirlari
    String[] columns = null;    // sutun basliklari

    /***
     * You may more constructors
     * 
     * @param path
     */
    Table(String path) {
        reloadFromFile(path);
    }

    Table(String[] kolon_adlari) { // filter ve project sonucu olusan yeni tablolar icin kullanilir
        rows = new LinkedList<Row>();
        columns = kolon_adlari;
    }

    int row_sayisi = 0;

    void populateHeader(String header) {
        columns = header.split(",");
    }

    void populateData(Scanner myReader) {
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            rows.add(new Row(data));

            row_sayisi++;
        }
        //System.out.println("Populate Data Tamamlandi");

    }

    Row getRow(int index) {
        return rows.get(index);
    }

    public void reloadFromFile(String path) {
        rows = new LinkedList<Row>(); // resets

        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            if (myReader.hasNextLine()) { // This is supposed to be the header
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
     * This will iterate over the columns and build BTree based indexes for all
     * columns
     */

    ArrayList<BTree<String, ArrayList<Integer>>> b_agaclari;    // her kolona ait bir agac var

    private void buildIndexes() {

        b_agaclari = new ArrayList<BTree<String, ArrayList<Integer>>>();

        for (int i = 0; i < columns.length; i++) {

            BTree new_tree_for_column = new BTree<String, List<Integer>>();

            b_agaclari.add(new_tree_for_column);

        }

        for (int j = 0; j < row_sayisi; j++) {

            for (int k = 0; k < columns.length; k++) {

                ArrayList<Integer> value_list = b_agaclari.get(k).get(rows.get(j).getColumnAtIndex(k));

                if (value_list != null) {
                    value_list.add(j);
                } else {
                    value_list = new ArrayList<Integer>();
                    value_list.add(j);
                }

                b_agaclari.get(k).put(rows.get(j).getColumnAtIndex(k), value_list);

                // key degerleri, csv dosyasindaki her bir hucredeki verilerden olusuyor.
                // her key degerinin en az bir value degeri var.
                // value degerleri, ilgili key degerinin verilen csv dosyasinda kacinci satirda bulundugu bilgisini veriyor
                // ornegin, bir first_name degeri birden fazla satirda bulunabilir.

            }

        }

        //System.out.println("Indexing Tamamlandi");

    }

    /***
     * This method is supposed to parse the filtering statement
     * identify which rows will be filtered
     * apply filters using btree indices
     * collect rows from each btree and find the mutual intersection of all.
     * Statement Rules: ColumnName==ColumnValue AND ColumnName==ColumnValue AND
     * ColumnName==ColumnValue
     * Can be chained by any number of "AND"s; 0 or more
     * sample filterStatement: first_name==Roberta AND id=3
     * Return Type: A new Table which consists of Rows that pass the filter test
     */

    String[] commands;
    String[] kolon_adlari;
    String[] degerler;
    ArrayList<Integer> yazdirilacak_satirlar;

    public Table filter(String filterStatement) {

        Table yeni_tablo = new Table(columns);

        commands = filterStatement.split(" AND ");

        /*
        // Test amacli
        for (int i = 0; i < commands.length; i++) {
            System.out.println(commands[i]);
        }
        */

        if (commands.length == 0) {
            kolon_adlari = new String[1];
            degerler = new String[1];

            kolon_adlari[0] = commands[0].substring(0, commands[0].indexOf("="));
            degerler[0] = commands[0].substring((commands[0].lastIndexOf("=")) + 1, commands[0].length());
        } else {

            kolon_adlari = new String[commands.length];
            degerler = new String[commands.length]; // yani key degerleri

            for (int i = 0; i < commands.length; i++) {
                kolon_adlari[i] = commands[i].substring(0, commands[i].indexOf("="));
                degerler[i] = commands[i].substring((commands[i].lastIndexOf("=")) + 1, commands[i].length());
            }
        }

        /*
        // Test amacli
        for (int i = 0; i < commands.length; i++) {

            System.out.println(kolon_adlari[i]);

            System.out.println(degerler[i]);
        }
        */

        yazdirilacak_satirlar = new ArrayList<>();
        BTree current_tree;
        ArrayList<Integer> temp;

        for (int i = 0; i < commands.length; i++) {
            for (int j = 0; j < columns.length; j++) {
                if (kolon_adlari[i].equals(columns[j])) { // j. b-tree uzerinden ilgili key ile ona ait value'larin listesini bul
                    current_tree = b_agaclari.get(j);
                    if (i == 0) {
                        yazdirilacak_satirlar = (ArrayList<Integer>) current_tree.get(degerler[i]);
                    } else {
                        temp = (ArrayList<Integer>) current_tree.get(degerler[i]);
                        yazdirilacak_satirlar = (ArrayList<Integer>) intersection(yazdirilacak_satirlar, temp);
                    }
                    break;

                }
            }
        }


        for (int i = 0; i < yazdirilacak_satirlar.size(); i++) {
            yeni_tablo.rows.add(rows.get(yazdirilacak_satirlar.get(i)));
        }

        return yeni_tablo;

    }

    public <T> List<T> intersection(List<T> list1, List<T> list2) {
        List<T> list = new ArrayList<T>();

        for (T t : list1) {
            if (list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }

    // This method projects only set of columns from the table and forms a new table
    // including all rows but only selected columns
    // columnsList is comma separated list of columns i.e., "id,email,ip_address"

    public Table project(String columnsList) {

        columnsList.replace("\"", "");
        String[] kolonlar = columnsList.split(",");

        Table yeni_tablo = new Table(kolonlar);

        ArrayList<Integer> yazdirilacak_kolon_index = new ArrayList<Integer>();

        for (int i = 0; i < kolonlar.length; i++) {
            for (int j = 0; j < this.columns.length; j++) {
                if (this.columns[j].equals(kolonlar[i])) {
                    yazdirilacak_kolon_index.add(j);
                }
            }
        }

        for (int i = 0; i < this.rows.size(); i++) {
            StringBuilder temp = new StringBuilder();

            for (int j = 0; j < yazdirilacak_kolon_index.size(); j++) {
                temp.append(this.rows.get(i).getColumnAtIndex(yazdirilacak_kolon_index.get(j)));

                if (j != yazdirilacak_kolon_index.size() - 1)
                    temp.append(",");
            }

            Row yeni_row = new Row(temp.toString());

            yeni_tablo.rows.add(yeni_row);
        }

        return yeni_tablo;

    }

    /***
     * Print column names in the first line
     * Print the rest of the table
     */

    public void show() {

        System.out.println(String.join(",", columns) + "\n");
        for (Row rw : rows) {
            System.out.println(rw.toString() + "\n");
        }

    }


    public static void main(String[] args) {
        Table tb = new Table("/Users/kaan/Desktop/BIL212/odev_2/hw99/userLogs.csv"); // change path as you like
        // tb.show(); // should print everything
        // tb.filter("id==3").project("first_name").show(); // should print Aldon

        // This is suppposed to print Jobling,sjoblingi@w3.org
        tb.filter("id==19 AND ip_address==242.40.106.103").project("last_name,email").show();
        //tb.filter("first_name==Erich").project("email,last_name").show();;

        // amathewesg@slideshare.net
        // imathewesdx@ucoz.com
        // tb.filter("last_name==Mathewes").project("email,id").show();

        // tb.filter("first_name==Kimberley").project("id,ip_address").show();

        // We might test with a different csv file with same format but different column
        // count
        // Good luck!!

    }
}

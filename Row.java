package hw99;

public class Row
{
    private String[] data;
    String info;

    public Row(String str)
    {
        info = str;
        data = info.split(",");
    }

    public String getColumnAtIndex(int i)
    {
        return data[i];
    }
    public String toString(){
        return info;
    }
}
package model;

public class Instrument {
    private int id;
    private String name;

    public Instrument(String name) { this.name = name; }
    public Instrument(int id, String name) { this.id = id; this.name = name; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() { return name; }

    // Статический список для выпадающих списков в GUI
    public static String[] getAllInstruments() {
        return new String[]{"Барабаны", "Гитарный комбик", "Бас-гитара",
                "Вокальный тракт", "Клавиши", "Саксофон"};
    }
}
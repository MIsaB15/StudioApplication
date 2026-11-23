package model;

import java.util.ArrayList;
import java.util.List;

public class Studio {
    private int id;
    private String name;
    private String address;
    private double pricePerHour;
    private double rating;
    private List<Instrument> instruments;
    private String description;

    public Studio(String name, String address, double pricePerHour,
                  double rating, List<Instrument> instruments, String description) {
        this.name = name;
        this.address = address;
        this.pricePerHour = pricePerHour;
        this.rating = rating;
        this.instruments = instruments != null ? instruments : new ArrayList<>();
        this.description = description;
    }

    // Конструктор для загрузки из БД
    public Studio(int id, String name, String address, double pricePerHour,
                  double rating, List<Instrument> instruments, String description) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.pricePerHour = pricePerHour;
        this.rating = rating;
        this.instruments = instruments != null ? instruments : new ArrayList<>();
        this.description = description;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(double pricePerHour) { this.pricePerHour = pricePerHour; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public List<Instrument> getInstruments() { return instruments; }
    public void setInstruments(List<Instrument> instruments) { this.instruments = instruments; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getInstrumentsAsString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < instruments.size(); i++) {
            sb.append(instruments.get(i).getName());
            if (i < instruments.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return name + " | " + pricePerHour + "₽/ч" + rating;
    }
}
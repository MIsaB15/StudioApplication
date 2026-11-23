package model;

import java.util.HashSet;
import java.util.Set;

public class SearchFilter {
    private double minPrice = 0;
    private double maxPrice = Double.MAX_VALUE;
    private double minRating = 0;
    private Set<String> requiredInstruments = new HashSet<>();

    public double getMinPrice() { return minPrice; }
    public void setMinPrice(double minPrice) { this.minPrice = minPrice; }
    public double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(double maxPrice) { this.maxPrice = maxPrice; }
    public double getMinRating() { return minRating; }
    public void setMinRating(double minRating) { this.minRating = minRating; }
    public Set<String> getRequiredInstruments() { return requiredInstruments; }
    public void addInstrument(String instrument) { this.requiredInstruments.add(instrument); }
    public void clearInstruments() { this.requiredInstruments.clear(); }
}
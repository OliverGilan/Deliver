package com.olivergilan.deliver;

import android.location.Location;

import java.util.ArrayList;

public class Order {

    private ArrayList<Product> items;
    private int itemCount;
    private int totalCost = 0;
    private double longitude;
    private double latitude;


    public Order(ArrayList<Product> products, double mlong, double mlat){
        items = products;
        itemCount = items.size();
        for (Product item: items) {
            totalCost += item.getCost();
        }
        longitude = mlong;
        latitude = mlat;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public ArrayList<Product> getItems() {
        return items;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getTotalCost() {
        return totalCost;
    }
}

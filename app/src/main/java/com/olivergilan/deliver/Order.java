package com.olivergilan.deliver;

import android.location.Location;

public class Order {

    private Product[] items;
    private int itemCount;
    private int totalCost = 0;
    private double longitude;
    private double latitude;


    public Order(Product[] products, double mlong, double mlat){
        items = products;
        itemCount = items.length;
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

    public Product[] getItems() {
        return items;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getTotalCost() {
        return totalCost;
    }
}

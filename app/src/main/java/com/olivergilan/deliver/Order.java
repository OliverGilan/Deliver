package com.olivergilan.deliver;

import android.location.Location;

import com.google.android.gms.location.places.Place;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class Order {

    private FirebaseUser customer;
    private ArrayList<Product> items;
    private Place pickupLocation;
    private int itemCount;
    private int totalCost = 0;
    private double longitude;
    private double latitude;


    public Order(ArrayList<Product> products, double mlong, double mlat, FirebaseUser user){
        items = products;
        itemCount = items.size();
        for (Product item: items) {
            totalCost += item.getCost();
        }
        longitude = mlong;
        latitude = mlat;
        customer = user;
    }

    public Order(ArrayList<Product> products, Place pickup, FirebaseUser user){
        items = products;
        itemCount = items.size();
        for (Product item: items) {
            totalCost += item.getCost();
        }
        pickupLocation = pickup;
        longitude = pickup.getLatLng().longitude;
        latitude = pickup.getLatLng().latitude;
        customer = user;
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

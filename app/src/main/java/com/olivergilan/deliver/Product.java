package com.olivergilan.deliver;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by olivergilan on 1/7/18.
 */

public class Product implements Parcelable{

    private String name;
    private String description;
    private double cost;

    public Product(String nameIn, double costIn){
        name = nameIn;
        cost = costIn;
    }

    public Product(Parcel in){
        readFromParcel(in);
    }

    public Product(){
        name="";
        cost=0;
    }

    public void setName(String nameIn){
        name = nameIn;
    }

    public String getName(){
        return name;
    }

    public void setCost(double costIn){
        cost = costIn;
    }

    public double getCost(){
        return cost;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeDouble(cost);
    }

    private void readFromParcel(Parcel in) {
        name = in.readString();
        cost = in.readDouble();
    }


    public static final Parcelable.Creator<Product> CREATOR
            = new Parcelable.Creator<Product>() {
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        public Product[] newArray(int size) {
            return new Product[size];
        }
    };
}

package com.olivergilan.deliver;

/**
 * Created by olivergilan on 1/7/18.
 */

public class Product {

    private String name;
    private String description;
    private double cost;

    public Product(String nameIn, double costIn){
        name = nameIn;
        cost = costIn;
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

}

package com.olivergilan.deliver;

public class MarkerTag {
    String id;
    Order order;

    public MarkerTag(String id, Order order){
        this.id = id;
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

    public String getId() {
        return id;
    }
}

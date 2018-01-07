package com.olivergilan.deliver;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ListItems extends AppCompatActivity {

    ListView list;
    EditText nameItem, itemCost;
    Button addItem;
    Button finishedBtn;
    ArrayList<String> items;
    ArrayList<Product> products;
    ArrayList<Double> costs;
    ArrayAdapter<Product> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_items);

        list = (ListView) findViewById(R.id.list);
        nameItem = (EditText) findViewById(R.id.itemName);
        itemCost = (EditText) findViewById(R.id.itemCost);
        addItem = (Button) findViewById(R.id.addItemBtn);
        finishedBtn = (Button) findViewById(R.id.doneBtn);
        items = new ArrayList<>();
        products = new ArrayList<>();
        costs = new ArrayList<>();


        arrayAdapter = new ArrayAdapter<Product>(this, android.R.layout.simple_list_item_2, android.R.id.text1, products){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(products.get(position).getName());
                text2.setText(Double.toString(products.get(position).getCost()));
                return view;
            }
        };
        list.setAdapter(arrayAdapter);

        addItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(nameItem.getText().toString().trim().matches("") || itemCost.getText().toString().trim().matches("")){
                    Toast.makeText(ListItems.this, "Enter name and estimated cost for item!", Toast.LENGTH_SHORT).show();
                    nameItem.requestFocus();
                } else {
                    String item = nameItem.getText().toString().trim();
                    double cost = Double.parseDouble(itemCost.getText().toString().trim());
                    Product product = new Product(item, cost);

                    //products.add(product);
                    arrayAdapter.add(product);
                    items.add(product.getName());
                    costs.add(product.getCost());

                    nameItem.setText("");
                    itemCost.setText("");
                }
            }
        });

        finishedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }
}

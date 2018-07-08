package com.olivergilan.deliver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.LauncherActivity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class ListItems extends AppCompatActivity {

    ListView list;
    EditText nameItem, itemCost;
    Button addItem;
    Button finishedBtn;
    private ArrayList<String> items;
    private ArrayList<Product> products;
    private ArrayList<Double> costs;
    ArrayAdapter<Product> arrayAdapter;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase mDatabase;
    DatabaseReference mRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_items);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, SignUp.class));
        }
        mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference("orders");


        list = (ListView) findViewById(R.id.list);
        nameItem = (EditText) findViewById(R.id.itemName);
        itemCost = (EditText) findViewById(R.id.itemCost);
        addItem = (Button) findViewById(R.id.addItemBtn);
        finishedBtn = (Button) findViewById(R.id.doneBtn);
        items = new ArrayList<>();
        products = new ArrayList<>();
        costs = new ArrayList<>();


        arrayAdapter = new ArrayAdapter<Product>(this, android.R.layout.simple_list_item_2, android.R.id.text1){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                text1.setText(products.get(position).getName());
                text2.setText("$" + Double.toString(products.get(position).getCost()));
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

                    products.add(product);
                    arrayAdapter.add(product);
//                    items.add(product.getName());
//                    costs.add(product.getCost());

                    nameItem.setText("");
                    itemCost.setText("");
                    nameItem.requestFocus();
                }
            }
        });

        finishedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(products.size() == 0){
                    startActivity(new Intent(ListItems.this, MainActivity.class));
                }else{
                    Intent intent = new Intent(ListItems.this, setOrderLocation.class);
                    intent.putExtra("items", products);
                    startActivity(intent);
                }
            }
        });
    }

}

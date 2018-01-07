package com.olivergilan.deliver;

import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class buildProfile extends AppCompatActivity {

    String name, email, password;
    ImageView profilePic;
    Button take, select, skip;
    TextView displayName;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    DatabaseReference mRef;
    private FirebaseStorage storage;
    StorageReference mStorageRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_profile);

        take = (Button) findViewById(R.id.takePhotoBtn);
        select = (Button) findViewById(R.id.selectPhotoBtn);
        skip = (Button) findViewById(R.id.skipBtn);
        displayName = (TextView) findViewById(R.id.displayName);

        profilePic = (ImageView) findViewById(R.id.profileImage);
        storage = FirebaseStorage.getInstance();
        mStorageRef = storage.getReference();
        StorageReference profileRef = mStorageRef.child("default_profile.png");
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        Intent intent = getIntent();
        name = intent.getStringExtra("username").toString().trim();
        email = mAuth.getCurrentUser().getEmail().toString().trim();

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name.toString().trim())
                .setPhotoUri(Uri.parse(profileRef.getPath()))
                .build();

        Glide.with(this.getApplicationContext())
                .using(new FirebaseImageLoader())
                .load(profileRef)
                .into(profilePic);

        displayName.setText(user.getDisplayName());

        take.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeProfilePic();
            }
        });

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectProfilePic();
            }
        });

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(buildProfile.this, MainActivity.class));
            }
        });

    }

    public void takeProfilePic(){

    }

    public void selectProfilePic(){

    }
}

package com.olivergilan.deliver;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.regex.Pattern;

public class SignUp extends AppCompatActivity{

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private FirebaseFirestore db;
    private DatabaseReference mRef;
    private EditText email, password, name;
    private Button signUpBtn;
    private TextView loginBtn;
    private ProgressBar progressbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        email = (EditText) findViewById(R.id.emailCreate);
        password = (EditText) findViewById(R.id.passwordCreate);
        signUpBtn = (Button) findViewById(R.id.signUpBtn);
        loginBtn = (TextView) findViewById(R.id.toLogin);
        name = (EditText) findViewById(R.id.name);
        progressbar = findViewById(R.id.progress);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        mRef = database.getReference("users");
        db = FirebaseFirestore.getInstance();

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerUser();
            }
        });
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignUp.this, Login.class));
            }
        });
    }


    public void registerUser() {
        String email = this.email.getText().toString().trim();
        String password = this.password.getText().toString().trim();
        final String username = this.name.getText().toString().trim();

        if(username.isEmpty()){
            this.name.setError("Name is required");
            this.name.requestFocus();
            return;
        }
        if(email.isEmpty()){
            this.email.setError("Email is required");
            this.email.requestFocus();
            return;
        }
        if(password.isEmpty()){
            this.password.setError("Password is required");
            this.password.requestFocus();
            return;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            this.email.setError("Please enter valid email!");
            this.email.requestFocus();
            return;
        }
        if(password.length()< 6){
            this.password.setError("Password must be at least 6 characters!");
            this.password.requestFocus();
            return;
        }
        progressbar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(SignUp.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            progressbar.setVisibility(View.INVISIBLE);
                            final FirebaseUser user = mAuth.getCurrentUser();
                            user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(username).build())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            db.collection("users")
                                                    .document(user.getUid())
                                                    .set(user);
                                        }
                                    });

                            user.sendEmailVerification().addOnCompleteListener(SignUp.this, new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(getApplicationContext(), "Verification Email Sent", Toast.LENGTH_SHORT).show();
                                }
                            });
                            Intent intent = new Intent(SignUp.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }else{
                            progressbar.setVisibility(View.INVISIBLE);
                            if(task.getException() instanceof FirebaseAuthUserCollisionException){
                                Toast.makeText(getApplicationContext(), "Email already registered to an account", Toast.LENGTH_LONG).show();
                            } else {
                                progressbar.setVisibility(View.INVISIBLE);
                                Log.e("Signup Error", "onCancelled", task.getException());
                                Toast.makeText(getApplicationContext(), "Sign Up Failed", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

}

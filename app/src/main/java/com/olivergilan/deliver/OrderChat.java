package com.olivergilan.deliver;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class OrderChat extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private FirebaseFirestore database;
    private String chatRef;

    private ListView chatList;
    private ChatData chatData;
    private ChatAdapter adapter;
    private ImageButton sendBtn;
    private EditText messageCompose;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_chat);

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();
        database = FirebaseFirestore.getInstance();
        chatRef = getIntent().getExtras().getString("path");

        chatList = (ListView) findViewById(R.id.messageList);
        sendBtn = (ImageButton) findViewById(R.id.messageSendBtn);
        messageCompose = (EditText) findViewById(R.id.messageTextEdit);

        loadMessages();

        if(getIntent().hasExtra("arrived")){
            messageCompose.setText("I have arrived with your package!");
            sendMessage();
            alert();
        }

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
    }

    private void loadMessages(){
        database.document(chatRef).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        chatData = document.toObject(ChatData.class);
                        Log.i("CHAT", chatData.getHead().toString());
                        adapter = new ChatAdapter(OrderChat.this, chatData);
                        chatList.setAdapter(adapter);
                    } else {
                        Log.d("CHAT", "No such document");
                    }
                } else {
                    Log.d("CHAT", "get failed with ", task.getException());
                }
            }
        });
    }

    private void sendMessage(){
        if(messageCompose.getText().toString().trim() == ""){
            Toast.makeText(this, "Cannot send empty message!", Toast.LENGTH_SHORT);
        }else{
            String message = messageCompose.getText().toString().trim();
            ChatMessage chatMessage = new ChatMessage(message, mCurrentUser.getDisplayName(), chatData.getHead());
            chatData.addMessage(chatMessage);
            database.document(chatRef).set(chatData);
            messageCompose.setText("");
            loadMessages();
        }
    }

    private void alert(){
        database.document(chatRef).update("Arrived", true);
    }
}

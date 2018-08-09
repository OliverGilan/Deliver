package com.olivergilan.deliver;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.StringTokenizer;

import javax.annotation.Nullable;

public class FollowOrder extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private FirebaseFirestore database;
    private String chatRef;

    private ListView chatList;
    private ChatData chatData;
    private ChatAdapter adapter;
    private ImageButton sendBtn;
    private EditText messageCompose;
    private Button receivedOrderBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_order);

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();
        database = FirebaseFirestore.getInstance();

        chatList = (ListView) findViewById(R.id.messageList);
        sendBtn = (ImageButton) findViewById(R.id.messageSendBtn);
        messageCompose = (EditText) findViewById(R.id.messageTextEdit);
        receivedOrderBtn = (Button) findViewById(R.id.orderReceived);

        chatRef = getIntent().getExtras().getString("chatPath");

        loadMessages();

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        receivedOrderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent  = new Intent(FollowOrder.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                String orderRef = "";
                String chars = "/";
                StringTokenizer st = new StringTokenizer(chatRef, chars, true);
                while(st.hasMoreTokens()){
                    String cheese = st.nextToken();
                    if(cheese.matches("chats")){
                        orderRef += "activeOrders";
                    }else{
                        orderRef += cheese;
                    }
                }
                Log.i("CHATS", orderRef);
                database.document(orderRef).delete();
                database.document(chatRef).delete();
                startActivity(intent);
            }
        });

        database.document(chatRef).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(e != null){
                    Log.i("CHAT", "Failed to listen");
                }
                if(documentSnapshot != null && documentSnapshot.exists()){
                    loadMessages();
                }
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
                        if(document.get("Arrived") != null){
                            receivedOrderBtn.setVisibility(View.VISIBLE);
                        }
                        chatData = document.toObject(ChatData.class);
                        Log.i("CHAT", chatData.getHead().toString());
                        adapter = new ChatAdapter(FollowOrder.this, chatData);
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
}

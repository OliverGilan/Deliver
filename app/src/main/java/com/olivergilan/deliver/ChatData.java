package com.olivergilan.deliver;

import java.util.LinkedList;

public class ChatData {

    private ChatMessage head;

    public ChatData(){
        head = null;
    }

    public ChatData(ChatMessage message){
        if(head == null){
            head = message;
        }
    }

    public void addMessage(ChatMessage message){
        if(head == null){
            head = message;
        }else{
            message.next = head;
            head = message;
        }
    }

    public ChatMessage getHead() {
        return head;
    }
}

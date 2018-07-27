package com.olivergilan.deliver;

public class ChatMessage {
    private String message;
    private String sender;
    private int timeStamp;
    public ChatMessage next;

    public ChatMessage(String message, String sender, int time, ChatMessage next){
        this.message = message;
        this.sender = sender;
        this.timeStamp = time;
        this.next = next;
    }

    public ChatMessage(String message, String sender, ChatMessage next){
        this.message = message;
        this.sender = sender;
        this.next = next;
    }

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }

    public int getTimeStamp() {
        return timeStamp;
    }

}

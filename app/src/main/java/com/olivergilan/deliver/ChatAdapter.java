package com.olivergilan.deliver;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import java.util.ArrayList;

public class ChatAdapter extends BaseAdapter{

    private Context context;
    private ChatData data;

    public ChatAdapter(Context context, ChatData data){
        this.context = context;
        this.data = data;
    }

    @Override
    public int getCount() {
        int counter = 0;
        if(data.getHead() == null){
            return 0;
        }else{
            ChatMessage ptr = data.getHead();
            while(ptr != null){
                counter++;
                ptr = ptr.next;
            }
        }
        return counter;
    }

    @Override
    public ChatMessage getItem(int i) {
        int counter = 0;
        ChatMessage ptr = data.getHead();
        while(counter < i){
            counter++;
            ptr = ptr.next;
        }
        return ptr;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.chat_adapter, viewGroup, false);
        TextView senderName = (TextView) rowView.findViewById(R.id.name);
        senderName.setText(getItem(i).getSender().toString());

        TextView messageText = (TextView) rowView.findViewById(R.id.messageText);
        messageText.setText(getItem(i).getMessage().toString());

        TextView time = (TextView) rowView.findViewById(R.id.timestamp);
        time.setText(Integer.toString(getItem(i).getTimeStamp()));

        return rowView;
    }
}

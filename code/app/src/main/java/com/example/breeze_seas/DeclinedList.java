package com.example.breeze_seas;

public class DeclinedList extends StatusList{
    public DeclinedList(Event event, int capacity) {
        super(event, -1);
    }


    @Override
    protected String getStatusName() {
        return "declined";
    }
}

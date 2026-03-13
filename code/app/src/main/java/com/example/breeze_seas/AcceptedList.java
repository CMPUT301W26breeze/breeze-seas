package com.example.breeze_seas;

public class AcceptedList extends StatusList{
    public AcceptedList(Event event, int capacity) {
        super(event, capacity);
    }
    @Override
    protected String getStatusName() {
        return "accepted";
    }
}

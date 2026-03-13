package com.example.breeze_seas;

public class PendingList extends StatusList {
    public PendingList(Event event, int capacity) {
        super(event, capacity);
    }


    public boolean tryUpdateCapacity(int newCapacity) {
        if (newCapacity < 0) return false;
        this.capacity = newCapacity;
        return true;
    }
    @Override
    protected String getStatusName() {
        return "pending";
    }
}

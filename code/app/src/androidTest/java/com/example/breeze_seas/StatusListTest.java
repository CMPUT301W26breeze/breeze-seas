package com.example.breeze_seas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.List;


@RunWith(AndroidJUnit4.class)
public class StatusListTest {

    @Test
    public void testBeyondWaitCapacity() {
        Event mockEvent = Mockito.mock(Event.class);
        Mockito.when(mockEvent.getEventId()).thenReturn("test_id");

        WaitingList list = new WaitingList(mockEvent, 1);
        User mockUser1 = Mockito.mock(User.class);
        Mockito.when(mockUser1.getDeviceId()).thenReturn("device_001");

        User mockUser2 = Mockito.mock(User.class);
        Mockito.when(mockUser2.getDeviceId()).thenReturn("device_002");
        list.getUserList().add(mockUser1);
        list.addUser(mockUser2, new StatusList.ListUpdateListener() {
            @Override
            public void onUpdate() {
                fail("Should have blocked the second user");
            }
            @Override
            public void onError(Exception e) {
                assertEquals("This list is currently full (1).", e.getMessage());
            }
        });
    }

    public void testBeyondPendingCapacity() {

        Event mockEvent = Mockito.mock(Event.class);
        Mockito.when(mockEvent.getEventId()).thenReturn("test_id");

        PendingList list = new PendingList(mockEvent, 1);
        User mockUser1 = Mockito.mock(User.class);
        Mockito.when(mockUser1.getDeviceId()).thenReturn("device_001");

        User mockUser2 = Mockito.mock(User.class);
        Mockito.when(mockUser2.getDeviceId()).thenReturn("device_002");
        list.getUserList().add(mockUser1);
        list.addUser(mockUser2, new StatusList.ListUpdateListener() {
            @Override
            public void onUpdate() {
                fail("Should have blocked the second user");
            }
            @Override
            public void onError(Exception e) {
                assertEquals("This list is currently full (1).", e.getMessage());
            }
        });

    }

    public void testBeyondAcceptedCapacity() {

        Event mockEvent = Mockito.mock(Event.class);
        Mockito.when(mockEvent.getEventId()).thenReturn("test_id");

        AcceptedList list = new AcceptedList(mockEvent, 1);
        User mockUser1 = Mockito.mock(User.class);
        Mockito.when(mockUser1.getDeviceId()).thenReturn("device_001");

        User mockUser2 = Mockito.mock(User.class);
        Mockito.when(mockUser2.getDeviceId()).thenReturn("device_002");
        list.getUserList().add(mockUser1);
        list.addUser(mockUser2, new StatusList.ListUpdateListener() {
            @Override
            public void onUpdate() {
                fail("Should have blocked the second user!");
            }
            @Override
            public void onError(Exception e) {
                assertEquals("This list is currently full (1).", e.getMessage());
            }
        });

    }

    public void testLotteryListFlow(){
        Event mockEvent = Mockito.mock(Event.class);
        Mockito.when(mockEvent.getEventId()).thenReturn("test_id");
        Lottery mockLottery=Mockito.mock(Lottery.class);
    }
}
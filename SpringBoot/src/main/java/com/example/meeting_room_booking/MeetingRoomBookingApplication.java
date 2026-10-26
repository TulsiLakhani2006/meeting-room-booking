package com.example.meeting_room_booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.meeting_room_booking.database.Database;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class MeetingRoomBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            MeetingRoomBookingApplication.class, args
        );
    }

    @PostConstruct
    public void initializeDatabase() {
        Database.initialize();
    }
}
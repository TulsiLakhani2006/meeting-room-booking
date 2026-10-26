package com.example.meeting_room_booking.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.meeting_room_booking.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> bookRoom(
            @RequestBody BookingRequest request) {

        Map<String, Object> result = bookingService.bookRoom(
                request.getRoomName(),
                request.getUserId(),
                request.getStart(),
                request.getEnd());

        String status = (String) result.get("status");

        if ("SUCCESS".equals(status)) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        }

        if ("CLASH".equals(status)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        if ("REJECTED".equals(status)) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.status(
                HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

@DeleteMapping("/{bookingId}")
public ResponseEntity<Map<String, Object>> cancelBooking(
        @PathVariable int bookingId) {

    Map<String, Object> result =
            bookingService.cancelBooking(bookingId);

    String status = (String) result.get("status");

    if ("SUCCESS".equals(status)) {
        return ResponseEntity.ok(result);
    }

    if ("REJECTED".equals(status)) {
        return ResponseEntity.badRequest().body(result);
    }

    if ("NOT_FOUND".equals(status)) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    return ResponseEntity.status(
            HttpStatus.INTERNAL_SERVER_ERROR).body(result);
}

@GetMapping("/free")
public ResponseEntity<Map<String, Object>> showFreeRooms(
        @RequestParam String start,
        @RequestParam String end,
        @RequestParam(defaultValue = "0") int minCapacity) {

    Map<String, Object> result =
            bookingService.showFreeRooms(start, end, minCapacity);

    String status = (String) result.get("status");

    if ("SUCCESS".equals(status)) {
        return ResponseEntity.ok(result);
    }

    if ("REJECTED".equals(status)) {
        return ResponseEntity.badRequest().body(result);
    }

    return ResponseEntity.status(
            HttpStatus.INTERNAL_SERVER_ERROR).body(result);
}

@PostMapping("/simulate")
public ResponseEntity<Map<String, Object>> simulateBookings() {

    Map<String, Object> result =
            bookingService.simulateBookings();

    return ResponseEntity.ok(result);
}
@GetMapping("/rooms/{roomName}")
public ResponseEntity<Map<String, Object>> showBookings(
        @PathVariable String roomName,
        @RequestParam String date) {

    Map<String, Object> result =
            bookingService.showBookings(roomName, date);

    String status = (String) result.get("status");

    if ("SUCCESS".equals(status)) {
        return ResponseEntity.ok(result);
    }

    if ("REJECTED".equals(status)) {
        return ResponseEntity.badRequest().body(result);
    }

    return ResponseEntity.status(
            HttpStatus.INTERNAL_SERVER_ERROR).body(result);
}
    public static class BookingRequest {
        private String roomName;
        private String userId;
        private String start;
        private String end;

        public String getRoomName() { return roomName; }
        public void setRoomName(String roomName) { this.roomName = roomName; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }
}
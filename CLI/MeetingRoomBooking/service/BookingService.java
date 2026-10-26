package service;

import database.Database;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import repository.BookingRepository;
import repository.RoomRepository;

public class BookingService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final int MAX_RETRIES = 5;
    private final BookingRepository bookingRepository =
        new BookingRepository();

private final RoomRepository roomRepository =
        new RoomRepository();
    private enum BookingStatus {
        SUCCESS, CLASH, REJECTED, ERROR
    }

    private static class BookingResult {
        BookingStatus status;
        String message;
        long bookingId;

        BookingResult(BookingStatus status, String message, long bookingId) {
            this.status = status;
            this.message = message;
            this.bookingId = bookingId;
        }
    }

    private interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

   private static Connection getDatabaseConnection() throws SQLException {
    return Database.getConnection();
}

    public void bookRoom(String roomName, String userId,
                         String start, String end) {

        BookingResult result = tryBookRoom(roomName, userId, start, end, Database::getConnection);

        if (result.status == BookingStatus.SUCCESS) {
            System.out.println(
                    "Booking successful. ID: " + result.bookingId);
        } else {
            System.out.println("Error: " + result.message);
        }
    }

    private String validateTime(String start, String end) {

        if (start == null || end == null) {
            return "Start and end times are required.";
        }

        LocalDateTime startTime;
        LocalDateTime endTime;

        try {
            startTime = LocalDateTime.parse(start, FORMATTER);
            endTime = LocalDateTime.parse(end, FORMATTER);
        } catch (DateTimeParseException e) {
            return "Invalid date/time. Use yyyy-MM-dd HH:mm.";
        }

        if (!startTime.isBefore(endTime)) {
            return "Start time must be before end time.";
        }

        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            return "Booking must be on the same day.";
        }

        long minutes = Duration.between(startTime, endTime).toMinutes();

        if (minutes < 15 || minutes > 240) {
            return "Duration must be 15 minutes to 4 hours.";
        }

        if (startTime.getMinute() % 15 != 0
                || endTime.getMinute() % 15 != 0
                || startTime.getSecond() != 0
                || endTime.getSecond() != 0
                || startTime.getNano() != 0
                || endTime.getNano() != 0) {
            return "Times must be in 15-minute steps.";
        }

        return null;
    }

    private BookingResult tryBookRoom(
            String roomName, String userId,
            String start, String end,
            ConnectionProvider provider) {

        if (roomName == null || roomName.isBlank()) {
            return new BookingResult(
                    BookingStatus.REJECTED, "Room name is required.", -1);
        }

        if (userId == null || userId.isBlank()) {
            return new BookingResult(
                    BookingStatus.REJECTED, "User ID is required.", -1);
        }

        String validationError = validateTime(start, end);

        if (validationError != null) {
            return new BookingResult(
                    BookingStatus.REJECTED, validationError, -1);
        }

        LocalDateTime startTime =
                LocalDateTime.parse(start, FORMATTER);
        LocalDateTime endTime =
                LocalDateTime.parse(end, FORMATTER);

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {

            try {
                return attemptBooking(
                        roomName, userId, startTime, endTime, provider);

            } catch (SQLException e) {

                if (isLockError(e) && attempt < MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(100L * (attempt + 1));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();

                        return new BookingResult(
                                BookingStatus.ERROR,
                                "Booking interrupted.", -1);
                    }
                } else {
                    return new BookingResult(
                            BookingStatus.ERROR,
                            "Database error: " + e.getMessage(), -1);
                }
            }
        }

        return new BookingResult(
                BookingStatus.ERROR,
                "Database remained locked after retries.", -1);
    }

    private BookingResult attemptBooking(
            String roomName, String userId,
            LocalDateTime startTime, LocalDateTime endTime,
            ConnectionProvider provider) throws SQLException {

        try (Connection connection = provider.getConnection()) {

            connection.setAutoCommit(false);

            try {
                try (Statement st = connection.createStatement()) {
                    st.execute("PRAGMA busy_timeout = 5000");
                    st.execute("PRAGMA foreign_keys = ON");

                    st.executeUpdate(
                            "UPDATE Room SET capacity = capacity " +
                            "WHERE id = (SELECT MIN(id) FROM Room)");
                }

                int roomId;

                String roomSQL =
                        "SELECT id FROM Room WHERE name = ?";

                try (PreparedStatement ps =
                             connection.prepareStatement(roomSQL)) {

                    ps.setString(1, roomName);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();

                            return new BookingResult(
                                    BookingStatus.REJECTED,
                                    "Room not found.", -1);
                        }

                        roomId = rs.getInt("id");
                    }
                }

                String countSQL =
                        "SELECT COUNT(*) FROM Booking " +
                        "WHERE user_id = ? AND status = 'ACTIVE' " +
                        "AND substr(start, 1, 10) = ?";

                try (PreparedStatement ps =
                             connection.prepareStatement(countSQL)) {

                    ps.setString(1, userId);
                    ps.setString(2,
                            startTime.toLocalDate().toString());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) >= 3) {
                            connection.rollback();

                            return new BookingResult(
                                    BookingStatus.REJECTED,
                                    "Daily booking limit reached.", -1);
                        }
                    }
                }

                String overlapSQL =
                        "SELECT id FROM Booking " +
                        "WHERE room_id = ? AND status = 'ACTIVE' " +
                        "AND start < ? AND end > ? LIMIT 1";

                try (PreparedStatement ps =
                             connection.prepareStatement(overlapSQL)) {

                    ps.setInt(1, roomId);
                    ps.setString(2, endTime.format(FORMATTER));
                    ps.setString(3, startTime.format(FORMATTER));

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int conflictId = rs.getInt("id");
                            connection.rollback();

                            return new BookingResult(
                                    BookingStatus.CLASH,
                                    "Conflicts with booking ID "
                                            + conflictId, -1);
                        }
                    }
                }

                String insertSQL =
                        "INSERT INTO Booking " +
                        "(room_id, user_id, start, end, status, created_at) " +
                        "VALUES (?, ?, ?, ?, 'ACTIVE', ?)";

                try (PreparedStatement ps =
                             connection.prepareStatement(insertSQL)) {

                    ps.setInt(1, roomId);
                    ps.setString(2, userId);
                    ps.setString(3, startTime.format(FORMATTER));
                    ps.setString(4, endTime.format(FORMATTER));
                    ps.setString(5,
                            LocalDateTime.now().format(FORMATTER));

                    ps.executeUpdate();
                }

                long bookingId;

                try (Statement st = connection.createStatement();
                     ResultSet rs =
                             st.executeQuery("SELECT last_insert_rowid()")) {

                    if (!rs.next()) {
                        throw new SQLException("Could not retrieve booking ID.");
                    }

                    bookingId = rs.getLong(1);
                }

                connection.commit();

                return new BookingResult(
                        BookingStatus.SUCCESS,
                        "Booking successful.", bookingId);

            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }

                throw e;
            }
        }
    }

    private boolean isLockError(SQLException e) {

        SQLException current = e;

        while (current != null) {
            String message = current.getMessage();

            if (message != null) {
                String lower = message.toLowerCase();

                if (lower.contains("database is locked")
                        || lower.contains("database is busy")
                        || lower.contains("sqlite_busy")
                        || lower.contains("sqlite_locked")) {
                    return true;
                }
            }

            int code = current.getErrorCode() & 0xFF;

            if (code == 5 || code == 6) {
                return true;
            }

            current = current.getNextException();
        }

        return false;
    }

    public void cancelBooking(int bookingId) {

        String sql =
                "UPDATE Booking SET status = 'CANCELLED' " +
                "WHERE id = ? AND status = 'ACTIVE'";

        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, bookingId);

            if (ps.executeUpdate() > 0) {
                System.out.println("Booking cancelled successfully.");
            } else {
                System.out.println(
                        "Booking not found or already cancelled.");
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public void showFreeRooms(String start, String end,
                              int minCapacity) {

        String error = validateTime(start, end);

        if (error != null) {
            System.out.println("Error: " + error);
            return;
        }

        if (minCapacity < 0) {
            System.out.println("Error: Capacity cannot be negative.");
            return;
        }

        String sql =
                "SELECT r.id, r.name, r.capacity FROM Room r " +
                "WHERE r.capacity >= ? " +
                "AND NOT EXISTS (" +
                "SELECT 1 FROM Booking b WHERE b.room_id = r.id " +
                "AND b.status = 'ACTIVE' " +
                "AND b.start < ? AND b.end > ?) " +
                "ORDER BY r.capacity ASC, r.name ASC";

        try (Connection connection = getDatabaseConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, minCapacity);
            ps.setString(2, end);
            ps.setString(3, start);

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;

                System.out.println("Available rooms:");

                while (rs.next()) {
                    found = true;

                    System.out.println(
                            "Room: " + rs.getString("name")
                                    + ", Capacity: "
                                    + rs.getInt("capacity"));
                }

                if (!found) {
                    System.out.println("No available rooms found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public void showBookings(String roomName, String date) {

        if (roomName == null || roomName.isBlank()) {
            System.out.println("Error: Room name is required.");
            return;
        }

        try {
            LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException e) {
            System.out.println(
                    "Error: Date must use yyyy-MM-dd format.");
            return;
        }

        String sql =
                "SELECT b.id, b.user_id, b.start, b.end, b.status " +
                "FROM Booking b JOIN Room r ON b.room_id = r.id " +
                "WHERE r.name = ? AND substr(b.start, 1, 10) = ? " +
                "ORDER BY b.start ASC";

        try (Connection connection = getDatabaseConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, roomName);
            ps.setString(2, date);

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;

                while (rs.next()) {
                    found = true;

                    System.out.println(
                            "Booking ID: " + rs.getInt("id")
                                    + ", User: " + rs.getString("user_id")
                                    + ", Start: " + rs.getString("start")
                                    + ", End: " + rs.getString("end")
                                    + ", Status: " + rs.getString("status"));
                }

                if (!found) {
                    System.out.println("No bookings found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public void simulate() {

        final int numberOfThreads = 20;

        AtomicInteger successful = new AtomicInteger();
        AtomicInteger clashes = new AtomicInteger();
        AtomicInteger otherFailures = new AtomicInteger();

        CountDownLatch ready = new CountDownLatch(numberOfThreads);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(numberOfThreads);

        Path testFile = null;

        try {
            testFile = Files.createTempFile("booking-test-", ".db");

            final String testUrl =
                    "jdbc:sqlite:" + testFile.toAbsolutePath();

            try (Connection connection =
                         DriverManager.getConnection(testUrl);
                 Statement st = connection.createStatement()) {

                st.execute("PRAGMA busy_timeout = 5000");
                st.execute("PRAGMA foreign_keys = ON");

                st.execute(
                        "CREATE TABLE Room (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT UNIQUE NOT NULL, " +
                        "capacity INTEGER NOT NULL)");

                st.execute(
                        "CREATE TABLE Booking (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "room_id INTEGER NOT NULL, " +
                        "user_id TEXT NOT NULL, " +
                        "start TEXT NOT NULL, " +
                        "end TEXT NOT NULL, " +
                        "status TEXT NOT NULL CHECK " +
                        "(status IN ('ACTIVE','CANCELLED')), " +
                        "created_at TEXT NOT NULL, " +
                        "FOREIGN KEY(room_id) REFERENCES Room(id))");

                st.execute(
                        "INSERT INTO Room(name, capacity) VALUES ('R3', 6)");
            }

            LocalDate testDate = LocalDate.now().plusDays(1);
            final String start = testDate + " 10:00";
            final String end = testDate + " 11:00";

            ConnectionProvider testProvider =
                    () -> DriverManager.getConnection(testUrl);

            for (int i = 1; i <= numberOfThreads; i++) {

                final int threadNumber = i;

                Thread thread = new Thread(() -> {
                    ready.countDown();

                    try {
                        startSignal.await();

                        BookingResult result = tryBookRoom(
                                "R3",
                                "SIM_USER_" + threadNumber,
                                start, end, testProvider);

                        if (result.status == BookingStatus.SUCCESS) {
                            successful.incrementAndGet();
                        } else if (result.status == BookingStatus.CLASH) {
                            clashes.incrementAndGet();
                        } else {
                            otherFailures.incrementAndGet();

                            System.out.println(
                                    "Thread " + threadNumber + ": "
                                            + result.message);
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        otherFailures.incrementAndGet();

                    } finally {
                        finished.countDown();
                    }
                });

                thread.start();
            }

            ready.await();
            startSignal.countDown();
            finished.await();

            int activeInSlot = 0;

            try (Connection connection =
                         DriverManager.getConnection(testUrl);
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT COUNT(*) FROM Booking " +
                         "WHERE room_id = 1 AND status = 'ACTIVE' " +
                         "AND start < ? AND end > ?")) {

                ps.setString(1, end);
                ps.setString(2, start);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        activeInSlot = rs.getInt(1);
                    }
                }
            }

            System.out.println("\nSimulation results:");
            System.out.println("Total threads: " + numberOfThreads);
            System.out.println("Successful bookings: " + successful.get());
            System.out.println("Clash failures: " + clashes.get());
            System.out.println("Other failures: " + otherFailures.get());
            System.out.println(
                    "Active bookings in test slot: " + activeInSlot);

            if (successful.get() == 1
                    && clashes.get() == 19
                    && otherFailures.get() == 0
                    && activeInSlot == 1) {
                System.out.println("PASS: Simulation results are correct.");
            } else {
                System.out.println(
                        "FAIL: Expected 1 success, 19 clashes, " +
                        "0 other failures, and 1 active booking.");
            }

        } catch (Exception e) {
            System.out.println("Simulation error: " + e.getMessage());

        } finally {
            if (testFile != null) {
                try {
                    Files.deleteIfExists(testFile);
                } catch (Exception e) {
                    System.out.println(
                            "Could not delete temporary database: "
                                    + e.getMessage());
                }
            }
        }
    }
}
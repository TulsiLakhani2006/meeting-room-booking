package com.example.meeting_room_booking.database;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String URL = "jdbc:sqlite:bookings.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() {

        String createRoomTable =
            "CREATE TABLE IF NOT EXISTS Room (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT UNIQUE NOT NULL, " +
            "capacity INTEGER NOT NULL)";

        String createBookingTable =
            "CREATE TABLE IF NOT EXISTS Booking (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "room_id INTEGER NOT NULL, " +
            "user_id TEXT NOT NULL, " +
            "\"start\" TEXT NOT NULL, " +
            "\"end\" TEXT NOT NULL, " +
            "status TEXT NOT NULL DEFAULT 'ACTIVE', " +
            "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY (room_id) REFERENCES Room(id))";

        try (Connection con = getConnection();
             Statement stmt = con.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute(createRoomTable);
            stmt.execute(createBookingTable);

            String countQuery = "SELECT COUNT(*) FROM Room";

            try (ResultSet rs = stmt.executeQuery(countQuery)) {
                if (rs.next() && rs.getInt(1) == 0) {

                    String insertRoom =
                        "INSERT INTO Room(name, capacity) VALUES (?, ?)";

                    try (PreparedStatement ps =
                             con.prepareStatement(insertRoom)) {

                        String[] names = {
                            "R1", "R2", "R3", "R4", "R5", "R6"
                        };

                        int[] capacities = {4, 4, 6, 8, 12, 20};

                        for (int i = 0; i < names.length; i++) {
                            ps.setString(1, names[i]);
                            ps.setInt(2, capacities[i]);
                            ps.executeUpdate();
                        }
                    }
                }
            }

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            throw new RuntimeException(
                "Database initialization failed: " + e.getMessage(), e
            );
        }
    }
}
package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String URL = "jdbc:sqlite:bookings.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = ON");

            String roomSQL =
                "CREATE TABLE IF NOT EXISTS Room (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT UNIQUE NOT NULL, " +
                "capacity INTEGER NOT NULL)";

            statement.executeUpdate(roomSQL);

            String bookingSQL =
                "CREATE TABLE IF NOT EXISTS Booking (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "room_id INTEGER NOT NULL, " +
                "user_id TEXT NOT NULL, " +
                "\"start\" TEXT NOT NULL, " +
                "\"end\" TEXT NOT NULL, " +
                "status TEXT NOT NULL CHECK " +
                "(status IN ('ACTIVE', 'CANCELLED')), " +
                "created_at TEXT NOT NULL, " +
                "FOREIGN KEY (room_id) REFERENCES Room(id))";

            statement.executeUpdate(bookingSQL);

            String insertSQL =
                "INSERT OR IGNORE INTO Room (name, capacity) VALUES " +
                "('R1', 4), ('R2', 4), ('R3', 6), " +
                "('R4', 8), ('R5', 12), ('R6', 20)";

            statement.executeUpdate(insertSQL);

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            throw new RuntimeException(
                "Database initialization failed", e
            );
        }
    }
}
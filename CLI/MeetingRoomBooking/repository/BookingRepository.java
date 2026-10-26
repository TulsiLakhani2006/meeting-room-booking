
package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BookingRepository {

    public int findRoomId(Connection connection, String roomName)
            throws SQLException {

        String sql = "SELECT id FROM Room WHERE name = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, roomName);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        }
    }

    public int countActiveBookings(
            Connection connection, String userId, String date)
            throws SQLException {

        String sql =
                "SELECT COUNT(*) FROM Booking " +
                "WHERE user_id = ? AND status = 'ACTIVE' " +
                "AND substr(start, 1, 10) = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, date);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int findConflictId(
            Connection connection, int roomId, String start, String end)
            throws SQLException {

        String sql =
                "SELECT id FROM Booking " +
                "WHERE room_id = ? AND status = 'ACTIVE' " +
                "AND start < ? AND end > ? LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, end);
            ps.setString(3, start);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        }
    }

    public long insertBooking(
            Connection connection,
            int roomId,
            String userId,
            String start,
            String end,
            String createdAt) throws SQLException {

        String sql =
                "INSERT INTO Booking " +
                "(room_id, user_id, start, end, status, created_at) " +
                "VALUES (?, ?, ?, ?, 'ACTIVE', ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, userId);
            ps.setString(3, start);
            ps.setString(4, end);
            ps.setString(5, createdAt);

            ps.executeUpdate();
        }

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        }

        throw new SQLException("Could not retrieve booking ID.");
    }

    public boolean cancelBooking(Connection connection, int bookingId)
            throws SQLException {

        String sql =
                "UPDATE Booking SET status = 'CANCELLED' " +
                "WHERE id = ? AND status = 'ACTIVE'";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            return ps.executeUpdate() > 0;
        }
    }
}

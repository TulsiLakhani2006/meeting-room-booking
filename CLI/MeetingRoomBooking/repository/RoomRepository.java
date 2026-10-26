
package repository;

import database.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Room;

public class RoomRepository {

    public List<Room> findAll() throws SQLException {

        List<Room> rooms = new ArrayList<>();

        String sql = "SELECT id, name, capacity FROM Room";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {

                Room room = new Room(
                    result.getInt("id"),
                    result.getString("name"),
                    result.getInt("capacity")
                );

                rooms.add(room);
            }
        }

        return rooms;
    }

    public Room findByName(String name) throws SQLException {

        String sql =
                "SELECT id, name, capacity FROM Room WHERE name = ?";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, name);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return new Room(
                        result.getInt("id"),
                        result.getString("name"),
                        result.getInt("capacity")
                    );
                }
            }
        }

        return null;
    }
}

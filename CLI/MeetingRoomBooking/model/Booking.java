package model;
public class Booking {
    private int id;
    private int roomId;
    private String userId;
    private String start;
    private String end;
    private String status;
    private String createdAt;

    public Booking(int id, int roomId, String userId,
                   String start, String end,
                   String status, String createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.userId = userId;
        this.start = start;
        this.end = end;
        this.status = status;
        this.createdAt = createdAt;
    }

    
    public int getId() {
        return id;
    }

    public int getRoomId() {
        return roomId;
    }

    public String getUserId() {
        return userId;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }


    public void setId(int id) {
        this.id = id;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
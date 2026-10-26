package presentation;
import database.Database;
import service.BookingService;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class Main {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        Database.initialize();

        BookingService service = new BookingService();

        System.out.println("Meeting Room Booking System");
        System.out.println("--------------------------------");
        System.out.println("Commands:");
        System.out.println("book [roomName userId \"start\" \"end\"]");
        System.out.println("cancel [bookingId]");
        System.out.println("free [\"start\" \"end\" minCapacity]");
        System.out.println("show [roomName date]");
        System.out.println("simulate");
        System.out.println("exit");
        System.out.println("--------------------------------");

        while (true) {

            System.out.print("> ");

            if (!sc.hasNextLine()) {
                break;
            }

            String line = sc.nextLine().trim();

            if (line.isEmpty()) {
                continue;
            }

            String[] parts = parseCommand(line);
            String command = parts[0].toLowerCase();

            try {
                switch (command) {

                    case "book":
                        handleBook(service, parts);
                        break;

                    case "cancel":
                        handleCancel(service, parts);
                        break;

                    case "free":
                        handleFree(service, parts);
                        break;

                    case "show":
                        handleShow(service, parts);
                        break;

                    case "simulate":
                        service.simulate();
                        break;

                    case "exit":
                        System.out.println("Exiting application.");
                        sc.close();
                        return;

                    default:
                        System.out.println(
                                "Unknown command. Use: book, cancel, "
                                        + "free, show, simulate, exit");
                }

            } catch (NumberFormatException e) {
                System.out.println(
                        "Error: Please enter a valid numeric value.");

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        sc.close();
    }

 
    private static String[] parseCommand(String line) {

        Pattern pattern = Pattern.compile("\"([^\"]*)\"|(\\S+)");
        Matcher matcher = pattern.matcher(line);

        java.util.ArrayList<String> values =
                new java.util.ArrayList<>();

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                values.add(matcher.group(1));
            } else {
                values.add(matcher.group(2));
            }
        }

        return values.toArray(new String[0]);
    }

    private static String getValue(
            String[] parts, int index, String prompt) {

        if (index < parts.length) {
            return parts[index];
        }

        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private static void handleBook(
            BookingService service, String[] parts) {

        String room = getValue(parts, 1, "Room name: ");
        String user = getValue(parts, 2, "User ID: ");
        String start = getValue(
                parts, 3, "Start (yyyy-MM-dd HH:mm): ");
        String end = getValue(
                parts, 4, "End (yyyy-MM-dd HH:mm): ");

        service.bookRoom(room, user, start, end);
    }

    private static void handleCancel(
            BookingService service, String[] parts) {

        String id = getValue(parts, 1, "Booking ID: ");
        service.cancelBooking(Integer.parseInt(id));
    }

    private static void handleFree(
        BookingService service, String[] parts) {

    String start = getValue(
            parts, 1, "Start (yyyy-MM-dd HH:mm): ");

    String end = getValue(
            parts, 2, "End (yyyy-MM-dd HH:mm): ");

    int capacity = 4; 

    if (parts.length == 4) {
     
        capacity = Integer.parseInt(parts[3]);

    } else if (parts.length == 6
            && parts[3].equals("--min-capacity")) {
       
        capacity = Integer.parseInt(parts[4]);

    } else if (parts.length != 3) {
        throw new IllegalArgumentException(
                "Usage: free \"start\" \"end\" [--min-capacity number]");
    }

    service.showFreeRooms(start, end, capacity);
}

    private static void handleShow(
            BookingService service, String[] parts) {

        String room = getValue(parts, 1, "Room name: ");
        String date = getValue(parts, 2, "Date (yyyy-MM-dd): ");

        service.showBookings(room, date);
    }
}

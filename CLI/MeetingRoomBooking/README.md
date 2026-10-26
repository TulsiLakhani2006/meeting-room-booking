# Meeting Room Booking System

A command-line-based Meeting Room Booking System developed using **Java and SQLite**. The application allows users to book and cancel meeting rooms, check room availability, view bookings, and test concurrent booking requests.

## Features

- **Book a Room:** Book a meeting room for a specified date and time.
- **Cancel a Booking:** Cancel an existing booking while preserving its history.
- **Check Available Rooms:** Find rooms available during a specified time slot based on minimum capacity.
- **View Bookings:** Display bookings for a particular room on a given date.
- **Booking Validation:** Validate room existence, booking time, and availability.
- **Daily Booking Limit:** Allow a maximum of 3 active bookings per user per day.
- **Conflict Prevention:** Prevent overlapping active bookings for the same room.
- **Database Transactions:** Use commit and rollback to maintain consistent booking operations.
- **Concurrency Testing:** Simulate 20 simultaneous booking requests to test double-booking prevention.
- **Persistent Storage:** Store booking data in an SQLite database.

## Technologies Used

- **Java** — Application logic and command-line interface.
- **JDBC (Java Database Connectivity)** — Connects Java to the database.
- **SQLite** — Lightweight relational database.
- **SQL** — Database queries and transaction operations.
- **Multithreading** — Simulates concurrent booking requests.

## Project Structure

```text
MeetingRoomBookingSystem/
├── Main.java
├── Database.java
├── BookingService.java
├── Room.java
├── Booking.java
├── BookingServiceTest.java
├── lib/
│   └── sqlite-jdbc.jar
├── bookings.db
└── README.md
```

**File descriptions:**

- `Main.java` — Entry point of the application. Handles user input and CLI commands.
- `Database.java` — Initializes the SQLite database and required tables.
- `BookingService.java` — Contains booking, cancellation, availability, and simulation logic.
- `Room.java` — Represents a meeting room.
- `Booking.java` — Represents a booking.
- `BookingServiceTest.java` — Contains booking service tests.
- `lib/` — Contains the SQLite JDBC driver JAR.
- `bookings.db` — SQLite database file generated or used by the application.
- `README.md` — Project documentation.

## Prerequisites

Make sure you have:

1. Java Development Kit (JDK) installed.
2. SQLite JDBC driver downloaded.
3. A terminal or command prompt.

Check your Java installation:

```bash
java -version
javac -version
```

## Setup and Installation

### 1. Download the SQLite JDBC Driver

Download the SQLite JDBC driver from:

https://github.com/xerial/sqlite-jdbc

Place the downloaded JAR file inside the `lib` directory.

For example:

```text
lib/sqlite-jdbc.jar
```

The actual JAR filename may include a version number.

### 2. Compile the Project

Open a terminal in the project directory.

**Windows:**

```bash
javac -cp ".;lib/*" *.java
```

**Linux/macOS:**

```bash
javac -cp ".:lib/*" *.java
```

### 3. Run the Application

**Windows:**

```bash
java -cp ".;lib/*" Main
```

**Linux/macOS:**

```bash
java -cp ".:lib/*" Main
```

The application initializes the database and displays the available commands.

## Available Commands

### 1. Book a Meeting Room

```text
book [roomName userId "start" "end"]
```

Example:

```text
book R3 U101 "2026-10-18 10:00" "2026-10-18 11:00"
```

Books room `R3` for user `U101` from 10:00 to 11:00 on October 18, 2026.

### 2. Cancel a Booking

```text
cancel [bookingId]
```

Example:

```text
cancel 5
```

Cancels the booking with ID `5`, subject to the application's cancellation rules.

### 3. Find Available Rooms

```text
free ["start" "end" minCapacity]
```

Example:

```text
free "2026-10-18 10:00" "2026-10-18 11:00" 6
```

Displays rooms available for the specified time slot with a minimum capacity of 6 people.

### 4. View Room Bookings

```text
show [roomName date]
```

Example:

```text
show R3 2026-10-18
```

Displays bookings for room `R3` on October 18, 2026.

### 5. Simulate Concurrent Bookings

```text
simulate
```

Creates a temporary SQLite database and starts 20 concurrent booking attempts for the same room and time slot.

The simulation reports:

- Total number of threads.
- Successful bookings.
- Booking clashes.
- Other failures.
- Number of active bookings in the tested time slot.

The expected result is one successful booking, 19 clashes, zero other failures, and one active booking in the tested slot.

The temporary test database is deleted after the simulation finishes.

### 6. Exit the Application

```text
exit
```

Closes the application.

## Booking Rules

The application applies the following booking rules:

1. The requested room must exist.
2. A user can have at most 3 active bookings per day.
3. A room cannot have two active bookings with overlapping time slots.
4. Bookings are stored with an `ACTIVE` status.
5. Cancelled bookings retain their history with a `CANCELLED` status.
6. Booking operations use database transactions to commit successful operations or roll back unsuccessful ones.
7. SQLite locking behavior is used to coordinate concurrent write operations.

The booking overlap condition is:

```sql
start < requested_end
AND end > requested_start
```

This allows one booking to begin exactly when another booking ends.

## Database Design

### Room Table

Stores meeting room information.

| Column | Description |
|---|---|
| `id` | Unique room ID |
| `name` | Room name |
| `capacity` | Maximum room capacity |

### Booking Table

Stores booking information.

| Column | Description |
|---|---|
| `id` | Unique booking ID |
| `room_id` | ID of the booked room |
| `user_id` | ID of the user |
| `start` | Booking start time |
| `end` | Booking end time |
| `status` | `ACTIVE` or `CANCELLED` |
| `created_at` | Booking creation timestamp |

The `room_id` column references the `Room` table.

## Concurrency Handling

The application uses SQLite transactions and write-locking behavior to reduce race conditions during simultaneous booking requests.

The booking process:

1. Opens a database connection.
2. Starts a transaction.
3. Acquires a write reservation before checking availability.
4. Validates the room and user's daily booking limit.
5. Checks for overlapping bookings.
6. Inserts the booking if all checks pass.
7. Commits the transaction or rolls it back if necessary.

The simulation helps verify that concurrent requests do not create multiple active bookings for the same room and time slot.

## Error Handling

The application handles common errors, including:

- Invalid numeric input.
- Room not found.
- Booking time conflicts.
- Daily booking limit exceeded.
- Database locking errors.
- SQL and database operation failures.

## Future Improvements

- Add a graphical user interface.
- Introduce user authentication and authorization.
- Add more comprehensive automated tests.
- Improve concurrent request retry handling.
- Add booking reports and search filters.

## Author

Developed as a Java-based database management project to demonstrate JDBC, SQLite, SQL queries, transaction handling, and multithreading.

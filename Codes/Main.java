import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/bomwee";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "001906";

    private static Connection connection;

    public static void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            createTableIfNotExists();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTableIfNotExists() throws SQLException {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS parked_cars (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "license_plate VARCHAR(20) NOT NULL," +
                "parking_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "removal_timestamp TIMESTAMP NULL," +
                "parking_fee INT DEFAULT 0)";
        Statement statement = connection.createStatement();
        statement.executeUpdate(createTableQuery);
    }

    public static void insertParkedCar(String licensePlate) {
        try {
            String insertQuery = "INSERT INTO parked_cars (license_plate) VALUES (?)";
            PreparedStatement statement = connection.prepareStatement(insertQuery);
            statement.setString(1, licensePlate);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeParkedCar(String licensePlate, int parkingFee) {
        try {
            String updateQuery = "UPDATE parked_cars SET removal_timestamp = CURRENT_TIMESTAMP, parking_fee = ? WHERE license_plate = ? AND removal_timestamp IS NULL";
            PreparedStatement statement = connection.prepareStatement(updateQuery);
            statement.setInt(1, parkingFee);
            statement.setString(2, licensePlate);
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected == 0) {
                System.out.println("The car is not parked here or it has already been removed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class ParkingSystem {

    static int totalSlots, availableSlots;
    static ArrayList<String> parkedCars = new ArrayList<>();
    static Map<String, Instant> parkingTimestamps = new HashMap<>();
    static final int RATE_PER_MINUTE = 5; // Rate in Nepali Rupees

    public static void main(String[] args) {
        DatabaseManager.initializeDatabase();

        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("ENTER THE TOTAL NUMBER OF PARKING SLOTS:");
            totalSlots = sc.nextInt();
            availableSlots = totalSlots;

            showAsciiArt(30); // This Calls the ASCII art method

            while (true) {
                displayMenu(); // Displaying the menu table
                int choice = sc.nextInt();

                switch (choice) {
                    case 1:
                        parkCar(sc);
                        showAsciiArt(30);
                        break;
                    case 2:
                        removeCar(sc);
                        showAsciiArt(30);
                        break;
                    case 3:
                        viewParkedCars();
                        break;
                    case 4:
                        calculateParkingFee(sc);
                        break;
                    case 5:
                        DatabaseManager.closeConnection();
                        System.exit(0);
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
        }
    }

    private static void displayMenu() {
        System.out.println("┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                             Parking Management System                       │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│ 1. Park a Car                                                               │");
        System.out.println("│ 2. Remove a Car                                                             │");
        System.out.println("│ 3. View Parked Cars                                                         │");
        System.out.println("│ 4. Calculate Parking Fee                                                    │");
        System.out.println("│ 5. Exit                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");
        System.out.print("Enter your choice: ");
    }

    public static void showAsciiArt(int borderLength) {
        final int slotWidth = 9; // Setting the desired width for each parking slot

        StringBuilder sb = new StringBuilder();
        sb.append(" _");
        int totalBorderLength = 32; // Setting the desired length for the top and bottom borders
        for (int i = 0; i < totalBorderLength; i++) {
            sb.append("_");
        }
        sb.append("_\n");

        sb.append("|");
        for (int i = 0; i < totalSlots; i++) {
            sb.append("|" + " ".repeat(slotWidth) + "|");
        }
        sb.append("|\n");

        for (int j = 0; j < 5; j++) {
            sb.append("|");
            for (int i = 0; i < totalSlots; i++) {
                if (i < parkedCars.size()) {
                    String licensePlate = parkedCars.get(i);
                    String formattedPlate = String.format("%-" + slotWidth + "s", licensePlate);
                    sb.append("|" + formattedPlate + "|");
                } else {
                    sb.append("|" + " ".repeat(slotWidth) + "|");
                }
            }
            sb.append("|\n");
        }

        sb.append("|_");
        for (int i = 0; i < totalBorderLength; i++) {
            sb.append("_");
        }
        sb.append("_\n");

        System.out.println(sb.toString());
    }

    public static void parkCar(Scanner sc) {
        if (availableSlots == 0) {
            System.out.println("Sorry, there are no available parking slots.");
            return;
        }

        sc.nextLine(); // Consume newline left-over
        System.out.println("Enter the license plate number of the car:");
        String licensePlate = sc.nextLine();
        parkedCars.add(licensePlate);
        parkingTimestamps.put(licensePlate, Instant.now());
        availableSlots--;
        System.out.println("Car parked successfully. Available slots: " + availableSlots);

        // Insert the parked car into the database
        DatabaseManager.insertParkedCar(licensePlate);
    }

    public static void removeCar(Scanner sc) {
        if (availableSlots == totalSlots) {
            System.out.println("There are no parked cars.");
            return;
        }

        sc.nextLine(); // Consume newline left-over
        System.out.println("ENTER THE LICENSE PLATE OF THE CAR TO BE REMOVED FROM SLOT:");
        String licensePlate = sc.nextLine();
        if (parkedCars.contains(licensePlate)) {
            parkedCars.remove(licensePlate);
            Instant parkingTimestamp = parkingTimestamps.remove(licensePlate);
            availableSlots++;
            System.out.println("Car removed successfully. Available slots: " + availableSlots);

            // Calculate and display the parking fee
            Instant removalTimestamp = Instant.now();
            long durationSeconds = Duration.between(parkingTimestamp, removalTimestamp).getSeconds();
            int parkingFee = calculateParkingFee(durationSeconds);
            System.out.println("Parking fee for " + licensePlate + ": NRs. " + parkingFee);

            // Update the parking fee in the database
            DatabaseManager.removeParkedCar(licensePlate, parkingFee);
        } else {
            System.out.println("The car is not parked here.");
        }
    }

    public static void viewParkedCars() {
        if (availableSlots == totalSlots) {
            System.out.println("There are no parked cars.");
            return;
        }

        System.out.println("Parked cars:");
        for (String licensePlate : parkedCars) {
            System.out.println(licensePlate);
        }
    }

    public static void calculateParkingFee(Scanner sc) {
        sc.nextLine(); // Consume newline left-over
        System.out.println("Enter the license plate number of the car:");
        String licensePlate = sc.nextLine();
        if (parkingTimestamps.containsKey(licensePlate)) {
            Instant parkingTimestamp = parkingTimestamps.get(licensePlate);
            Instant removalTimestamp = Instant.now();
            long durationSeconds = Duration.between(parkingTimestamp, removalTimestamp).getSeconds();
            int parkingFee = calculateParkingFee(durationSeconds);
            System.out.println("Parking fee for " + licensePlate + ": NRs. " + parkingFee);
        } else {
            System.out.println("The car is not parked here.");
        }
    }

    private static int calculateParkingFee(long durationSeconds) {
        long durationMinutes = durationSeconds / 60;
        return (int) (durationMinutes * RATE_PER_MINUTE);
    }
}
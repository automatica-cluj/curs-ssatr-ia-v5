package taxi;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * TaxiDriver - Console application for accepting taxi requests
 *
 * Features:
 * - Generates random driver ID
 * - Listens for taxi requests from clients on fanout exchange
 * - Can accept available orders
 * - Ignores orders confirmed by other drivers
 * - Handles order cancellations from clients
 */
public class TaxiDriver {
    private static final String FANOUT_EXCHANGE = "taxi_requests";

    private final String driverId;
    private final String driverQueue;
    private Connection connection;
    private Channel channel;

    // Track available orders: key = orderNumber, value = "clientId:location"
    private final Map<Integer, String> availableOrders;
    private int orderCounter;

    // Track confirmed orders (by client+location) to ignore them
    private final Set<String> confirmedOrders;

    // Track cancelled orders
    private final Set<String> cancelledOrders;

    public TaxiDriver() {
        this.driverId = generateDriverId();
        this.driverQueue = driverId + "_queue";
        this.availableOrders = new ConcurrentHashMap<>();
        this.confirmedOrders = Collections.synchronizedSet(new HashSet<>());
        this.cancelledOrders = Collections.synchronizedSet(new HashSet<>());
        this.orderCounter = 1;
    }

    /**
     * Generate a random driver ID using simple algorithm
     */
    private String generateDriverId() {
        Random random = new Random();
        int randomNum = 1000 + random.nextInt(9000); // 4-digit number
        return "Driver" + randomNum;
    }

    /**
     * Initialize RabbitMQ connection and setup queues
     */
    public void initialize() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("control.aut.utcluj.ro");
        factory.setPort(11156);
        factory.setUsername("***");
        factory.setPassword("***");

        connection = factory.newConnection();
        channel = connection.createChannel();

        // Declare the fanout exchange for broadcasting
        channel.exchangeDeclare(FANOUT_EXCHANGE, BuiltinExchangeType.FANOUT, true);

        // Create driver's queue (auto-delete when driver disconnects)
        channel.queueDeclare(driverQueue, false, false, true, null);

        // Bind driver queue to fanout exchange to receive all messages
        channel.queueBind(driverQueue, FANOUT_EXCHANGE, "");

        System.out.println("===========================================");
        System.out.println("     TAXI DRIVER - " + driverId);
        System.out.println("===========================================");
        System.out.println("Connected to RabbitMQ");
        System.out.println("Listening for taxi requests...");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  accept:<order_number>  - Accept an order");
        System.out.println("  list                   - List available orders");
        System.out.println("  quit                   - Exit application");
        System.out.println("===========================================");
        System.out.println();

        // Start listening for requests
        startListeningForRequests();
    }

    /**
     * Start listening for taxi requests on the fanout exchange
     */
    private void startListeningForRequests() {
        Thread listenerThread = new Thread(() -> {
            try {
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    handleIncomingMessage(message);
                };

                channel.basicConsume(driverQueue, true, deliverCallback, consumerTag -> {});
            } catch (IOException e) {
                System.err.println("Error listening for requests: " + e.getMessage());
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Handle incoming messages (requests, confirmations, cancellations)
     */
    private void handleIncomingMessage(String message) {
        String[] parts = message.split(":", 3);

        if (parts.length < 3) {
            return; // Invalid message format
        }

        String messageType = parts[0];
        String sender = parts[1];
        String location = parts[2];
        String orderKey = sender + ":" + location;

        switch (messageType) {
            case "request":
                // Check if order is already confirmed or cancelled
                if (confirmedOrders.contains(orderKey) || cancelledOrders.contains(orderKey)) {
                    return; // Ignore
                }

                // Add to available orders
                int orderNum = orderCounter++;
                availableOrders.put(orderNum, orderKey);

                System.out.println("\n📱 NEW REQUEST #" + orderNum);
                System.out.println("   Client: " + sender);
                System.out.println("   Location: " + location);
                System.out.println("   Type 'accept:" + orderNum + "' to accept this order");
                System.out.print("\n> ");
                break;

            case "confirmed":
                // Another driver confirmed this order
                if (!sender.equals(driverId)) {
                    confirmedOrders.add(orderKey);
                    // Remove from available orders
                    removeOrderByKey(orderKey);

                    System.out.println("\n⚠ Order taken by " + sender + " (Location: " + location + ")");
                    System.out.print("\n> ");
                }
                break;

            case "cancel":
                // Client cancelled the order
                cancelledOrders.add(orderKey);
                removeOrderByKey(orderKey);

                System.out.println("\n✖ Order cancelled by " + sender + " (Location: " + location + ")");
                System.out.print("\n> ");
                break;
        }
    }

    /**
     * Remove an order from available orders by its key (clientId:location)
     */
    private void removeOrderByKey(String orderKey) {
        Iterator<Map.Entry<Integer, String>> iterator = availableOrders.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> entry = iterator.next();
            if (entry.getValue().equals(orderKey)) {
                iterator.remove();
                break;
            }
        }
    }

    /**
     * Accept a taxi order and send confirmation
     */
    private void acceptOrder(int orderNumber) throws IOException {
        String orderKey = availableOrders.get(orderNumber);

        if (orderKey == null) {
            System.out.println("⚠ Invalid order number or order no longer available.");
            return;
        }

        String[] parts = orderKey.split(":");
        String clientId = parts[0];
        String location = parts[1];

        // Check if order was already confirmed or cancelled
        if (confirmedOrders.contains(orderKey) || cancelledOrders.contains(orderKey)) {
            System.out.println("⚠ This order is no longer available.");
            availableOrders.remove(orderNumber);
            return;
        }

        // Send confirmation to fanout exchange
        String confirmMessage = "confirmed:" + driverId + ":" + location;
        channel.basicPublish(FANOUT_EXCHANGE, "", null, confirmMessage.getBytes(StandardCharsets.UTF_8));

        // Mark as confirmed
        confirmedOrders.add(orderKey);
        availableOrders.remove(orderNumber);

        System.out.println("✓ Order #" + orderNumber + " ACCEPTED");
        System.out.println("  Client: " + clientId);
        System.out.println("  Location: " + location);
        System.out.println("  Confirmation sent to client!");
    }

    /**
     * List all available orders
     */
    private void listAvailableOrders() {
        if (availableOrders.isEmpty()) {
            System.out.println("No available orders at the moment.");
            return;
        }

        System.out.println("\n=== AVAILABLE ORDERS ===");
        for (Map.Entry<Integer, String> entry : availableOrders.entrySet()) {
            String[] parts = entry.getValue().split(":");
            System.out.println("#" + entry.getKey() + " - Client: " + parts[0] + ", Location: " + parts[1]);
        }
        System.out.println("========================");
    }

    /**
     * Start interactive console interface
     */
    public void startConsole() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input;

        System.out.print("> ");

        while ((input = reader.readLine()) != null) {
            input = input.trim();

            if (input.isEmpty()) {
                System.out.print("> ");
                continue;
            }

            if (input.equalsIgnoreCase("quit")) {
                System.out.println("Goodbye!");
                break;
            }

            if (input.startsWith("accept:")) {
                String orderNumStr = input.substring(7).trim();
                try {
                    int orderNum = Integer.parseInt(orderNumStr);
                    acceptOrder(orderNum);
                } catch (NumberFormatException e) {
                    System.out.println("⚠ Invalid order number. Example: accept:1");
                }
            } else if (input.equalsIgnoreCase("list")) {
                listAvailableOrders();
            } else {
                System.out.println("⚠ Unknown command. Use: accept:<order_number>, list, or quit");
            }

            System.out.print("\n> ");
        }

        cleanup();
    }

    /**
     * Clean up resources
     */
    private void cleanup() throws IOException {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (TimeoutException e) {
            System.err.println("Timeout during cleanup: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        TaxiDriver driver = new TaxiDriver();
        try {
            driver.initialize();
            driver.startConsole();
        } catch (IOException | TimeoutException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

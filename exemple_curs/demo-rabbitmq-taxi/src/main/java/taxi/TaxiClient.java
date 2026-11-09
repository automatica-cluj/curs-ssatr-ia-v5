package taxi;

import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 * TaxiClient - Console application for requesting taxi services
 *
 * Features:
 * - Generates random client ID
 * - Creates personal confirmation queue
 * - Sends taxi requests to fanout exchange (broadcast to all drivers)
 * - Receives confirmations from drivers on personal queue
 * - Can cancel pending orders
 */
public class TaxiClient {
    private static final String FANOUT_EXCHANGE = "taxi_requests";

    private final String clientId;
    private final String confirmationQueue;
    private Connection connection;
    private Channel channel;
    private String currentLocation;
    private boolean waitingForConfirmation;

    public TaxiClient() {
        this.clientId = generateClientId();
        this.confirmationQueue = clientId; // Queue name same as client ID
        this.waitingForConfirmation = false;
    }

    /**
     * Generate a random client ID using simple algorithm
     */
    private String generateClientId() {
        Random random = new Random();
        int randomNum = 1000 + random.nextInt(9000); // 4-digit number
        return "Client" + randomNum;
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

        // Declare the fanout exchange for broadcasting requests
        channel.exchangeDeclare(FANOUT_EXCHANGE, BuiltinExchangeType.FANOUT, true);

        // Create personal confirmation queue (durable, non-exclusive)
        channel.queueDeclare(confirmationQueue, true, false, false, null);

        // Bind confirmation queue to fanout exchange to receive confirmations
        channel.queueBind(confirmationQueue, FANOUT_EXCHANGE, "");

        System.out.println("===========================================");
        System.out.println("     TAXI CLIENT - " + clientId);
        System.out.println("===========================================");
        System.out.println("Connected to RabbitMQ");
        System.out.println("Personal confirmation queue: " + confirmationQueue);
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  request:<location>  - Request a taxi");
        System.out.println("  cancel              - Cancel current order");
        System.out.println("  quit                - Exit application");
        System.out.println("===========================================");
        System.out.println();

        // Start listening for confirmations in a separate thread
        startListeningForConfirmations();
    }

    /**
     * Start listening for driver confirmations on personal queue
     */
    private void startListeningForConfirmations() {
        Thread listenerThread = new Thread(() -> {
            try {
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    handleIncomingMessage(message);
                };

                channel.basicConsume(confirmationQueue, true, deliverCallback, consumerTag -> {});
            } catch (IOException e) {
                System.err.println("Error listening for confirmations: " + e.getMessage());
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Handle incoming messages (confirmations or other client requests)
     */
    private void handleIncomingMessage(String message) {
        String[] parts = message.split(":", 3);

        if (parts.length < 3) {
            return; // Invalid message format
        }

        String messageType = parts[0];
        String sender = parts[1];
        String location = parts[2];

        switch (messageType) {
            case "confirmed":
                // Check if this confirmation is for our current order
                if (waitingForConfirmation && location.equals(currentLocation)) {
                    System.out.println("\n✓ TAXI CONFIRMED by " + sender + " for location: " + location);
                    System.out.println("  Your taxi is on the way!");
                    waitingForConfirmation = false;
                    currentLocation = null;
                    System.out.print("\n> ");
                }
                break;

            case "request":
                // Ignore requests from other clients (drivers handle these)
                break;

            case "cancel":
                // Ignore cancellations from other clients
                break;
        }
    }

    /**
     * Send a taxi request to the fanout exchange
     */
    private void requestTaxi(String location) throws IOException {
        if (waitingForConfirmation) {
            System.out.println("⚠ You already have a pending request. Cancel it first or wait for confirmation.");
            return;
        }

        String message = "request:" + clientId + ":" + location;
        channel.basicPublish(FANOUT_EXCHANGE, "", null, message.getBytes(StandardCharsets.UTF_8));

        currentLocation = location;
        waitingForConfirmation = true;

        System.out.println("✓ Taxi requested for location: " + location);
        System.out.println("  Waiting for driver confirmation...");
    }

    /**
     * Cancel the current taxi order
     */
    private void cancelOrder() throws IOException {
        if (!waitingForConfirmation) {
            System.out.println("⚠ No pending order to cancel.");
            return;
        }

        String message = "cancel:" + clientId + ":" + currentLocation;
        channel.basicPublish(FANOUT_EXCHANGE, "", null, message.getBytes(StandardCharsets.UTF_8));

        System.out.println("✓ Order cancelled for location: " + currentLocation);
        waitingForConfirmation = false;
        currentLocation = null;
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

            if (input.startsWith("request:")) {
                String location = input.substring(8).trim();
                if (location.isEmpty()) {
                    System.out.println("⚠ Please specify a location. Example: request:Downtown");
                } else {
                    requestTaxi(location);
                }
            } else if (input.equalsIgnoreCase("cancel")) {
                cancelOrder();
            } else {
                System.out.println("⚠ Unknown command. Use: request:<location>, cancel, or quit");
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
        TaxiClient client = new TaxiClient();
        try {
            client.initialize();
            client.startConsole();
        } catch (IOException | TimeoutException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

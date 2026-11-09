/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ro.utcluj.rabbitmq.taxi;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Standalone helper which lists the broker queues through the RabbitMQ management HTTP API (not AMQP).
 * It is not used by the taxi apps and still targets a local broker.
 */
public class RabbitMQUtil {

    private static final String RABBITMQ_API = "http://localhost:15672/api/queues";
    private static final String USER = "guest"; // Change this to your RabbitMQ username
    private static final String PASSWORD = "guest"; // Change this to your RabbitMQ password

    public static List<String> getQueuesFromRabbitMQ() {
        List<String> queueNames = new ArrayList<>();
        try {
            URL url = new URL(RABBITMQ_API);
            String auth = Base64.getEncoder().encodeToString((USER + ":" + PASSWORD).getBytes());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Basic " + auth);
            
            String response = IOUtils.toString(conn.getInputStream(), "UTF-8");
            JSONArray queues = new JSONArray(response);

            for (int i = 0; i < queues.length(); i++) {
                JSONObject queue = queues.getJSONObject(i);
                String name = queue.getString("name");
                queueNames.add(name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return queueNames;
    }

    public static void main(String[] args) {
        List<String> queues = getQueuesFromRabbitMQ();
        System.out.println("Queues in RabbitMQ: " + queues);
    }
}

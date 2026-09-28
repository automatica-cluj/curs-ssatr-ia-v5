package utcn.aut;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.List;

public class MqttDemoMultipleTopics {
    public static void main(String[] args) {
        String broker = "tcp://control.aut.utcluj.ro:11188";
        String clientId = "JavaSample";
        MemoryPersistence persistence = new MemoryPersistence();

        // Create an ArrayList of topics to subscribe to
        List<String> topics = new ArrayList<>();
        topics.add("topic1");
        topics.add("topic2");
        // Add more topics to the list as needed

        configureMqttAndSubscribe(broker, clientId, persistence, topics);
    }

    public static void configureMqttAndSubscribe(String broker, String clientId, MemoryPersistence persistence, List<String> topics) {
        try {
            MqttClient client = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            System.out.println("Connecting to broker: " + broker);
            client.connect(connOpts);
            System.out.println("Connected");

            // Subscribe to all topics in the list
            for (String topic : topics) {
                client.subscribe(topic);
            }

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // Handle connection loss here
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("Message received on topic '" + topic + "': " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Delivery complete callback
                }
            });
        } catch (MqttException me) {
            me.printStackTrace();
        }
    }
}

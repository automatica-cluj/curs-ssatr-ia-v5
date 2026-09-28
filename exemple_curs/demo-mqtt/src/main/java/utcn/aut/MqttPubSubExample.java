package utcn.aut;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttPubSubExample {
    public static void main(String[] args) {
        String broker = "tcp://control.aut.utcluj.ro:11188";
        //String broker = "tcp://mosquitto-broker:1883";
        String clientId = "JavaCli-"+System.currentTimeMillis();
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient client = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            System.out.println("Connecting to broker: " + broker);
            client.connect(connOpts);
            System.out.println("Connected");

            String subscribeTopic = "topic1";
            String publishTopic = "topic2";

            // Subscribe to the first topic
            client.subscribe(subscribeTopic);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // Handle connection loss here
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // When a message is received on the subscribed topic
                    System.out.println("Message received on topic '" + topic + "': " + new String(message.getPayload()));

                    // Publish a message to the second topic
                    MqttMessage mqttMessage = new MqttMessage("Response Message".getBytes());
                    mqttMessage.setQos(0); // Adjust QoS as needed
                    client.publish(publishTopic, mqttMessage);
                    System.out.println("Response message published to topic '" + publishTopic + "'");
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


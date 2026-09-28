package utcn.aut;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttDemo {
    public static void main(String[] args) {
        String broker = "tcp://control.aut.utcluj.ro:11188";
        String clientId = "JavaSample";
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient client = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);

            System.out.println("Connecting to broker: " + broker);
            client.connect(connOpts);
            System.out.println("Connected");

            String topic = "sensor1";
            String message = "Hello, MQTT!";

            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(0);

            System.out.println("Publishing message: " + message);
            client.publish(topic, mqttMessage);
            System.out.println("Message published");

            client.subscribe(topic);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // Handle connection loss here
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("Message received: " + new String(message.getPayload()));
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

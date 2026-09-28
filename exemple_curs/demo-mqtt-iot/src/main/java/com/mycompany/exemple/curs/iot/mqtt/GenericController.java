/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.exemple.curs.iot.mqtt;

/**
 *
 * @author mihai
 */
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;

import java.util.ArrayList;
import java.util.StringTokenizer;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class GenericController implements MqttCallback {
    private ArrayList<String> topics;
    private MqttClient client;
    private final String actuatorTopic = "actuator";

    public GenericController(String broker, ArrayList<String> topics) {
        this.topics = topics;

        try {
            this.client = new MqttClient(broker, MqttClient.generateClientId(), new MemoryPersistence());
            this.client.setCallback(this);
            this.client.connect();
            System.out.println("Connected to MQTT broker.");
            // Subscribe to all topics
            for (String topic : this.topics) {
                this.client.subscribe(topic);
                System.out.println("Subscribed to "+topic+".");
            }
            
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        // Code to handle connection loss
        System.out.println("Connection lost.");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        controlMethod(topic, new String(message.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Called when a message has been delivered
        System.out.println("Message delivery complete.");
    }

    // Implementation of the control method
    protected void controlMethod(String topic, String message) {
        try {
            System.out.println("CALL");
            StringTokenizer st = new StringTokenizer(message, " ");
            double v = Double.parseDouble(st.nextToken());
            
            String command = (v>0)?"ON":"OFF";
            MqttMessage commandMessage = new MqttMessage(command.getBytes());
            client.publish(actuatorTopic, commandMessage);
        } catch (NumberFormatException e) {
            System.err.println("Received message is not a valid double: " + message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        ArrayList<String> list = new ArrayList<>();
        list.add("temperature");
        GenericController gc = new GenericController("tcp://control.aut.utcluj.ro:11188",list);
    }

    
}


/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.exemple.curs.iot.mqtt;

/**
 *
 * @author mihai
 */
import java.util.Locale;
import java.util.Observable;
import java.util.Random;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttException;

public class GenericMinMaxSensor extends Observable implements Runnable {
    private double minRange;
    private double maxRange;
    private String measurementUnit;
    private MqttClient client;
    private String mqttTopic;

    public GenericMinMaxSensor(double x, double y, String z, String broker, String topic) {
        this.minRange = x;
        this.maxRange = y;
        this.measurementUnit = z;
        this.mqttTopic = topic;
        try {
            this.client = new MqttClient(broker, MqttClient.generateClientId());
            this.client.connect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Random rand = new Random();
        while (!Thread.currentThread().isInterrupted()) {
            double generatedValue = minRange + (maxRange - minRange) * rand.nextDouble();
            // Locale.US so the decimal separator is always "." (a Romanian locale would write "," and the controller could not parse it)
            String formattedValue = String.format(Locale.US, "%.2f %s", generatedValue, measurementUnit);

            // Notify observers
            setChanged();
            notifyObservers(formattedValue);

            try {
                MqttMessage message = new MqttMessage(formattedValue.getBytes());
                client.publish(mqttTopic, message);
            } catch (MqttException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000); // Adjust the sleep time as needed
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        GenericMinMaxSensor vt = new GenericMinMaxSensor(-30,30, "C", "tcp://control.aut.utcluj.ro:11188", "temperature");
        Thread t = new Thread(vt);
        t.start();
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ro.utcluj.rabbitmq.sample;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author mihai
 */
public class RabbitMQReceiver implements DeliverCallback{

    private String QUEUE_NAME="bookingrequests";
    
    public void startReceiver() throws IOException, TimeoutException{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("control.aut.utcluj.ro");
        factory.setPort(11156);
        factory.setUsername("***");
        factory.setPassword("***");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        System.out.println("Start consuming messages...");
        channel.basicConsume(QUEUE_NAME, true, this, consumerTag -> { });
        //basicConsume() does not block, so this line prints right away; messages are handled later on a RabbitMQ library thread
        System.out.println("DUPA CALL CONSUME");
    }
    
   
    @Override
    public void handle(String string, Delivery dlvr) throws IOException {
        String message = new String(dlvr.getBody(), "UTF-8");
        System.out.println(" [x] Received '" + message + "'");
    }
    
    public static void main(String[] args) throws IOException, TimeoutException {
        RabbitMQReceiver rc = new RabbitMQReceiver();
        rc.startReceiver();       
    }
}

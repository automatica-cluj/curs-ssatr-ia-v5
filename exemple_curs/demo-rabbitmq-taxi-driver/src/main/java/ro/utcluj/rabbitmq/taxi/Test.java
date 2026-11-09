/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ro.utcluj.rabbitmq.taxi;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Entry point of the taxi demo: creates the exchange, then opens two clients and two drivers in the same process.
 *
 * @author mihai
 */
public class Test {
    
    /**
     * This is needed to configure fanout queue which will be used to broadcast orders to all dirvers. 
     * Each driver will have a queue bind to this fanout queue (see TaxiDriverApp code). 
     * 
     * @throws IOException
     * @throws TimeoutException 
     */
    public static void DeclareExchange() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("control.aut.utcluj.ro");
        factory.setPort(11156);
        factory.setUsername("***");
        factory.setPassword("***");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        //Declare fanout exchange. durable=true: the exchange survives a broker restart.
        channel.exchangeDeclare(TaxiNames.EXCHANGE, BuiltinExchangeType.FANOUT, true);
        channel.close();
    }
    
    public static void main(String[] args) throws IOException, TimeoutException {
        
        //must run first: drivers bind their queues to this exchange and clients publish on it
        DeclareExchange();
        
        TaxiClientApp c1 = new TaxiClientApp("Alin");
        c1.setVisible(true);
        TaxiClientApp c2 = new TaxiClientApp("Dan");
        c2.setVisible(true);
        
        TaxiDriverApp d1 = new TaxiDriverApp("Driver1");
        d1.setVisible(true);
        TaxiDriverApp d2 = new TaxiDriverApp("Driver2");
        d2.setVisible(true);
    }
}

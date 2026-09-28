/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package isp.multithread.programming.future;

import isp.multithread.programming.basic.TemperatureSensorT;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mihai.hulea
 */
public class Main {
    
    public static void test1(){
        Executor executor = Executors.newSingleThreadExecutor();
        BuyOrder1 bu1 = new BuyOrder1(10);
        executor.execute(bu1);
        System.out.printf("[1] Order buy price %d and total value is %d.\n",bu1.getPrice(), bu1.price*bu1.getQuantity());
        //this will not work and will print 0, why?
    }
    
    public static void test2(){
        Executor executor = Executors.newSingleThreadExecutor();
        BuyOrder1 bu1 = new BuyOrder1(10);
        executor.execute(bu1);
        //wait few seconds beofre printing the results.
        try{TimeUnit.SECONDS.sleep(4);}catch(Exception e){}
        System.out.printf("[2] Order buy price %d and total value is %d.\n",bu1.getPrice(), bu1.price*bu1.getQuantity());
        
    }   
    
    /**
     * Using a callable to submit a task, wait and get the result back.
     * 
     * @throws InterruptedException
     * @throws ExecutionException 
     */
    public static void test3() throws InterruptedException, ExecutionException{
        ExecutorService executors = Executors.newFixedThreadPool(10);
        BuyOrder2 bu1 = new BuyOrder2(5);
        Future<Integer> f = executors.submit(bu1);
        int buyPrice = f.get(); //this is a blocking call and is waiting till computation is done
        System.out.printf("[3] Order buy price %d and total value is %d.\n",buyPrice, bu1.price*bu1.getQuantity());
        
    }
    
    
    static int availableFunds = 1000000; //declare as static so that we can update it in the lambda function
    public static void test4() throws InterruptedException, ExecutionException{
        ExecutorService executors = Executors.newFixedThreadPool(10);
        
        //suply an async task which will be executed after computation done. No need to block and wait for computation domplete.
        CompletableFuture<Integer> f = CompletableFuture.supplyAsync(new BuyOrder3(10));
        f.thenAccept(p -> {System.out.println("Buy price is "+p);availableFunds-=p;});
        
        int k = 0; 
        //do something while computation is done in "background"
        while(k<10){
            System.out.println("Available funds "+availableFunds);
            TimeUnit.MILLISECONDS.sleep(800);
            k++;
        }//.finishi this loop after 10 iterations 
    }
    
    
    
    public static void main(String[] args) throws Exception {
        //test1();
        //test2();
        //test3();
        test4();
    }
   
}



class BuyOrder1 implements Runnable{

    int quantity;
    int price;

    public BuyOrder1(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPrice() {
        return price;
    }
    
    public void run(){
         try {
                Random r = new Random();
                price = r.nextInt(100);             
                System.out.println("[1] Buy sorder complete with price "+price);
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException ex) {
                Logger.getLogger(TemperatureSensorT.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
}

class BuyOrder2 implements Callable<Integer>{
    
    int quantity;
    int price;

    public BuyOrder2(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPrice() {
        return price;
    }
    
    
    @Override
    public Integer call() throws Exception {
         Random r = new Random();
         int price = r.nextInt(100);              
         System.out.println("[2] Buy sorder complete with price "+price);
        TimeUnit.SECONDS.sleep(2);
        return price;
    }
    
}

class BuyOrder3 implements Supplier<Integer>{
    
    int quantity;
    int price;

    public BuyOrder3(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPrice() {
        return price;
    }
    
    @Override
    public Integer get() {
        Random r = new Random();
        int price = r.nextInt(100);              
        System.out.println("[2] Buy sorder complete with price "+price);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException ex) {
            Logger.getLogger(BuyOrder3.class.getName()).log(Level.SEVERE, null, ex);
        }
        return price;
    }
    
}

package bgu.spl.gameClient;

import java.io.*;
import java.net.*;
 
public class Client {
  public static void main(String[] args) throws IOException{
    Socket clientSocket = null; // the connection socket
    BufferedReader in = null;
    BufferedReader userIn=null;
    String host = args[0];
    int port = Integer.decode(args[1]).intValue();

    System.out.println("Connecting to " + host + ":" + port);
    try {  // Trying to connect to a socket and initialize an output stream
      clientSocket = new Socket(host, port); // host and port
    } 
    catch (UnknownHostException e) {
      System.out.println("Unknown host: " + host);
      System.exit(1);
    } 
    catch (IOException e) {
      System.out.println("Couldn't get output to " + host + " connection");
      System.exit(1);
    }

    try { // Initialize an input stream
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF-8"));
      userIn= new BufferedReader(new InputStreamReader(System.in));
    } 
    catch (IOException e) {
      System.out.println("Couldn't get input to " + host + " connection");
      System.exit(1);
    }

    System.out.println("Connected to server!");
    readInput(clientSocket, in, userIn);

    userIn.close();
    in.close();
    clientSocket.close();
  }

  private static void readInput(Socket clientSocket, BufferedReader in, BufferedReader userIn) {
    boolean closedByServer=false;
    KeyboardReadingThread readingThread= new KeyboardReadingThread(clientSocket, userIn);
    Thread t=new Thread(readingThread);

    t.start();   
    try{
      while(true){
        String tmp=in.readLine();
        int len=tmp.length();
        if (tmp.contains("~")) {
          t.interrupt();
          break; 
        }
        System.out.print(tmp);
        if ((len>0 && tmp.charAt(tmp.length()-1)=='>') ||  tmp.contains("UNIDENTIFIED")){
          System.out.println("\n");
          synchronized(userIn){
            userIn.notifyAll();
          }
        }	
      }	
    }
    catch(IOException e){ 
      System.out.println("Server was disconnected. Please exit (with the command QUIT CLIENT)"); 
      closedByServer=true;
      t.interrupt();
    }
    if (!closedByServer)
      System.out.println("Exiting...");
  }
}
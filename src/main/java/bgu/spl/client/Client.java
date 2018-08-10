package bgu.spl.client;

import java.io.*;
import java.net.*;
 
public class Client {
    public static void main(String[] args) throws IOException{
        Socket clientSocket = null; // the connection socket
        BufferedReader in = null , userIn=null;
        String host = args[0], tmp;
        int port = Integer.decode(args[1]).intValue(), len;
        boolean closedByServer=false;
        System.out.println("Connecting to " + host + ":" + port);
        
        try {  // Trying to connect to a socket and initialize an output stream
            clientSocket = new Socket(host, port); // host and port
        } catch (UnknownHostException e) {
              System.out.println("Unknown host: " + host);
              System.exit(1);
        } catch (IOException e) {
            System.out.println("Couldn't get output to " + host + " connection");
            System.exit(1);
        }
        
        try { // Initialize an input stream
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF-8"));
            userIn= new BufferedReader(new InputStreamReader(System.in));
        } catch (IOException e) {
            System.out.println("Couldn't get input to " + host + " connection");
            System.exit(1);
        }

        System.out.println("Connected to server!");
        KeyboardReadingThread readingThread= new KeyboardReadingThread(clientSocket, userIn);
        Thread t=new Thread(readingThread);
        t.start();
        
        try{
	        while(true){
        		tmp=in.readLine();
        		len=tmp.length();
        		if (tmp.contains("~")) {
        			t.interrupt();
        			break; 
        		}
        		System.out.print(tmp);
        		if ((len>0 && tmp.charAt(tmp.length()-1)=='>') ||  tmp.contains("UNIDENTIFIED")){
        			System.out.println();
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
        userIn.close();
        in.close();
        clientSocket.close();
    }
}
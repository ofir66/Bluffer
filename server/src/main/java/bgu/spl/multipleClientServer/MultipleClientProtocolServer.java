package bgu.spl.multipleClientServer;

import java.io.*;
import java.net.*;

import bgu.spl.protocol.ProtocolCallbackImpl;
import bgu.spl.protocol.ServerProtocol;
import bgu.spl.protocol.ServerProtocolFactory;
import bgu.spl.protocol.TBGPFactory;
import bgu.spl.tokenizer.*;
import java.util.concurrent.atomic.AtomicInteger;
  
class ConnectionHandler implements Runnable {
    /**
     * represents the encoding of our messages to tell the tokenizer what he needs to handle
     */
	private final Encoder encoder;
	private final StringTokenizer tokenizer;
    private PrintWriter out;
    private AtomicInteger connectionCount;
    Socket clientSocket;
    ServerProtocol<StringMessage> protocol;
    /**
     * the callback representing the client (player).
     */
    ProtocolCallbackImpl callback;
    
    public ConnectionHandler(Socket acceptedSocket, Encoder e, StringTokenizer t, ServerProtocol<StringMessage> p, AtomicInteger connectionCount)
    {
        out = null;
        clientSocket = acceptedSocket;
		encoder = e;
		tokenizer =t;
        protocol = p;
        callback = null;
        this.connectionCount=connectionCount;
        System.out.println("Accepted connection from client!");
        System.out.println("The client is from: " + acceptedSocket.getInetAddress() + ":" + acceptedSocket.getPort());
        System.out.println("Number of clients connected to the server: "+connectionCount.incrementAndGet());
        System.out.println();
    }
    
    public void run()
    {
        
        try {
            callback = initialize();
        }
        catch (IOException e) {
            System.out.println("Error in initializing I/O");
        }
 
        try {
            process();
        } 
        catch (IOException e) {
            System.out.println("Error in I/O");
        } 
        
        System.out.println(clientSocket.getInetAddress() + ":" + clientSocket.getPort()+" - Connection closed - bye bye...");
        System.out.println("Number of clients connected to the server: "+connectionCount.decrementAndGet());
        System.out.println();
        close();
 
    }
    
    public void process() throws IOException
    {
    	String msg;
		
		while (!protocol.shouldClose() && !clientSocket.isClosed()){
    		try{
    			if (!tokenizer.isAlive())
    				protocol.connectionTerminated();
    			else{
    				msg = tokenizer.nextToken();
    				protocol.processMessage(new StringMessage(msg), callback);
    			}
    		} catch (IOException e){
    			protocol.connectionTerminated();
    			break;
    		}
    	}
    }
    
    // Starts listening
    public ProtocolCallbackImpl initialize() throws IOException
    {
       // Initialize I/O
        out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(),encoder.getCharset()), true);
        callback = new ProtocolCallbackImpl(out);
		
        return callback;
    }
    
    // Closes the connection
    public void close()
    {
        try {
            if (out != null)
            {
                out.close();
            }
            
            clientSocket.close();
        }
        catch (IOException e)
        {
            System.out.println("Exception in closing I/O");
        }
    }
    
}
 
class MultipleClientProtocolServer implements Runnable {
    private ServerSocket serverSocket;
    private int listenPort;
    private ServerProtocolFactory<StringMessage> factory;
    private AtomicInteger connectionCount;
    
    
    public MultipleClientProtocolServer(int port, ServerProtocolFactory<StringMessage> p, AtomicInteger connectionCount)
    {
        serverSocket = null;
        listenPort = port;
        factory = p;
        this.connectionCount=connectionCount;
    }
    
    public void run()
    {
		Socket socket;
		Encoder encoder = new EncoderImpl("UTF-8");
		
        try {
			serverSocket = new ServerSocket(listenPort);
        	System.out.println("Server IP address is " + InetAddress.getLocalHost().getHostAddress() + "\n" +
								"Server is ready. Listening on port " + listenPort + "\n" +
								"Number of clients connected to the server: " + connectionCount.get() + "\n");
        }
        catch (IOException e) {
            System.out.println("Cannot listen to selected port. Please reconnect to available port in the range 1-65535");
        }
        catch(IllegalArgumentException e){
        	System.out.println("Cannot listen to selected port. Please reconnect to available port in the range 1-65535");
        }
        
        while (true)
        {
            try {
            	socket = serverSocket.accept();
		        StringTokenizer tokenizer = new StringTokenizer(new InputStreamReader(socket.getInputStream(),encoder.getCharset()),'\n');
				ConnectionHandler newConnection = new ConnectionHandler(socket,encoder,tokenizer, factory.create(), connectionCount);
				new Thread(newConnection).start();
            }
            catch(SocketException e){
				break;
            }
            catch (IOException e){
                System.out.println("Failed to accept on port " + listenPort);
            }
            catch (NullPointerException e){
				break;
            }
        }
    }
    
 
    // Closes the connection
    public void close() throws IOException
    {
		if (serverSocket!=null)
		  serverSocket.close();
    }
    
    public static void main(String[] args) throws IOException
    {	
        // Get port
        int port = Integer.decode(args[0]).intValue();
        AtomicInteger connectionCount= new AtomicInteger(0);
        MultipleClientProtocolServer server = new MultipleClientProtocolServer(port, new TBGPFactory(), connectionCount);
        Thread serverThread = new Thread(server);
        Thread inputThread=new Thread(new InputThread(server, connectionCount));
		
        serverThread.start();
        inputThread.start();
        try {
            serverThread.join();
        }
        catch (InterruptedException e)
        {
            System.out.println("Server stopped");
        }          
    }
}
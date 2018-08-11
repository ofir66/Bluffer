package bgu.spl.multipleClientServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

public class InputThread implements Runnable {

	private MultipleClientProtocolServer fServer;
	private AtomicInteger connectionCount;
	
	public InputThread(MultipleClientProtocolServer server, AtomicInteger connectionCount){
		this.fServer=server;
		this.connectionCount=connectionCount;
	}
	
	@Override
	public void run() {
		BufferedReader buffer=new BufferedReader(new InputStreamReader(System.in));
		String line=null;
		
		while(true){
			try{ 
				line=buffer.readLine();
			}
			catch (IOException e){
				e.printStackTrace();
				break;
			}
			if (line!=null && line.equals("QUIT")){
				if (this.connectionCount.get()==0){
					try{
					  this.fServer.close();   
					}
					catch (IOException e) { e.printStackTrace();}
					break;	
				}
				else{ // cannot disconnect if clients are connected
					System.out.println("Cannot disconnect server- there are clients that are still connected");
				}
			}
		}
	}
}
package bgu.spl.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class KeyboardReadingThread implements Runnable{
	
	private Socket clientSocket;
	private BufferedReader userIn;
	
	public KeyboardReadingThread(Socket clientSocket, BufferedReader userIn){
		this.clientSocket=clientSocket;
		this.userIn=userIn;
	}
	
	@Override
	public void run() {
		PrintWriter out = null;
		String msg;
		try {
			out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
		} catch (IOException e1) {
			System.out.println("Couldn't get output to server connection");
			System.exit(1);
		}
		while(true){
			try {
				msg = userIn.readLine();
			} catch (IOException e) {
				break;
			}
			if (msg==null) { break; }
			out.println(msg);
			synchronized(userIn){
				try {
					userIn.wait();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		out.close();
	}
}

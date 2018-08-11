package bgu.spl.protocol;

import java.io.IOException;
import java.io.PrintWriter;

import bgu.spl.protocol.ProtocolCallback;
import bgu.spl.tokenizer.StringMessage;

public class ProtocolCallbackImpl implements ProtocolCallback<StringMessage> {

	private PrintWriter printer;
	
	public ProtocolCallbackImpl(PrintWriter printer) {
		this.printer = printer;
	}
	
	@Override
	public void sendMessage(StringMessage msg) throws IOException {
		printer.println(msg);
		
	}

	public void close(){
		printer.close();
	}
}

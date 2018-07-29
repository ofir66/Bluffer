package protocol;

import tokenizer.StringMessage;

public class TBGPFactory implements ServerProtocolFactory<StringMessage> {

	@Override
	public ServerProtocol<StringMessage> create() {
		return new TBGP();
	}
	
}

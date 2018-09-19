package bgu.spl.protocol;

import bgu.spl.tokenizer.StringMessage;

public class TBGPFactory implements ServerProtocolFactory<StringMessage> {

  @Override
  public ServerProtocol<StringMessage> create() {
    return new TBGP();
  }
	
}

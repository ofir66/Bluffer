package bgu.spl.protocol;

public interface ServerProtocolFactory<T> {
	ServerProtocol<T> create();
}

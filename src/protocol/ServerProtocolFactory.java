package protocol;

public interface ServerProtocolFactory<T> {
	ServerProtocol<T> create();
}

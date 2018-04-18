package s3.protocol.identify;

public class ProtocolHTTP implements IProtocol {
	String host = "";

	public ProtocolHTTP(String host) {
		this.host = host;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "HTTP";
	}

	@Override
	public String getHost() {
		// TODO Auto-generated method stub
		return host;
	}

}

package s3.protocol.identify;

import libcore.tlswire.handshake.ClientHello;
import libcore.tlswire.handshake.HelloExtension;
import libcore.tlswire.handshake.ServerNameHelloExtension;
import libcore.tlswire.util.TlsProtocolVersion;

public class ProtocolTLS implements IProtocol {
	String name;
	String host = "";
	TlsProtocolVersion tlsv;

	public ProtocolTLS(ClientHello result, TlsProtocolVersion tlsv) {
		this.tlsv = tlsv;
		name = "HTTPS/" + tlsv;
		for (HelloExtension ex : result.extensions) {
			if (ex instanceof ServerNameHelloExtension) {
				host = ((ServerNameHelloExtension) ex).getHosts();
			}
		}

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public String getHost() {
		// TODO Auto-generated method stub
		return host;
	}

}

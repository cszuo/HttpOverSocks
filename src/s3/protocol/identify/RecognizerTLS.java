package s3.protocol.identify;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import libcore.tlswire.handshake.ClientHello;
import libcore.tlswire.util.IoUtils;
import libcore.tlswire.util.TlsProtocolVersion;

public class RecognizerTLS implements IRecognizer {

	public IProtocol tryIt(byte[] buffer) {
		// try tls
		try {
			if (buffer.length >= 5) {
				DataInputStream datain = new DataInputStream(new ByteArrayInputStream(buffer));
				if (datain.readByte() == 22) {
					TlsProtocolVersion tlsv = TlsProtocolVersion.read(datain);
					int len = datain.readUnsignedShort();
					if (datain.available() >= len) {
						int type = datain.readUnsignedByte();
						if (type == 1) {// Client Hello
							ClientHello result = new ClientHello();
							result.type = type;
							int bodyLength = IoUtils.readUnsignedInt24(datain);
							if (datain.available() >= bodyLength) {
								result.parseBody(datain);
								// System.out.println(result);
								return new ProtocolTLS(result, tlsv);
							} else {
								System.out.println("Need more data!");
							}
						} else {
							System.out.println("Unknown Handshake Protocol Type " + type);
						}
					} else {
						System.out.println("Need more data!");
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}

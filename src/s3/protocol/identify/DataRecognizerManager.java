package s3.protocol.identify;

import java.util.HashSet;

public class DataRecognizerManager {

	static HashSet<IRecognizer> rcs = new HashSet<IRecognizer>();

	static {
		rcs.add(new RecognizerTLS());
		rcs.add(new RecognizerHTTP());
	}

	public static IProtocol tryIt(byte[] buffer) {
		// try tls
		IProtocol res = null;
		for (IRecognizer rc : rcs) {
			try {
				res = rc.tryIt(buffer);
				if (res != null)
					break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return res;
	}
}

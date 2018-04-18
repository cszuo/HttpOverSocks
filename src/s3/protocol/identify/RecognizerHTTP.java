package s3.protocol.identify;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class RecognizerHTTP implements IRecognizer {
	static String reg;
	static {
		String opt[] = { "GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE", "CONNECT" };
		reg = "(" + StringUtils.join(opt, "|") + ")" + "\\s+([^?\\s]+)((?:[?&][^&\\s]+)*)\\s+(HTTP/.*)";
	}

	public IProtocol tryIt(byte[] buffer) {
		String str = new String(buffer);
		if (str.contains("\n")) {
			String[] strs = str.split("\n");
			String reqLine = strs[0].trim();
			if (Pattern.matches(reg, reqLine)) {
				String host = "";
				for (int i = 1; i < strs.length; i++) {
					if (strs[i].trim().toLowerCase().startsWith("host") && strs[i].contains(":")) {
						host = strs[i].split(":")[1].trim();
					}
				}
				return new ProtocolHTTP(host);
			}
		}
		return null;
	}

	public static void main(String[] args) {
		String content = "GET / HTTP/1.1";
		System.out.println(Pattern.matches(reg, content));
		System.out.println(String.format("sdf%s","1"));
	}
}

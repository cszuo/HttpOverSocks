package s3.configure;

import java.util.regex.Pattern;

import org.json.JSONObject;

public class Configure {
	Class<?> CLS = FileConfig.class;
	static Configure conf = new Configure();

	public static Configure getInstance() {
		return conf;
	}

	JSONObject js;
	String proxyIp;
	int proxyPort;

	private Configure() {
	}

	public void print(Class<?> clas, String str) {
		System.out.printf("%s %s\n", clas, str);
	}

	public void setCofigJS(JSONObject js) {
		this.js = js;
		JSONObject tmp;

		String str[] = js.getString("proxy").split(":");
		proxyIp = str[0];
		proxyPort = Integer.parseInt(str[1]);
		print(CLS, proxyIp + ":" + proxyPort);

		for (int i = 0; i < js.getJSONArray("redirects").length(); i++) {
			tmp = js.getJSONArray("redirects").getJSONObject(i);

			for (int j = 0; j < tmp.getJSONArray("ports").length(); j++) {
				print(CLS, "  " + tmp.getString("host") + ":" + tmp.getJSONArray("ports").getString(j));
			}
		}
	}

	public boolean isTarget(String host, int port) {

		JSONObject tmp;
		for (int i = 0; i < js.getJSONArray("redirects").length(); i++) {
			tmp = js.getJSONArray("redirects").getJSONObject(i);
			if (Pattern.matches(tmp.getString("host"), host)) {
				for (int j = 0; j < tmp.getJSONArray("ports").length(); j++) {
					if (Pattern.matches(tmp.getJSONArray("ports").getString(j), port + ""))
						return true;
				}
			}
		}
		return false;
	}

	public String getProxyIp() {
		return proxyIp;
	}

	public int getProxyPort() {
		return proxyPort;
	}

}

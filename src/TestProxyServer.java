import java.io.IOException;
import java.net.URISyntaxException;

import fucksocks.server.SocksProxyServer;
import fucksocks.server.SocksProxyServerFactory;
import s3.configure.FileConfig;

public class TestProxyServer {
	public static void main(String args[]) throws IOException, URISyntaxException {

		// System.out.println(Pattern.matches(".*.com", "wefawef.com"));

		FileConfig.load();

		SocksProxyServer proxyServer = SocksProxyServerFactory.newNoAuthenticaionServer();
		proxyServer.start();// Creat a SOCKS5 server bind at port 1080
 
	}

}

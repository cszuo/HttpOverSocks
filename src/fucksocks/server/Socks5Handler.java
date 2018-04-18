/*
 * Copyright 2015-2025 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package fucksocks.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fucksocks.common.NotImplementException;
import fucksocks.common.ProtocolErrorException;
import fucksocks.common.SocksException;
import fucksocks.common.methods.SocksMethod;
import fucksocks.server.filters.FilterChain;
import fucksocks.server.filters.SocksListener;
import fucksocks.server.io.Pipe;
import fucksocks.server.io.SocketPipe;
import fucksocks.server.msg.CommandMessage;
import fucksocks.server.msg.CommandResponseMessage;
import fucksocks.server.msg.MethodSelectionMessage;
import fucksocks.server.msg.MethodSeleteionResponseMessage;
import fucksocks.server.msg.ServerReply;
import s3.configure.Configure;
import s3.protocol.identify.IProtocol;
import s3.protocol.identify.IProtocolRecognizeReceiver;
import s3.protocol.identify.ProtocolHTTP;
import s3.protocol.identify.ProtocolTLS;

/**
 * The class <code>Socks5Handler</code> represents a handler that can handle
 * SOCKS5 protocol.
 *
 * @author Youchao Feng
 * @date Apr 16, 2015 11:03:49 AM
 * @version 1.0
 *
 */
public class Socks5Handler implements SocksHandler, IProtocolRecognizeReceiver {

	/**
	 * Logger
	 */
	protected static final Logger logger = LoggerFactory.getLogger(Socks5Handler.class);
	Class<?> CLS = Socks5Handler.class;

	/**
	 * Protocol version.
	 */
	private static final int VERSION = 0x5;

	/**
	 * Session.
	 */
	private Session session;

	/**
	 * Method selector.
	 */
	private MethodSelector methodSelector;

	private FilterChain filterChain;

	private int bufferSize;

	private int idleTime = 1000;

	private List<SocksListener> socksListeners;
	private boolean needRedirect = false;

	@Override
	public void handle(Session session) throws SocksException, IOException {

		sessionCreated(session);

		MethodSelectionMessage msg = new MethodSelectionMessage();
		session.read(msg);

		if (msg.getVersion() != VERSION) {
			throw new ProtocolErrorException("Protocol! error");
		}
		SocksMethod selectedMethod = methodSelector.select(msg);

		Configure.getInstance().print(CLS,
				String.format("[%s]SOKCS5 Server seleted:%s", session, selectedMethod.getMethodName()));
		// send select method.
		session.write(new MethodSeleteionResponseMessage(VERSION, selectedMethod));

		// do method.
		selectedMethod.doMethod(session);

		CommandMessage commandMessage = new CommandMessage();

		try {
			session.read(commandMessage); // Read command request.

			Configure.getInstance().print(CLS, String.format("Session[%s] send Rquest:%s  %s:%s", session.getId(),
					commandMessage.getCommand(), commandMessage.getInetAddress(), commandMessage.getPort()));

		} catch (SocksException e) {
			session.write(new CommandResponseMessage(e.getServerReply()));
			Configure.getInstance().print(CLS, e.getMessage());
			e.printStackTrace();
			return;
		}

		commandReceived(session, commandMessage);

		/****************************
		 * DO COMMAND
		 ******************************************/

		switch (commandMessage.getCommand()) {

		case BIND:
			doBind(session, commandMessage);
			break;
		case CONNECT:
			doConnect(session, commandMessage);
			break;
		case UDP_ASSOCIATE:
			doUDPAssociate(session, commandMessage);
			break;
		default:
			throw new NotImplementException("Not support command");

		}

	}

	SocketPipe spip;

	@Override
	public void doConnect(Session session, CommandMessage commandMessage) throws SocksException, IOException {

		ServerReply reply = null;
		Socket socket = null;
		InetAddress bindAddress = null;
		int bindPort = 0;

		// set default bind address.
		byte[] defaultAddress = { 0, 0, 0, 0 };
		bindAddress = InetAddress.getByAddress(defaultAddress);
		// DO connect
		try {
			rport = commandMessage.getPort();
			socket = new Socket(commandMessage.getInetAddress(), commandMessage.getPort());
			bindAddress = socket.getLocalAddress();
			bindPort = socket.getLocalPort();
			reply = ServerReply.SUCCESSED;

		} catch (Exception e) {
			if (e.getMessage().equals("Connection refused")) {
				reply = ServerReply.CONNECTION_REFUSED;
			} else if (e.getMessage().equals("Operation timed out")) {
				reply = ServerReply.TTL_EXPIRED;
			} else if (e.getMessage().equals("Network is unreachable")) {
				reply = ServerReply.NETWORK_UNREACHABLE;
			}
			Configure.getInstance().print(CLS, "connect exception:" + e);
		}

		session.write(new CommandResponseMessage(VERSION, reply, bindAddress, bindPort));

		if (reply != ServerReply.SUCCESSED) { // 如果返回失败信息，则退出该方法。
			session.close();
			return;
		}

		spip = new SocketPipe(session.getSocket(), socket, this, null);
		spip.setBufferSize(bufferSize);
		spip.start(); // This method will create tow thread to run tow internal
						// pipes.

		// wait for pipe exit.
		while (spip.isRunning() && !needRedirect) {
			try {
				Thread.sleep(idleTime);
			} catch (InterruptedException e) {
				spip.stop();
				session.close();
				Configure.getInstance().print(CLS, String.format("Session[%s] closed", session.getId()));
			}
		}

		if (needRedirect) {
			doRedirect(session);
		}

	}

	private void doRedirect(Session session) throws IOException {

		String ip = Configure.getInstance().getProxyIp();
		int port = Configure.getInstance().getProxyPort();
		Socket socket = null;
		Configure.getInstance().print(CLS, String.format("Session[%s] redirect to proxy", session.getId()));
		try {
			socket = new Socket(ip, port);

			Configure.getInstance().print(CLS, "connect(doRedirect) status:" + socket.isConnected());
		} catch (IOException e) {
			Configure.getInstance().print(CLS, String.format("connect(doRedirect) exception:%s", e));
			return;
		}

		spip = new SocketPipe(session.getSocket(), socket, null, firstPkt);
		spip.setBufferSize(bufferSize);
		spip.start();
		firstPkt = null;

		// wait for pipe exit.
		while (spip.isRunning()) {
			try {
				Thread.sleep(idleTime);
			} catch (InterruptedException e) {
				spip.stop();
				session.close();
				Configure.getInstance().print(CLS, String.format("Session[%s] closed", session.getId()));
			}
		}

	}

	@Override
	public void doBind(Session session, CommandMessage commandMessage) throws SocksException, IOException {

		ServerSocket serverSocket = new ServerSocket(commandMessage.getPort());
		int bindPort = serverSocket.getLocalPort();
		Socket socket = null;
		logger.info("Create TCP server bind at {} for session[{}]", serverSocket.getLocalSocketAddress(),
				session.getId());
		session.write(
				new CommandResponseMessage(VERSION, ServerReply.SUCCESSED, serverSocket.getInetAddress(), bindPort));

		socket = serverSocket.accept();
		session.write(new CommandResponseMessage(VERSION, ServerReply.SUCCESSED, socket.getLocalAddress(),
				socket.getLocalPort()));

		Pipe pipe = new SocketPipe(session.getSocket(), socket, this, null);
		pipe.setBufferSize(bufferSize);
		pipe.start();

		// wait for pipe exit.
		while (pipe.isRunning()) {
			try {
				Thread.sleep(idleTime);
			} catch (InterruptedException e) {
				pipe.stop();
				session.close();
				logger.info("Session[{}] closed", session.getId());
			}
		}
		serverSocket.close();
		// throw new NotImplementException("Not implement BIND command");
	}

	@Override
	public void doUDPAssociate(Session session, CommandMessage commandMessage) throws SocksException, IOException {
		UDPRelayServer udpRelayServer = new UDPRelayServer(
				((InetSocketAddress) session.getRemoteAddress()).getAddress(), commandMessage.getPort());
		InetSocketAddress socketAddress = (InetSocketAddress) udpRelayServer.start();
		logger.info("Create UDP relay server at[{}] for {}", socketAddress, commandMessage.getSocketAddress());
		session.write(new CommandResponseMessage(VERSION, ServerReply.SUCCESSED, InetAddress.getLocalHost(),
				socketAddress.getPort()));
		while (udpRelayServer.isRunning()) {
			try {
				Thread.sleep(idleTime);
			} catch (InterruptedException e) {
				session.close();
				logger.info("Session[{}] closed", session.getId());
			}
			if (session.isClose()) {
				udpRelayServer.stop();
				logger.debug("UDP relay server for session[{}] is closed", session.getId());
			}

		}

	}

	protected void sessionCreated(Session session) {
		if (socksListeners == null) {
			return;
		}
		for (int i = 0; i < socksListeners.size(); i++) {
			socksListeners.get(i).onSessionCreated(session);
		}
	}

	protected void commandReceived(Session session, CommandMessage message) {
		if (socksListeners == null) {
			return;
		}
		for (int i = 0; i < socksListeners.size(); i++) {
			socksListeners.get(i).onCommandReceived(session, message);
			;
		}
	}

	@Override
	public void setSession(Session session) {
		this.session = session;
	}

	@Override
	public void run() {
		try {
			handle(session);
		} catch (Exception e) {
			Configure.getInstance().print(CLS, String.format("Session[%s]:%s", session.getId(), e.getMessage()));
		} finally {
			/*
			 * At last, close the session.
			 */
			session.close();
			Configure.getInstance().print(CLS, String.format("Session[%s] closed", session.getId()));
		}
	}

	@Override
	public FilterChain getFilterChain() {
		return filterChain;
	}

	@Override
	public void setFilterChain(FilterChain filterChain) {
		this.filterChain = filterChain;
	}

	@Override
	public MethodSelector getMethodSelector() {
		return methodSelector;
	}

	@Override
	public void setMethodSelector(MethodSelector methodSelector) {
		this.methodSelector = methodSelector;
	}

	@Override
	public int getBufferSize() {
		return bufferSize;
	}

	@Override
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public List<SocksListener> getSocksListeners() {
		return socksListeners;
	}

	@Override
	public void setSocksListeners(List<SocksListener> socksListeners) {
		this.socksListeners = socksListeners;
	}

	@Override
	public int getIdleTime() {
		return idleTime;
	}

	@Override
	public void setIdleTime(int idleTime) {
		this.idleTime = idleTime;
	}

	byte[] firstPkt;
	int rport;

	@Override
	public void recognized(IProtocol p) {
		// TODO Auto-generated method stub
		if (p instanceof ProtocolTLS || p instanceof ProtocolHTTP) {
			if (Configure.getInstance().isTarget(p.getHost(), rport)) {
				Configure.getInstance().print(CLS, String.format("Target(%s:%s)", p.getHost(), rport));
				firstPkt = spip.closeAndKeepSrcAlive();
				needRedirect = true;
			} else {
				Configure.getInstance().print(CLS, String.format("Not Target(%s:%s)", p.getHost(), rport));
			}
		}
	}

}

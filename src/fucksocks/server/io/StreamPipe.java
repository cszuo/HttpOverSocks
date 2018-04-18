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

package fucksocks.server.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import s3.configure.Configure;
import s3.protocol.identify.DataRecognizerManager;
import s3.protocol.identify.IProtocol;
import s3.protocol.identify.IProtocolRecognizeReceiver;

/**
 * The class <code>StreamPipe</code> represents a pipe the can transfer data
 * from a input stream to a output stream.
 * 
 * @author Youchao Feng
 * @date Apr 6, 2015 11:37:16 PM
 * @version 1.0
 *
 */
public class StreamPipe implements Runnable, Pipe {

	Class<?> CLS = StreamPipe.class;
	/**
	 * Default buffer size.
	 */
	private static final int BUFFER_SIZE = 1024 * 1024 * 5;

	/**
	 * Listeners
	 */
	private List<PipeListener> pipeListeners;

	/**
	 * Input stream.
	 */
	private InputStream from;

	/**
	 * Output stream.
	 */
	private OutputStream to;

	/**
	 * Buffer size.
	 */
	private int bufferSize = BUFFER_SIZE;

	/**
	 * Running thread.
	 */
	private Thread runningThread;

	/**
	 * A flag.
	 */
	private boolean running = false;

	/**
	 * Name of the pipe.
	 */
	private String name;

	private byte[] buf_of_first = new byte[BUFFER_SIZE];
	private int buf_of_first_len = 0;
	private boolean from_client = false;
	private IProtocol recon_res = null;
	IProtocolRecognizeReceiver rr = null;

	/**
	 * Constructs a Pipe instance with a input stream and a output stream.
	 * 
	 * @param from
	 *            stream where it comes from.
	 * @param to
	 *            stream where it will be transfered to.
	 */
	public StreamPipe(InputStream from, OutputStream to, IProtocolRecognizeReceiver rr) {
		this.from = from;
		this.to = to;
		this.rr = rr;
		pipeListeners = new ArrayList<>();
	}

	public StreamPipe(InputStream from, OutputStream to) {
		this.from = from;
		this.to = to;
		pipeListeners = new ArrayList<>();
	}

	public StreamPipe(InputStream from, OutputStream to, String name) {
		this.from = from;
		this.to = to;
		pipeListeners = new ArrayList<>();
		this.name = name;
	}

	byte[] data2Send = null;

	public void setData2Send(byte[] data2Send) {
		this.data2Send = data2Send;
	}

	@Override
	public boolean start() {
		if (!running) { // If the pipe is not running, run it.
			running = true;
			runningThread = new Thread(this);
			runningThread.start();
			for (PipeListener listener : pipeListeners) {
				listener.onStarted(this);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean stop() {
		if (running) { // if the pipe is working, stop it.
			running = false;
			if (runningThread != null) {
				runningThread.interrupt();
			}
			for (int i = 0; i < pipeListeners.size(); i++) {
				PipeListener listener = pipeListeners.get(i);
				listener.onStoped(this);
			}
			return true;
		}
		return false;
	}

	@Override
	public void run() {
		byte[] buffer = new byte[bufferSize];

		while (running) {
			int size = doTransfer(buffer);
			if (size == -1) {
				stop();
			}
		}
	}

	/**
	 * transfer a buffer.
	 * 
	 * @param buffer
	 *            Buffer that transfer once.
	 * @return number of byte that transfered.
	 */
	protected int doTransfer(byte[] buffer) {

		int length = -1;
		try {
			if (data2Send != null) {
				System.arraycopy(data2Send, 0, buffer, 0, data2Send.length);
				length = data2Send.length;
				data2Send = null;
			} else {
				length = from.read(buffer);
			}
			if (length > 0) { // transfer the buffer to output stream.

				if (from_client && recon_res == null && buf_of_first_len < buf_of_first.length) {
					int tocopy = length > buf_of_first.length - buf_of_first_len
							? buf_of_first.length - buf_of_first_len : length;
					System.arraycopy(buffer, 0, buf_of_first, buf_of_first_len, tocopy);
					buf_of_first_len += tocopy;
					recon_res = DataRecognizerManager.tryIt(buf_of_first);
					if (recon_res != null) {
						if (rr != null)
							rr.recognized(recon_res);
						System.out.println(recon_res.getName() + ":" + recon_res.getHost());
					}
				}
				if (running) {
					to.write(buffer, 0, length);
					to.flush();
					for (PipeListener listener : pipeListeners) {
						listener.onTransfered(this, buffer, length);
					}
				}
			}

		} catch (IOException e) {
			if (e.getMessage().equals("Socket closed")) {
				Configure.getInstance().print(CLS,
						String.format("Socket is closed, the pipe[%s] going to close", name));
			} else {
				Configure.getInstance().print(CLS, String.format("Unknow excepition:%s", e.getMessage()));
			}
			stop();
		}

		return length;
	}

	@Override
	public boolean close() {
		stop();

		try {
			from.close();
			to.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
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
	public boolean isRunning() {
		return running;
	}

	@Override
	public void addPipeListener(PipeListener pipeListener) {
		pipeListeners.add(pipeListener);
	}

	@Override
	public void removePipeListener(PipeListener pipeListener) {
		pipeListeners.remove(pipeListener);
	}

	public List<PipeListener> getPipeListeners() {
		return pipeListeners;
	}

	public void setPipeListeners(List<PipeListener> pipeListeners) {
		this.pipeListeners = pipeListeners;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		if (name != null && name.equals("OUTPUT_PIPE")) {
			from_client = true;
		}
	}

	public byte[] getFirstPkt() {
		byte[] buf = new byte[buf_of_first_len];
		System.arraycopy(buf_of_first, 0, buf, 0, buf_of_first_len);
		return buf;
	}

}

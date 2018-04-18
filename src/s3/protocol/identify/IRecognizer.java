package s3.protocol.identify;

public interface IRecognizer {
	public IProtocol tryIt(byte[] buffer);
}

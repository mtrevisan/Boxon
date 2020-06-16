package unit731.boxon.annotations.checksummers;


public interface Checksummer<T>{

	T calculateCRC(final byte[] data, final int start, final int end);

}

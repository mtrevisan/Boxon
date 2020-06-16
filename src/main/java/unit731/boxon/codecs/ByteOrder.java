package unit731.boxon.codecs;


public enum ByteOrder{

	LITTLE_ENDIAN,
	BIG_ENDIAN;


	@SuppressWarnings("unused")
	public static final ByteOrder NATIVE_BYTE_ORDER = (java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.BIG_ENDIAN?
		BIG_ENDIAN: LITTLE_ENDIAN);

}

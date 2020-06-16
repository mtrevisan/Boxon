package unit731.boxon.codecs.queclink;

import unit731.boxon.annotations.BindByte;


public class Version{
	@BindByte
	public byte major;
	@BindByte
	public byte minor;
	public byte build;

}

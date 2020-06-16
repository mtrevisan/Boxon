package unit731.boxon.codecs.queclink;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import unit731.boxon.annotations.Assign;
import unit731.boxon.annotations.BindStringTerminated;
import unit731.boxon.annotations.MessageHeader;
import unit731.boxon.annotations.transformers.Transformer;
import unit731.boxon.annotations.validators.IMEIValidator;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;


@MessageHeader(start = "+ACK:", end = "$")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ASCII_ACKMessage{

	private static final Map<Byte, String> MESSAGE_TYPE_MAP = new HashMap<>();
	static{
		MESSAGE_TYPE_MAP.put((byte)0, "AT+GTBSI");
		MESSAGE_TYPE_MAP.put((byte)1, "AT+GTSRI");
		MESSAGE_TYPE_MAP.put((byte)2, "AT+GTQSS");
		MESSAGE_TYPE_MAP.put((byte)4, "AT+GTCFG");
		MESSAGE_TYPE_MAP.put((byte)5, "AT+GTTOW");
		MESSAGE_TYPE_MAP.put((byte)6, "AT+GTEPS");
		MESSAGE_TYPE_MAP.put((byte)7, "AT+GTDIS");
		MESSAGE_TYPE_MAP.put((byte)8, "AT+GTOUT");
		MESSAGE_TYPE_MAP.put((byte)9, "AT+GTIOB");
		MESSAGE_TYPE_MAP.put((byte)10, "AT+GTTMA");
		MESSAGE_TYPE_MAP.put((byte)11, "AT+GTFRI");
		MESSAGE_TYPE_MAP.put((byte)12, "AT+GTGEO");
		MESSAGE_TYPE_MAP.put((byte)13, "AT+GTSPD");
		MESSAGE_TYPE_MAP.put((byte)14, "AT+GTSOS");
		MESSAGE_TYPE_MAP.put((byte)15, "AT+GTMON");
		MESSAGE_TYPE_MAP.put((byte)16, "AT+GTRTO");
		MESSAGE_TYPE_MAP.put((byte)21, "AT+GTUPD");
		MESSAGE_TYPE_MAP.put((byte)22, "AT+GTPIN");
		MESSAGE_TYPE_MAP.put((byte)23, "AT+GTDAT");
		MESSAGE_TYPE_MAP.put((byte)24, "AT+GTOWH");
		MESSAGE_TYPE_MAP.put((byte)25, "AT+GTDOG");
		MESSAGE_TYPE_MAP.put((byte)26, "AT+GTAIS");
		MESSAGE_TYPE_MAP.put((byte)27, "AT+GTJDC");
		MESSAGE_TYPE_MAP.put((byte)28, "AT+GTIDL");
		MESSAGE_TYPE_MAP.put((byte)29, "AT+GTHBM");
		MESSAGE_TYPE_MAP.put((byte)30, "AT+GTHMC");
		MESSAGE_TYPE_MAP.put((byte)32, "AT+GTURT");
		MESSAGE_TYPE_MAP.put((byte)34, "AT+GTWLT");
		MESSAGE_TYPE_MAP.put((byte)35, "AT+GTHRM");
		MESSAGE_TYPE_MAP.put((byte)36, "AT+GTFFC");
		MESSAGE_TYPE_MAP.put((byte)37, "AT+GTJBS");
		MESSAGE_TYPE_MAP.put((byte)38, "AT+GTSSR");
		MESSAGE_TYPE_MAP.put((byte)41, "AT+GTEFS");
		MESSAGE_TYPE_MAP.put((byte)43, "AT+GTIDA");
		MESSAGE_TYPE_MAP.put((byte)44, "AT+GTACD");
		MESSAGE_TYPE_MAP.put((byte)45, "AT+GTPDS");
		MESSAGE_TYPE_MAP.put((byte)46, "AT+GTCRA");
		MESSAGE_TYPE_MAP.put((byte)47, "AT+GTBZA");
		MESSAGE_TYPE_MAP.put((byte)48, "AT+GTSPA");
		MESSAGE_TYPE_MAP.put((byte)53, "AT+GTRMD");
		MESSAGE_TYPE_MAP.put((byte)57, "AT+GTPGD");
		MESSAGE_TYPE_MAP.put((byte)62, "AT+GTSSI");
		MESSAGE_TYPE_MAP.put((byte)63, "AT+GTASC");
		MESSAGE_TYPE_MAP.put((byte)64, "AT+GTTRF");
	}

	public static class MessageTypeTransformer implements Transformer<Byte, String>{
		@Override
		public String decode(final Byte value){
			return MESSAGE_TYPE_MAP.get(value);
		}

		@Override
		public Byte encode(final String value){
			for(final Map.Entry<Byte, String> elem : MESSAGE_TYPE_MAP.entrySet())
				if(elem.getValue().equals(value))
					return elem.getKey();
			return 0x00;
		}
	}


	@BindStringTerminated(terminator = ':')
	private String messageHeader;
	@BindStringTerminated(terminator = ',')
	private String messageType;
	@BindStringTerminated(terminator = ',')
	public String deviceTypeAndVersion;
	@BindStringTerminated(terminator = ',', validator = IMEIValidator.class)
	private String imei;
	@BindStringTerminated(terminator = ',')
	private String deviceName;
	@BindStringTerminated(terminator = ',')
	private String id;
	@BindStringTerminated(terminator = ',')
	private String ackSerialNumber;
	@BindStringTerminated(terminator = ',', transformer = QueclinkHelper.StringDateTimeYYYYMMDDHHMMSSTransformer.class)
	private ZonedDateTime eventTime;
	@BindStringTerminated(terminator = '$', consumeTerminator = false, transformer = QueclinkHelper.HexStringToIntTransformer.class)
	private int messageId;

	@Assign("T(Integer).valueOf(deviceTypeAndVersion.substring(0, 2), 16).byteValue()")
	private byte deviceTypeCode;
	@Assign("deviceTypeAndVersion.substring(2, 6)")
	private String deviceVersion;
	@Assign("#deviceTypes.getDeviceTypeName(deviceTypeCode)")
	private String deviceTypeName;
	@Assign("T(java.time.ZonedDateTime).now()")
	@JsonDeserialize(using = ZonedDateTimeDeserializer.class)
	private ZonedDateTime receptionTime;
	@Assign("messageHeader.startsWith('+B')")
	private boolean buffered;

}

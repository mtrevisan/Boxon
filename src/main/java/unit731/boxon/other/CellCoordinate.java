package unit731.boxon.other;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


@JsonPropertyOrder({"mcc", "mnc", "lac", "cid", "rssi", "umtsCell"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class CellCoordinate{

	public static final int RSSI_LEVEL_NO_COVERAGE = 0;


	//International Mobile Subscriber Identity = MCC + MNC + MSIN (Mobile Station Identification Number)
	//Cell Global Identifier = MCC + MNC + LAC + CID
	//Location Area Identity = MCC + MNC + LAC
	/** Mobile Country Code [12 bits (dec)]. */
	private int mcc;
	/** Mobile Network Code [12 bits (dec)]. */
	private int mnc;
	//Routing Area Identity = LAC + RAC (Routing Area Code [8 bits])
	/** Location Area Code [16 bits]. */
	private int lac;
	/** Cell ID [28 bits]. */
	private int cid;

	/**
	 * Radio Signal Strength Indicator [dBm].<br>
	 * Typically between -121 dBm and -40 dBm, or LEVEL_NO_COVERAGE if the radio is out of coverage.
	 */
	private Integer rssi;


	private CellCoordinate(){}

	public static CellCoordinate createFrom(final int mcc, final int mnc, final int lac, final int cid){
		return new CellCoordinate(mcc, mnc, lac, cid, null);
	}

	public static CellCoordinate createFrom(final int mcc, final int mnc, final int lac, final int cid, final int rssi){
		return new CellCoordinate(mcc, mnc, lac, cid, rssi);
	}

	private CellCoordinate(final int mcc, final int mnc, final int lac, final int cid, final Integer rssi){
		this.mcc = mcc;
		this.mnc = mnc;
		this.lac = lac;
		this.cid = cid;
		this.rssi = rssi;

		if(!isValid())
			throw new IllegalArgumentException("Invalid cell coordinates: " + this);
	}

	public final int getMCC(){
		return mcc;
	}

	public final int getMNC(){
		return mnc;
	}

	public final int getLAC(){
		return lac;
	}

	public final int getCID(){
		return cid;
	}

	/**
	 * Retrieves the Radio Signal Strength Indicator.
	 *
	 * @return	The RSSI
	 */
	public final Integer getRSSI(){
		return rssi;
	}

	public final boolean isUmtsCell(){
		//GSM: 4 hex digits, UMTS: 6 hex digits
		return ((cid >>> Short.SIZE) != 0x0000);
	}

	@JsonIgnore
	public boolean isValid(){
		return (mcc >= 0 && mnc >= 0 && lac >= 0 && cid >= 0
			&& (rssi == null || rssi == RSSI_LEVEL_NO_COVERAGE || rssi < 0));
	}

	@Override
	public String toString(){
		return new ToStringBuilder(this, ShortPrefixNotNullToStringStyle.SHORT_PREFIX_NOT_NULL_STYLE)
			.append("mcc", mcc)
			.append("mnc", mnc)
			.append("lac", lac)
			.append("cid", cid)
			.append("rssi", rssi)
			.toString();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder(17, 37)
			.append(mcc)
			.append(mnc)
			.append(lac)
			.append(cid)
			.toHashCode();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == null || obj.getClass() != getClass()){
			return false;
		}
		if(obj == this){
			return true;
		}
		final CellCoordinate rhs = (CellCoordinate)obj;
		return new EqualsBuilder()
			.append(mcc, rhs.mcc)
			.append(mnc, rhs.mnc)
			.append(lac, rhs.lac)
			.append(cid, rhs.cid)
			.isEquals();
	}

}

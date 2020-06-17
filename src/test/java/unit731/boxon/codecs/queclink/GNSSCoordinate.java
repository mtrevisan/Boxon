package unit731.boxon.codecs.queclink;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;


@JsonPropertyOrder({"latitude", "longitude", "altitude", "hdop", "vdop"})
public class GNSSCoordinate{

	private static final BigDecimal TO_RADIANS_FACTOR = new BigDecimal(Math.PI).divide(new BigDecimal(180), MathContext.DECIMAL128);


	/** The latitude [deg]. */
	private BigDecimal latitude;
	/** The longitude [deg]. */
	private BigDecimal longitude;
	/** The altitude [m]. */
	private BigDecimal altitude;

	/** Horizontal Dilution of Precision (GPGGA, GPGSA) */
	private Float hdop;
	/** Vertical Dilution of Precision (GPGSA) */
	private Float vdop;


	public static GNSSCoordinate createFrom(final BigDecimal latitude, final BigDecimal longitude){
		return new GNSSCoordinate(latitude, longitude, null, null, null);
	}

	public static GNSSCoordinate createFrom(final BigDecimal latitude, final BigDecimal longitude, final Float hdop){
		return new GNSSCoordinate(latitude, longitude, null, null, null);
	}

	public static GNSSCoordinate createFrom(final BigDecimal latitude, final BigDecimal longitude, final BigDecimal altitude){
		return new GNSSCoordinate(latitude, longitude, altitude, null, null);
	}

	public static GNSSCoordinate createFrom(final BigDecimal latitude, final BigDecimal longitude, final BigDecimal altitude,
			final Float hdop){
		return new GNSSCoordinate(latitude, longitude, altitude, hdop, null);
	}

	public static GNSSCoordinate createFrom(final BigDecimal latitude, final BigDecimal longitude, final BigDecimal altitude,
			final Float hdop, final Float vdop){
		return new GNSSCoordinate(latitude, longitude, altitude, hdop, vdop);
	}

	private GNSSCoordinate(){}

	private GNSSCoordinate(final BigDecimal latitude, final BigDecimal longitude, final BigDecimal altitude, final Float hdop,
			final Float vdop){
		Objects.requireNonNull(latitude);
		Objects.requireNonNull(longitude);

		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.hdop = hdop;
		this.vdop = vdop;

		if(!isValid())
			throw new IllegalArgumentException("Invalid GNSS coordinates: " + this);
	}

	public BigDecimal getLatitude(){
		return latitude;
	}

	public BigDecimal getLongitude(){
		return longitude;
	}

	public BigDecimal getAltitude(){
		return altitude;
	}

	/**
	 * Returns the horizontal (2D) dilution of precision.
	 * <p>
	 * Returns <code>NaN</code> if cannot be determined yet.<br>
	 * NOTE: Value 1 are ideal, from 2 to 3 are excellent, from 4 to 6 good, 7 to 8 moderate, 9 to 20 fair, 21 to 50 poor.
	 *
	 * @return	The HDOP
	 */
	public Float getHdop(){
		return hdop;
	}

	/**
	 * Returns the vertical dilution of precision.
	 * <p>
	 * Returns <code>NaN</code> if cannot be determined yet.<br>
	 * NOTE: value 1 are ideal, from 2 to 3 are excellent, from 4 to 6 good, 7 to 8 moderate, 9 to 20 fair, 21 to 50 poor.
	 *
	 * @return	The VDOP
	 */
	public Float getVdop(){
		return vdop;
	}

	/**
	 * Returns the position dilution of precision.
	 * <p>
	 * Returns <code>NaN</code> if cannot be determined yet.<br>
	 * NOTE: value 1 are ideal, from 2 to 3 are excellent, from 4 to 6 good, 7 to 8 moderate, 9 to 20 fair, 21 to 50 poor.
	 *
	 * @return	The PDOP
	 */
	@JsonIgnore
	public Float getPdop(){
		return (hdop != null && !Float.isNaN(hdop) && vdop != null && !Float.isNaN(vdop)? (float)Math.sqrt(hdop * hdop + vdop * vdop): null);
	}

	@JsonIgnore
	public boolean isValid(){
		return (Math.abs(latitude.doubleValue()) <= 90. && Math.abs(longitude.doubleValue()) <= 180.
			&& (hdop == null || Float.isNaN(hdop) || hdop > 0.) && (vdop == null || Float.isNaN(vdop) || vdop > 0.));
	}

	@Override
	public String toString(){
		return new ToStringBuilder(this, ShortPrefixNotNullToStringStyle.SHORT_PREFIX_NOT_NULL_STYLE)
			.append("lat", latitude)
			.append("lon", longitude)
			.append("alt", altitude)
			.append("hdop", hdop)
			.append("vdop", vdop)
			.toString();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder(17, 37)
			.append(latitude)
			.append(longitude)
			.append(altitude)
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
		final GNSSCoordinate rhs = (GNSSCoordinate)obj;
		return new EqualsBuilder()
			.append(latitude, rhs.latitude)
			.append(longitude, rhs.longitude)
			.append(altitude, rhs.altitude)
			.isEquals();
	}

	public static double calculateInitialBearing(final GNSSCoordinate initialCoord, final GNSSCoordinate finalCoord){
		final double initialLatitude = toRadians(initialCoord.getLatitude());
		final double finalLatitude = toRadians(finalCoord.getLatitude());
		final double deltaLongitude = toRadians(finalCoord.getLongitude().min(initialCoord.getLongitude()));
		final double y = Math.sin(deltaLongitude) * Math.cos(finalLatitude);
		final double x = Math.cos(initialLatitude) * Math.sin(finalLatitude)
			- Math.sin(initialLatitude) * Math.cos(finalLatitude) * Math.cos(deltaLongitude);
		double initialBearing = Math.atan2(y, x);
		initialBearing = Math.toDegrees(initialBearing);
		return (initialBearing + 360.) % 360.;
	}

	private static double toRadians(final BigDecimal angle){
		return angle.multiply(TO_RADIANS_FACTOR)
			.doubleValue();
	}

	public static double calculateFinalBearing(final GNSSCoordinate initialCoord, final GNSSCoordinate finalCoord){
		final double initialBearing = calculateInitialBearing(finalCoord, initialCoord);
		return (initialBearing + 180.) % 360.;
	}

}

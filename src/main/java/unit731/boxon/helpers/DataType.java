package unit731.boxon.helpers;

import java.util.HashMap;
import java.util.Map;


public enum DataType{

	BYTE(Byte.TYPE, Byte.class),
	SHORT(Short.TYPE, Short.class),
	INTEGER(Integer.TYPE, Integer.class),
	LONG(Long.TYPE, Long.class),
	FLOAT(Float.TYPE, Float.class),
	DOUBLE(Double.TYPE, Double.class);

	/** Maps primitive {@code Class}es to their corresponding wrapper {@code Class} */
	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = new HashMap<>(6);
	/** Maps wrapper {@code Class}es to their corresponding primitive types */
	private static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP = new HashMap<>(6);
	private static final Map<Class<?>, DataType> TYPE_MAP = new HashMap<>(12);
	static{
		for(final DataType te : values()){
			PRIMITIVE_WRAPPER_MAP.put(te.primitiveType, te.objectiveType);
			WRAPPER_PRIMITIVE_MAP.put(te.objectiveType, te.primitiveType);
			TYPE_MAP.put(te.primitiveType, te);
			TYPE_MAP.put(te.objectiveType, te);
		}
	}

	final Class<?> primitiveType;
	final Class<?> objectiveType;


	DataType(final Class<?> primitiveType, final Class<?> objectiveType){
		this.primitiveType = primitiveType;
		this.objectiveType = objectiveType;
	}

	public static Class<?> toObjectiveTypeOrDefault(final Class<?> primitiveType){
		return PRIMITIVE_WRAPPER_MAP.getOrDefault(primitiveType, primitiveType);
	}

	public static Class<?> toPrimitiveTypeOrDefault(final Class<?> objectiveType){
		return WRAPPER_PRIMITIVE_MAP.getOrDefault(objectiveType, objectiveType);
	}

	public static DataType fromType(final Class<?> type){
		return TYPE_MAP.get(type);
	}

	public static boolean isObjectivePrimitive(final Class<?> type){
		return WRAPPER_PRIMITIVE_MAP.containsKey(type);
	}

}

package org.tmt.aps.peas.j2f;

import java.util.HashMap;
import java.util.Map;

public class DataTypeMapper {
	
	static Map<String, String> fortran2Native = new HashMap<String, String>();	
	static Map<String, String> native2JNI = new HashMap<String, String>();
	static Map<String, String> jni2Java = new HashMap<String, String>();
	static Map<String, String> jni2JavaWrapper = new HashMap<String, String>();
	static Map<String, String> jni2JavaPrimitiveGet = new HashMap<String, String>();

	static {
		fortran2Native.put("BYTE", "unsigned char");
		fortran2Native.put("INTEGER*2", "short");
		fortran2Native.put("INTEGER*4", "int");
		fortran2Native.put("INTEGER", "int");
		fortran2Native.put("LOGICAL", "int");
		fortran2Native.put("REAL*4", "float");
		fortran2Native.put("REAL", "float");
		fortran2Native.put("REAL*8", "double");
		fortran2Native.put("DOUBLE PRECISION", "double");
		fortran2Native.put("INTEGER*8", "long");
	
	 
		native2JNI.put("unsigned char", "jboolean");
		native2JNI.put("short", "jshort");
		native2JNI.put("int", "jint");
		native2JNI.put("float", "jfloat");
		native2JNI.put("double", "jdouble");
		native2JNI.put("long", "jlong");
	
		jni2Java.put("jboolean", "byte");
		jni2Java.put("jshort", "short");
		jni2Java.put("jint", "int");
		jni2Java.put("jfloat", "float");
		jni2Java.put("jdouble", "double");
		jni2Java.put("jlong", "long");
	
		jni2JavaWrapper.put("jboolean", "Byte");
		jni2JavaWrapper.put("jshort", "Short");
		jni2JavaWrapper.put("jint", "Integer");
		jni2JavaWrapper.put("jfloat", "Float");
		jni2JavaWrapper.put("jdouble", "Double");
		jni2JavaWrapper.put("jlong", "Long");


		jni2JavaPrimitiveGet.put("jboolean", "byteValue()");
		jni2JavaPrimitiveGet.put("jshort", "shortValue()");
		jni2JavaPrimitiveGet.put("jint", "intValue()");
		jni2JavaPrimitiveGet.put("jfloat", "floatValue()");
		jni2JavaPrimitiveGet.put("jdouble", "doubleValue()");
		jni2JavaPrimitiveGet.put("jlong", "longValue()");

	}
	
	public static String getNativeFromFortran(String datatype) {
		return fortran2Native.get(datatype);
	}

	public static String getJNIFromNative(String datatype) {
		return native2JNI.get(datatype);
	}

	public static String getJNIFromFortran(String datatype) {
		return native2JNI.get(fortran2Native.get(datatype));
	}
	
	public static String getJavaFromJNI(String datatype) {
		return jni2Java.get(datatype);
	}
	public static String getJavaFromFortran(String datatype) {
		return jni2Java.get(getJNIFromFortran(datatype));
	}
	public static String getJavaWrapperFromFortran(String datatype) {
		return jni2JavaWrapper.get(getJNIFromFortran(datatype));
	}
	public static String getJavaPrimitiveGetMethodFromFortran(String datatype) {
		return jni2JavaPrimitiveGet.get(getJNIFromFortran(datatype));
	}
}

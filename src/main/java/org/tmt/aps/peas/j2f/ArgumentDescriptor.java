package org.tmt.aps.peas.j2f;

public class ArgumentDescriptor {

	String argName;
	String argShortDesc;
	String argLongDesc;
	String argDataType;
	String argUnits;
	String argInOut;
	boolean argIsArray;
	
	public String getArgName() {
		return argName;
	}
	public void setArgName(String argName) {
		this.argName = argName;
	}
	public String getArgShortDesc() {
		return argShortDesc;
	}
	public void setArgShortDesc(String argShortDesc) {
		this.argShortDesc = argShortDesc;
	}
	public String getArgLongDesc() {
		return argLongDesc;
	}
	public void setArgLongDesc(String argLongDesc) {
		this.argLongDesc = argLongDesc;
	}
	public String getArgDataType() {
		return argDataType;
	}
	public void setArgDataType(String argDataType) {
		this.argDataType = argDataType;
	}
	public String getArgUnits() {
		return argUnits;
	}
	public void setArgUnits(String argUnits) {
		this.argUnits = argUnits;
	}
	
	public String getArgInOut() {
		return argInOut;
	}
	public void setArgInOut(String argInOut) {
		this.argInOut = argInOut;
	}
	public boolean isArgIsArray() {
		return argIsArray;
	}
	public void setArgIsArray(boolean argIsArray) {
		this.argIsArray = argIsArray;
	}
	public String getArgCDataType() {
		return DataTypeMapper.getNativeFromFortran(argDataType.toUpperCase());
	}
	public String getArgJNIDataType() {
		// return the datatype that will be declared in the body of the C function (jint, jfloat, etc)
		return DataTypeMapper.getJNIFromFortran(argDataType.toUpperCase());
	}
	public String getArgJavaDataType() {
		// return the datatype that will be declared in the body of the Java method (int, float, etc)
		return DataTypeMapper.getJavaFromFortran(argDataType.toUpperCase());
	}
	public String getArgJavaWrapperClass() {
		return DataTypeMapper.getJavaWrapperFromFortran(argDataType.toUpperCase());
	}
	public String getArgJavaPrimitiveGetMethod() {
		return DataTypeMapper.getJavaPrimitiveGetMethodFromFortran(argDataType.toUpperCase());
	}	
	public boolean isOutput() {
		return (argInOut.toUpperCase().equals("OUT"));
	}
}

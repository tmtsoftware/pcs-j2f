package org.tmt.aps.peas.j2f;

import java.util.ArrayList;
import java.util.List;

public class ArgumentDescriptor {

	String argName;
	String argShortDesc;
	String argLongDesc;
	String argDataType;
	String argUnits;
	String argInOut;
	int argDimension;
	List<ArgumentDescriptor> childArgs = new ArrayList();
	
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
	public boolean isScalar() {
		return (argDimension == 0);
	}
	public boolean isScalarOutput() {
		return (argInOut.toUpperCase().equals("OUT") && argDimension == 0);
	}
	public boolean isScalarInput() {
		return (argInOut.toUpperCase().equals("IN") && argDimension == 0);
	}
	public boolean isInput() {
		return (argInOut.toUpperCase().equals("IN"));
	}
	public int getArgDimension() {
		return argDimension;
	}
	public void setArgDimension(int argDimension) {
		this.argDimension = argDimension;
	}
	public void add(ArgumentDescriptor sizeArgDesc) {
		childArgs.add(sizeArgDesc);
	}
	public List<ArgumentDescriptor> getChildArgs() {
		return childArgs;
	}
	public void setChildArgs(List<ArgumentDescriptor> childArgs) {
		this.childArgs = childArgs;
	}
}

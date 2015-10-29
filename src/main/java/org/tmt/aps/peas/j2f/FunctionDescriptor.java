package org.tmt.aps.peas.j2f;

import java.util.List;

public class FunctionDescriptor {
	
	String functionName;
	boolean generateJni;
	String author;
	String functionShortDesc;
	String functionLongDesc;
	List<ArgumentDescriptor> functionArgs;
	List<ArgumentDescriptor> generatedFunctionArgs; // additional args and order for array sizing
	List<String> usesList;
	
	public String getFunctionName() {
		return functionName;
	}
	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getFunctionShortDesc() {
		return functionShortDesc;
	}
	public void setFunctionShortDesc(String functionShortDesc) {
		this.functionShortDesc = functionShortDesc;
	}
	public String getFunctionLongDesc() {
		return functionLongDesc;
	}
	public void setFunctionLongDesc(String functionLongDesc) {
		this.functionLongDesc = functionLongDesc;
	}
	public List<ArgumentDescriptor> getFunctionArgs() {
		return functionArgs;
	}
	public void setFunctionArgs(List<ArgumentDescriptor> functionArgs) {
		this.functionArgs = functionArgs;
	}
	public List<ArgumentDescriptor> getGeneratedFunctionArgs() {
		return generatedFunctionArgs;
	}
	public void setGeneratedFunctionArgs(List<ArgumentDescriptor> generatedFunctionArgs) {
		this.generatedFunctionArgs = generatedFunctionArgs;
	}
	public List<String> getUsesList() {
		return usesList;
	}
	public void setUsesList(List<String> usesList) {
		this.usesList = usesList;
	}
	public boolean isGenerateJni() {
		return generateJni;
	}
	public void setGenerateJni(boolean generateJni) {
		this.generateJni = generateJni;
	}
	
	
}

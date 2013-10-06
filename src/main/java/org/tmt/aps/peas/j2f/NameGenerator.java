package org.tmt.aps.peas.j2f;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class NameGenerator {
	
	private String fortranFileName;
	private String functionName;
	private FunctionDescriptor fd;
	
	public NameGenerator(String fortranFileName, FunctionDescriptor fd) {
		
		this.fortranFileName = fortranFileName;
		functionName = fd.getFunctionName();
		this.fd = fd;
	}
	
	public String getJavaSourceFileName() {
		return "J" + functionName + ".java";
	}
	
	public String getCSourceFileName() {
		return "c_" + functionName + ".c";
	}
	public String getFortranSourceFileName() {
		return fortranFileName;
	}
	
	public String getJavaClassFileName() {
		return "J" + functionName + ".class";
	}
	
	public String getCObjectFileName() {
		return "c_" + functionName + ".o";
	}
	public String getFortranObjectFileName() {
		String filePrefix = fortranFileName.substring(0, fortranFileName.indexOf("."));
		return filePrefix + ".o";
	}
	
	public String getLibraryFileName() {
		return "lib" + functionName + ".so";
	}
	
	public String getLibraryName() {
		return functionName;
	}
	
	public String getJNIHeaderFileName() {
		return "J" + functionName + ".h";
	}
	
	public String getCFunctionName() {
		return "Java_org_tmt_aps_peas_lang_interop_" + getJavaClassName() + "_" + getJavaNativeMethodName();
	}
	
	public String getJavaNativeMethodName() {
	//	return functionName + "_";
		return functionName;
	}

	public String getJavaMethodName() {
		return "j"+ functionName;
	}
	
	public String getJavaOutClassName() {
		return "j"+ functionName + "_output";
	}
	
	public String getJavaClassName() {
		return "J" + functionName;
	}
	
	public String getNativeMethodSignature() {

		StringBuffer buf = new StringBuffer("RetVal retVal, ");
		
		for (Iterator<ArgumentDescriptor> it = fd.getFunctionArgs().iterator(); it.hasNext(); ) {
			ArgumentDescriptor argDesc = it.next();
			
			if (argDesc.isOutput()) {				
				buf.append(argDesc.getArgJavaDataType() + " " + argDesc.getArgName() + "[], "); 
			} else {
				buf.append(argDesc.getArgJavaDataType() + " " + argDesc.getArgName());
				for (int i=0; i<argDesc.getArgDimension(); i++) {
					buf.append("[]");
				}
				buf.append(", "); 				
			}
		}
		buf.deleteCharAt(buf.length()-2);
 
		
		return buf.toString();
	}

	public String getJavaMethodSignature() {

		StringBuffer buf = new StringBuffer("RetVal retVal, ");
		
		for (Iterator<ArgumentDescriptor> it = fd.getFunctionArgs().iterator(); it.hasNext(); ) {
			ArgumentDescriptor argDesc = it.next();
			
			if (!argDesc.isOutput()) {				
				buf.append(argDesc.getArgJavaDataType() + " " + argDesc.getArgName());
				for (int i=0; i<argDesc.getArgDimension(); i++) {
					buf.append("[]");
				}
				buf.append(", "); 				
			}
		}
		buf.deleteCharAt(buf.length()-2);
 
		
		return buf.toString();
	}
	
	public String getCFunctionSignature() {
		StringBuffer buf = new StringBuffer();

		buf.append("JNIEnv *env, jobject obj, jobject jretval, ");		

		for (Iterator<ArgumentDescriptor> it = fd.getFunctionArgs().iterator(); it.hasNext(); ) {
			ArgumentDescriptor argDesc = it.next();
			
			if (argDesc.isOutput() || argDesc.getArgDimension() > 0) {				
				// TODO: generalize the output
				buf.append(argDesc.getArgJNIDataType() + "Array " + argDesc.getArgName() + ", "); 
			} else {
				buf.append(argDesc.getArgJNIDataType() + " " + argDesc.getArgName() + ", "); 				
			}
		}
		buf.deleteCharAt(buf.length()-2);
 
		
		return buf.toString();
	}
	public String getCCallableFortranFunctionName() {
		// I think there are rules here with underscores in the name depending on if there are underscores in the fortran 
		// subroutine name
		
		//if (getFFEFunctionName().indexOf("_") >= 0) {
		//	return getFFEFunctionName() + "__";
		//} else {		
			return getFFEFunctionName() + "_";
		//}
	}

	public String getFFESourceFileName() {
		// TODO Auto-generated method stub
		return "ffe_" + fortranFileName;
	}

	public String getFFEFunctionName() {
		// TODO Auto-generated method stub
		return "ffe_" + functionName;
	}
	
	
}

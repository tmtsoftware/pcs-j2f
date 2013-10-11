package org.tmt.aps.peas.j2f;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;

public class FileGenerator {

	NameGenerator nameGenerator;
	FunctionDescriptor functionDescriptor;

	public FileGenerator(NameGenerator nameGenerator, FunctionDescriptor functionDescriptor) {
		this.nameGenerator = nameGenerator;
		this.functionDescriptor = functionDescriptor;
	}

	public void generateJavaSourceFile(File stagingDirectory) throws Exception {
		// Generate input string
		StringBuffer content = new StringBuffer();
		content.append("package org.tmt.aps.peas.lang.interop; " + "\n");
		content.append("public class " + nameGenerator.getJavaClassName() + "\n");
		content.append("{\n");
		content.append("\tpublic native void " + nameGenerator.getJavaNativeMethodName() + "(");
		content.append(nameGenerator.getNativeMethodSignature() + ");\n");
		content.append("\tstatic { System.loadLibrary(\"" + nameGenerator.getLibraryName() + "\"); }\n");
		content.append("\t// TODO: We need to write the public method that calls the private and unpacks output arrays\n");
		content.append("\n");
		content.append("\tpublic Object[] " + nameGenerator.getJavaMethodName() + "(");
		content.append(nameGenerator.getJavaMethodSignature() + ") {\n");

		// create output param definitions
		int outputCount = 0;
		content.append("\t\t// Output variable definitions\n");
		for (Iterator<ArgumentDescriptor> it = functionDescriptor.getFunctionArgs().iterator(); it.hasNext();) {
			ArgumentDescriptor argDesc = it.next();
			if (argDesc.isScalarOutput()) {
				outputCount++;
				content.append("\t\t" + argDesc.getArgJavaDataType() + " " + argDesc.getArgName() + "_outArray[] = new "
						+ argDesc.getArgJavaDataType() + "[1];\n");
			}
		}
		
		// deal with array lengths
		content.append("\t\t// Deal with Array Lengths\n");
		for (ArgumentDescriptor argDesc : functionDescriptor.getFunctionArgs()) {
			
			if (argDesc.getArgDimension() != 0) {
				StringBuffer elemZero = new StringBuffer();
				for (int i = 0; i<argDesc.getArgDimension(); i++) {
					content.append("\t\tint " + argDesc.getArgName() + "_len" + (i+1) + " = " + argDesc.getArgName() + elemZero + ".length;\n");
					elemZero.append("[0]"); // for next dimension
				}
			}
			if (argDesc.getArgDimension() == 2) {
				content.append("\t\t" + argDesc.getArgJavaDataType() + "[] " + argDesc.getArgName() + "_collapse = new " + argDesc.getArgJavaDataType() + "[");
				content.append(argDesc.getArgName() + "_len1 * " + argDesc.getArgName() + "_len2");
				content.append("];\n");
			}
		}
		
		// collapse 2-d to 1-d
		for (ArgumentDescriptor argDesc : functionDescriptor.getFunctionArgs()) {
			
			if (argDesc.getArgDimension() == 2) {
				
				content.append("\t\t// collapse array to one dimension\n");
				content.append("\t\tfor (int i=0; i<" + argDesc.getArgName() + "_len1; i++) { \n");
				content.append("\t\t\tfor (int j=0; j<" + argDesc.getArgName() + "_len2; j++) { \n");
				content.append("\t\t\t\t" + argDesc.getArgName() + "_collapse[i*" + argDesc.getArgName() + "_len2 + j] = " + argDesc.getArgName() + "[i][j]; \n");
				content.append("\t\t\t} \n");
				content.append("\t\t} \n");				
			}
		}
		
		
		
		content.append("\t\t// Call native method\n");
		content.append("\t\t" + nameGenerator.getJavaNativeMethodName() + "(retVal, ");
		for (Iterator<ArgumentDescriptor> it = functionDescriptor.getFunctionArgs().iterator(); it.hasNext();) {
			ArgumentDescriptor argDesc = it.next();
			if (argDesc.isScalarOutput()) {
				content.append(argDesc.getArgName() + "_outArray, ");
			} else {
				
				if (argDesc.getArgDimension() == 2) {
					content.append(argDesc.getArgName() + "_collapse, ");
				} else {
					content.append(argDesc.getArgName() + ", ");
				}
				for (int i = 0; i<argDesc.getArgDimension(); i++) {
					content.append(argDesc.getArgName() + "_len" + (i+1) + ",");
				}
			}
		}
		content.deleteCharAt(content.length() - 1);
		content.append(");\n");

		// expand 1-d to 2-d
		for (ArgumentDescriptor argDesc : functionDescriptor.getFunctionArgs()) {
			
			if (argDesc.getArgDimension() == 2) {
				
				content.append("\t\t// expand array to two dimensions\n");
				content.append("\t\tfor (int i=0; i<" + argDesc.getArgName() + "_len1; i++) { \n");
				content.append("\t\t\tfor (int j=0; j<" + argDesc.getArgName() + "_len2; j++) { \n");
				content.append("\t\t\t\t" + argDesc.getArgName() + "[i][j] = " + argDesc.getArgName() + "_collapse[i*" + argDesc.getArgName() + "_len2 + j];\n");
				content.append("\t\t\t} \n");
				content.append("\t\t} \n");				
			}
		}
		
		
		content.append("\t\t// Assign output variables\n");
		content.append("\t\tObject[] out = new Object[" + outputCount + "];\n");
		int outIndex = 0;
		for (Iterator<ArgumentDescriptor> it = functionDescriptor.getFunctionArgs().iterator(); it.hasNext();) {
			ArgumentDescriptor argDesc = it.next();
			if (argDesc.isScalarOutput()) {
				content.append("\t\t" + "out[" + outIndex++ + "] = " + argDesc.getArgName() + "_outArray[0];\n");
			}
		}
		content.append("\t\treturn out;\n");
		content.append("\t}\n");

		content.append("}");

		// create and write to file
		createAndWriteFile(stagingDirectory, nameGenerator.getJavaSourceFileName(), content.toString());
	}

	public void generateCSourceFile(File stagingDirectory) throws Exception {
		// TODO: in this first experiment, we only deal with scalar primitives.
		// Later, we will expand to include support for
		// array data types

		// Generate input string
		StringBuffer content = new StringBuffer();
		content.append("// THIS FILE WAS AUTO-GENERATED BY J2F.  DO NOT MODIFY.\n");
		content.append("#include <stdio.h>\n");
		content.append("#include \"org_tmt_aps_peas_lang_interop_" + nameGenerator.getJNIHeaderFileName()
				+ "\"   // this header was generated by javah\n");
		content.append("\n");
		content.append("struct retval {\n");
		content.append("	   int    code; \n");
		content.append("	   double arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9;\n");
		content.append("	};\n");
		content.append("\n");

		content.append("JNIEXPORT void JNICALL " + nameGenerator.getCFunctionName() + "(" + nameGenerator.getCFunctionSignature()
				+ ")\n");
		content.append("{\n");
		// create the Fortran argument definitions
		// for each argument name, use the corresponding "c" language type and
		// name as "f_" + argName;
		content.append("\t// Define variables used directly in Fortran call\n");
		for (Iterator<ArgumentDescriptor> it = functionDescriptor.getFunctionArgs().iterator(); it.hasNext();) {
			ArgumentDescriptor argDesc = it.next();
			if (argDesc.isScalar()) { 
				content.append("\t" + argDesc.getArgCDataType() + " " + "f_" + argDesc.getArgName() + ";\n");
			}
		}

		// define c variables for return value structure

		content.append("  jclass clazz;\n");
		content.append("  jfieldID fid1;\n");
		content.append("  jfieldID fid2;\n");

		content.append("  struct retval ret_val;\n");
		content.append("  struct retval *ptx = &ret_val;\n");

		// create output param definitions
		content.append("\t// Output variable definitions\n");
		for (Iterator<ArgumentDescriptor> it = functionDescriptor.getFunctionArgs().iterator(); it.hasNext();) {
			ArgumentDescriptor argDesc = it.next();

			if (argDesc.isScalarOutput()) {
				content.append("\t" + argDesc.getArgJNIDataType() + " " + "*jni_" + argDesc.getArgName() + ";\n");
			}

		}
		// first get array elements call
		content.append("\t//  Get a lock on the java output variables\n");
		for (Iterator<ArgumentDescriptor> it = functionDescriptor.getFunctionArgs().iterator(); it.hasNext();) {
			ArgumentDescriptor argDesc = it.next();

			String dataType = argDesc.getArgJavaDataType();

			if (argDesc.isScalarOutput()) {
				content.append("\tjni_" + argDesc.getArgName() + " = (*env)->Get" + Character.toUpperCase(dataType.charAt(0))
						+ dataType.substring(1) + "ArrayElements(env, " + argDesc.getArgName() + ", NULL);\n");

			} else if (argDesc.getArgDimension() > 0) {

				// jfloat *jni_array = (*env)->GetFloatArrayElements(env, jarray, 0);
				content.append("\t" + argDesc.getArgJNIDataType() + " *f_" + argDesc.getArgName() + " = (*env)->Get"
						+ Character.toUpperCase(dataType.charAt(0)) + dataType.substring(1) + "ArrayElements(env, "
						+ argDesc.getArgName() + ", 0);\n");

			}

		}
		// initialize Fortran input variables
		content.append("\t// Initialize Fortran input variables\n");

		content.append("  /* Initialize the structure values to 0 */\n");
		content.append("  ret_val.arg0 = 0.0;\n");
		content.append("  ret_val.arg1 = 0.0;\n");
		content.append("  ret_val.arg2 = 0.0;\n");
		content.append("  ret_val.arg3 = 0.0;\n");
		content.append("  ret_val.arg4 = 0.0;\n");
		content.append("  ret_val.arg5 = 0.0;\n");
		content.append("  ret_val.arg6 = 0.0;\n");
		content.append("  ret_val.arg7 = 0.0;\n");
		content.append("  ret_val.arg8 = 0.0;\n");
		content.append("  ret_val.arg9 = 0.0;\n");

		for (Iterator<ArgumentDescriptor> it = functionDescriptor.getFunctionArgs().iterator(); it.hasNext();) {
			ArgumentDescriptor argDesc = it.next();

			if (argDesc.isScalarInput()) {
				content.append("\tf_" + argDesc.getArgName() + " = " + argDesc.getArgName() + ";\n");
			}
		}
		// call the Fortran Subroutine
		content.append("\t// Call the Fortran subroutine\n");
		content.append("\t" + nameGenerator.getCCallableFortranFunctionName() + "(");
		content.append("ptx, "); // for retVal
		for (Iterator<ArgumentDescriptor> it = functionDescriptor.getFunctionArgs().iterator(); it.hasNext();) {
			ArgumentDescriptor argDesc = it.next();

			if (argDesc.getArgDimension() == 0) {
				content.append("&f_" + argDesc.getArgName() + ",");
			} else {
				content.append("f_" + argDesc.getArgName() + ",");
				for (int i=0; i<argDesc.getArgDimension(); i++) {
					content.append("&" + argDesc.getArgName() + "_len" + (i+1) + ",");
				}
			}
			
			
		}
		// remove trailing ","
		content.deleteCharAt(content.length() - 1);
		content.append(");\n");

		// copy local to output array
		content.append("\t// Copy local variables to output arrays at element 0\n");
		for (Iterator<ArgumentDescriptor> it = functionDescriptor.getFunctionArgs().iterator(); it.hasNext();) {
			ArgumentDescriptor argDesc = it.next();

			if (argDesc.isScalarOutput()) {
				content.append("\tjni_" + argDesc.getArgName() + "[0] = f_" + argDesc.getArgName() + ";\n");
			}
		}

		// release hold on java output arrays
		content.append("\t//  Release lock on the java output variables\n");
		for (Iterator<ArgumentDescriptor> it = functionDescriptor.getFunctionArgs().iterator(); it.hasNext();) {
			ArgumentDescriptor argDesc = it.next();

			if (argDesc.isScalarOutput()) {
				String dataType = argDesc.getArgJavaDataType();
				content.append("\t(*env)->Release" + Character.toUpperCase(dataType.charAt(0)) + dataType.substring(1)
						+ "ArrayElements(env, " + argDesc.getArgName() + ", jni_" + argDesc.getArgName() + ",0);\n");
			} 
		}

		content.append("// put return code into Java\n\n");

		content.append("clazz = (*env)->GetObjectClass(env, jretval);\n");
		content.append("fid1 = (*env)->GetFieldID(env,clazz,\"code\",\"I\");\n");
		content.append("(*env)->SetIntField(env, jretval, fid1, ret_val.code);\n");
		content.append("fid2 = (*env)->GetFieldID(env,clazz,\"arg0\", \"D\");\n");
		content.append("(*env)->SetDoubleField(env, jretval, fid2, ret_val.arg0);\n");
		content.append("fid2 = (*env)->GetFieldID(env,clazz,\"arg1\", \"D\");\n");
		content.append("(*env)->SetDoubleField(env, jretval, fid2, ret_val.arg1);\n");

		content.append("}");

		// create and write to file
		createAndWriteFile(stagingDirectory, nameGenerator.getCSourceFileName(), content.toString());

	}

	public void generateMakefile(File stagingDirectory) throws Exception {

		// Generate input string
		StringBuffer content = new StringBuffer();
		content.append("products: " + nameGenerator.getLibraryFileName() + "\n\n");

		content.append(nameGenerator.getLibraryFileName() + ": " + nameGenerator.getFortranObjectFileName() + " "  + "ffe_" + nameGenerator.getFortranObjectFileName() + " "
				+ nameGenerator.getCObjectFileName() + " structures.o \n");
		// FIXME: library files g2c and gfortran need to be inputs, not
		// constants
		content.append("\t/usr/bin/gcc --shared -o " + nameGenerator.getLibraryFileName() + " "
				+ nameGenerator.getCObjectFileName() + " " + "ffe_" + nameGenerator.getFortranObjectFileName() + " " + nameGenerator.getFortranObjectFileName() + " -lgfortran \n\n");

		content.append(nameGenerator.getCObjectFileName() + ": " + nameGenerator.getCSourceFileName() + " "
				+ nameGenerator.getJNIHeaderFileName() + "\n");
		// FIXME: include files need to be inputs, not constants
		content.append("\tgcc -c -o " + nameGenerator.getCObjectFileName()
				+ " -I/usr/lib/jvm/java-1.6.0-openjdk/include -I/usr/lib/jvm/java-1.6.0-openjdk/include/linux "
				+ nameGenerator.getCSourceFileName() + " -fPIC \n\n");

		content.append(nameGenerator.getJNIHeaderFileName() + ": " + nameGenerator.getJavaClassFileName() + "\n");
		content.append("\tjavah -jni org.tmt.aps.peas.lang.interop." + nameGenerator.getJavaClassName() + "\n\n");

		content.append(nameGenerator.getJavaClassFileName() + ": " + nameGenerator.getJavaSourceFileName() + "\n");
		content.append("\tjavac -d . RetVal.java\n");
		content.append("\tjavac -d . " + nameGenerator.getJavaSourceFileName() + "\n\n");

		content.append("ffe_" + nameGenerator.getFortranObjectFileName() + ": " + "ffe_" + nameGenerator.getFortranSourceFileName()
				+ " structures.mod \n");
		content.append("\tgfortran -c -o " + "ffe_" + nameGenerator.getFortranObjectFileName() + " "
				+ "ffe_" + nameGenerator.getFortranSourceFileName() + " -fPIC -ffree-form\n\n");


		
		content.append(nameGenerator.getFortranObjectFileName() + ": " + nameGenerator.getFortranSourceFileName()
				+ " structures.mod \n");
		content.append("\tgfortran -c -o " + nameGenerator.getFortranObjectFileName() + " "
				+ nameGenerator.getFortranSourceFileName() + " -fPIC -ffree-form\n\n");

		content.append("structures.mod: structures.f90 \n");
		content.append("\tgfortran -c structures.f90 \n\n");

		content.append("clean:\n");
		content.append("\trm *.so *.class *.h *.o\n");

		// create and write to file
		createAndWriteFile(stagingDirectory, "makefile", content.toString());

	}

	public void generateScript(File stagingDirectory, String fortranFileName, String currentDir) throws Exception {

		StringBuffer content = new StringBuffer();
		content.append("#!/bin/sh\n");

		content.append("chmod a+x " + stagingDirectory + File.separator + "*\n");

		// execute makefile
		// copy fortran source file to staging directory
		content.append("cp " + currentDir + "/" + fortranFileName + " " + stagingDirectory.getAbsolutePath() + "\n");
		content.append("cp " + currentDir + "/" + "structures.f90" + " " + stagingDirectory.getAbsolutePath() + "\n");
		content.append("cp " + currentDir + "/" + "RetVal.java" + " " + stagingDirectory.getAbsolutePath() + "\n");
		content.append("cd " + stagingDirectory.getAbsolutePath() + "\n");
		content.append("make" + "\n");

		// TODO: we might want to consider putting everything in a jar.

		// copy Java and library file to current directory
		content.append("cp " + "*.java" + " " + currentDir + "\n");
		content.append("cp " + nameGenerator.getLibraryFileName() + " " + "/opt/apps/lib" + "\n");

		// create and write to file
		createAndWriteFile(stagingDirectory, "script.sh", content.toString());
	}

	private void createAndWriteFile(File dir, String filename, String content) throws Exception {

		File file = new File(dir.getAbsolutePath() + File.separator + filename);
		file.createNewFile();

		BufferedWriter out = new BufferedWriter(new FileWriter(file.getAbsolutePath()));
		out.write(content);
		out.close();
	}

	public void generateFFESourceFile(File stagingDirectory) throws Exception {
		
		// pass 2: put in abstract method name and arguments
		
		// we need to define a set of arguments for internal use (arrays + sizes arguments)
		
		StringBuffer content = new StringBuffer();
		
		// subroutine definition
		content.append("\tsubroutine " + nameGenerator.getFFEFunctionName() +  "(ret_val, ");
		
		for (ArgumentDescriptor argDesc : functionDescriptor.getGeneratedFunctionArgs()) {
			content.append("\t" + argDesc.getArgName() + ", &\n");
		}
		content.delete(content.length() - 4, content.length());
		
		content.append(")\n\n");
		
		content.append("\tUSE Structures\n");
		content.append("\tIMPLICIT NONE\n\n");

		// interface declaration
		content.append("\tINTERFACE\n");
		content.append("\t\tSUBROUTINE " + functionDescriptor.getFunctionName() + "(ret_val, ");
		// Loop over "external" function args, these are the ones of the original fortran file
		for (ArgumentDescriptor argDesc : functionDescriptor.getFunctionArgs()) {
			content.append(argDesc.getArgName() + ",");
		}
		content.deleteCharAt(content.length() - 1);
		content.append(")\n");

		content.append("\t\tUSE Structures\n");
		content.append("\t\tTYPE (RETVAL), INTENT(INOUT) :: RET_VAL\n");
		
		// Loop over "external" function args.  Handles standard data types as well as arrays
		for (ArgumentDescriptor argDesc : functionDescriptor.getFunctionArgs()) {

			content.append("\t\t" + argDesc.getArgDataType() + ", INTENT(" + argDesc.getArgInOut() + ") :: " + argDesc.getArgName());
			
			if (argDesc.getArgDimension() > 0) {
				content.append("(");
				for (int i=0; i<argDesc.getArgDimension(); i++) {
					content.append(":,");
				}
				content.deleteCharAt(content.length() - 1);
				content.append(")");
			}
			content.append("\n");
		
		}
		
		content.append("\t\tEND SUBROUTINE array\n");
		content.append("\tEND INTERFACE\n\n");
      

		content.append("\tTYPE (RETVAL) RET_VAL\n\n");

		content.append("\tINTEGER :: i\n\n");
		content.append("\tINTEGER :: j\n\n");
		
		// define all the "generated" variables, including the dimension sizes
		for (ArgumentDescriptor argDesc : functionDescriptor.getGeneratedFunctionArgs()) {

			content.append("\t" + argDesc.getArgDataType() + " :: " + argDesc.getArgName());
			
			if (argDesc.getArgDimension() > 0) {
				content.append("(");
				for (ArgumentDescriptor sizeArgDesc : argDesc.getChildArgs()) {
					content.append(sizeArgDesc.getArgName() + "*");
				}
				content.deleteCharAt(content.length() - 1);
				content.append(")");
			}
			content.append("\n");
		
		}
		
		// define the target (orginal) arrays as allocatable - use "local_" prefix in name
		for (ArgumentDescriptor argDesc : functionDescriptor.getGeneratedFunctionArgs()) {

			if (argDesc.getArgDimension() > 0) {
				content.append("\t" + argDesc.getArgDataType() + ", ALLOCATABLE :: local_" + argDesc.getArgName());

				content.append("(");
				for (int i=0; i<argDesc.getArgDimension(); i++) {
					content.append(":,");
				}
				content.deleteCharAt(content.length() - 1);
				content.append(")");
			}
			content.append("\n");
		
		}
		content.append("\n");


		// allocate the local_ arrays
		for (ArgumentDescriptor argDesc : functionDescriptor.getGeneratedFunctionArgs()) {

			if (argDesc.getArgDimension() > 0) {
				content.append("\tALLOCATE(local_" + argDesc.getArgName() + "(");

				for (ArgumentDescriptor sizeArgDesc : argDesc.getChildArgs()) {
					content.append(sizeArgDesc.getArgName() + ",");
				}
				content.deleteCharAt(content.length() - 1);
				content.append("))");
			}
			content.append("\n");
		
		}
		content.append("\n");
		

		// copy the array from the input arrays to the local arrays
		// FIXME: just doing one dimension here, fix to be two dimensions
		for (ArgumentDescriptor argDesc : functionDescriptor.getGeneratedFunctionArgs()) {

			if (argDesc.getArgDimension() == 1) {
			//if (argDesc.getArgDimension() > 0 && argDesc.isInput()) {  // TODO: when input/output is reworked
				
				// start do loop
				
				content.append("\tdo i=1, " + argDesc.getChildArgs().get(0).getArgName() + "\n");

				content.append("\t\tlocal_" + argDesc.getArgName() + "(i) = " + argDesc.getArgName() + "(i)\n");
				
				content.append("\tenddo\n");
				
			}
			
			if (argDesc.getArgDimension() == 2) {
			//if (argDesc.getArgDimension() > 0 && argDesc.isInput()) {  // TODO: when input/output is reworked
								
				content.append("\tdo i=1, " + argDesc.getChildArgs().get(0).getArgName() + "\n");
				content.append("\t\tdo j=1, " + argDesc.getChildArgs().get(1).getArgName() + "\n");

				content.append("\t\t\tlocal_" + argDesc.getArgName() + "(i,j) = " + argDesc.getArgName() + "((i-1)*" + argDesc.getChildArgs().get(1).getArgName() + " + j)\n");
				
				content.append("\t\tenddo\n");
				content.append("\tenddo\n");
				
			}
			
			
			content.append("\n");
		}
		content.append("\n");
		

		
		// call the original (external) function

		content.append("\tcall " + functionDescriptor.getFunctionName() + "(ret_val, ");
		for (ArgumentDescriptor argDesc : functionDescriptor.getFunctionArgs()) {
			
			// if the arg is an array, we use the local value to pass
			
			if (argDesc.getArgDimension() > 0) {
				content.append("local_" + argDesc.getArgName() + ",");
			} else {
				content.append( argDesc.getArgName() + ",");				
			}
			
		}
		content.deleteCharAt(content.length() - 1);
		content.append(")\n\n");

		
		// copy the array from the input arrays to the local arrays
		// FIXME: just doing one dimension here, fix to be two dimensions
		for (ArgumentDescriptor argDesc : functionDescriptor.getGeneratedFunctionArgs()) {

			if (argDesc.getArgDimension() == 1) {
			// if (argDesc.getArgDimension() > 0 && argDesc.isOutput()) { // TODO: when input/output is reworked
				
				// start do loop
				
				content.append("\tdo i=1, " + argDesc.getChildArgs().get(0).getArgName() + "\n");

				content.append("\t\t" + argDesc.getArgName() + "(i) = local_" + argDesc.getArgName() + "(i)\n");
				
				content.append("\tenddo\n");
				
			}
			if (argDesc.getArgDimension() == 2) {
			//if (argDesc.getArgDimension() > 0 && argDesc.isInput()) {  // TODO: when input/output is reworked
								
				content.append("\tdo i=1, " + argDesc.getChildArgs().get(0).getArgName() + "\n");
				content.append("\t\tdo j=1, " + argDesc.getChildArgs().get(1).getArgName() + "\n");

				content.append("\t\t\t" + argDesc.getArgName() + "((i-1)*" + argDesc.getChildArgs().get(1).getArgName() + " + j) = local_" + argDesc.getArgName() + "(i,j)\n");
				
				content.append("\t\tenddo\n");
				content.append("\tenddo\n");
				
			}
			
			
			
			content.append("\n");
		}
		content.append("\n");
		

		
		
		
		
		// TODO: We need to DEALLOCATE array too
		
		content.append("\tEND\n");

		// create and write to file
		createAndWriteFile(stagingDirectory, nameGenerator.getFFESourceFileName(), content.toString());


	}

}

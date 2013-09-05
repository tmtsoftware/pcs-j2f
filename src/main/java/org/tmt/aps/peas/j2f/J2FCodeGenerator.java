package org.tmt.aps.peas.j2f;

import java.io.File;
import java.util.Random;

public class J2FCodeGenerator {

	public static void main(String args[]) {
		
		// TODO: handle "usage:" 
		
		// the only input arguments should be:
		// the path to the fortran file to generate from
	    // the java package name (if absent, then no package will be used)
		// the code generator will create a staging temp directory, and output a Java class and a library 
		
		String fortranFile = args[0];
		String javaPackage = null;
		if (args.length > 1) {
			javaPackage = args[1];
		}
				
		try {
		File stagingDirectory = createTempDir();
		String currentDir = new File(".").getAbsolutePath();
		System.out.println("currentDir = " + currentDir);
				
		// Parse the file
		J2FParser parser = new J2FParser();
		FunctionDescriptor fortranFunctionDescriptor = parser.parse(fortranFile);
		
		File ff = new File(fortranFile);
		String fortranFileName = ff.getName();
		
		// Generate names
		NameGenerator nameGenerator = new NameGenerator(fortranFileName, fortranFunctionDescriptor);
		
		// use the nameGenerator as a source of information for creating files
		FileGenerator fileGenerator = new FileGenerator(nameGenerator, fortranFunctionDescriptor);
		
		fileGenerator.generateJavaSourceFile(stagingDirectory);
		fileGenerator.generateCSourceFile(stagingDirectory);
		fileGenerator.generateMakefile(stagingDirectory);
		fileGenerator.generateScript(stagingDirectory, fortranFileName, currentDir);

		executeShellCommand("chmod a+x " + stagingDirectory + File.separator + "*");		
		
		// execute makefile
		executeShellCommand("/bin/sh " + stagingDirectory + File.separator + "script.sh\n");		
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void executeShellCommand(String command) {
		
		try {
			Runtime rt = Runtime.getRuntime();
			Process p = rt.exec(command);
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	private static File createTempDir() {
		final String baseTempPath = System.getProperty("java.io.tmpdir");
		
		Random rand = new Random();
		int randomInt = 1 + rand.nextInt();
		
		File tempDir = new File(baseTempPath + File.separator + "tempDir" + randomInt);
		
		if (!tempDir.exists()) {
			tempDir.mkdir();
		}
		tempDir.deleteOnExit();
		
		return tempDir;
	}
}

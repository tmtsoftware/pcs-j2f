package org.tmt.aps.peas.j2f;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class J2FCodeGenerator {

	static List<String> objectFileNameList = new ArrayList<String>();
	static StringBuffer makeBuffer = new StringBuffer();
	static StringBuffer scriptBuffer = new StringBuffer();
	
	public static void main(String args[]) {

		// TODO: handle "usage:"

		// the only input arguments should be:
		// the path to the fortran file to generate from
		// the java package name (if absent, then no package will be used)
		// the code generator will create a staging temp directory, and output a
		// Java class and a library

		String fortranPath = args[0];
		String workingPath = args[1];
		String javaPackage = null;
		if (args.length > 2) {
			javaPackage = args[2];
		}
		
		String currentDir = new File(workingPath).getAbsolutePath();

		J2FCodeGenerator codeGenerator = new J2FCodeGenerator();

		File fortranDir = new File(fortranPath);
		
		codeGenerator.generate(fortranDir, javaPackage, currentDir);
	
	}

	public String generate(File fortranDir, String javaPackage, String currentDir) {

		try {
			File stagingDirectory = createTempDir();
			System.out.println("currentDir = " + currentDir);
			System.out.println("stagingDirectory = " + stagingDirectory);
			System.out.println("fortranDir = " + fortranDir);

			
			for (File fortranFile : fortranDir.listFiles()) {
			
				System.out.println("fortranFile = " + fortranFile);
				generateFiles(fortranFile, currentDir, stagingDirectory, fortranDir);

			}
			
			String makeHeading = generateMakefileHeading();
			String makeFooter = generateMakefileFooter();

			makeBuffer.insert(0, makeHeading);
			makeBuffer.append(makeFooter);
			
			FileGenerator.createAndWriteFile(stagingDirectory, "makefile", makeBuffer.toString());
			

			String scriptHeading = generateScriptHeading(stagingDirectory, currentDir);
			String scriptFooter = generateScriptFooter(stagingDirectory, currentDir);
			
			scriptBuffer.insert(0, scriptHeading);
			scriptBuffer.append(scriptFooter);

			// create and write to file
			FileGenerator.createAndWriteFile(stagingDirectory, "script.sh", scriptBuffer.toString());

			
			executeShellCommand("chmod a+x " + stagingDirectory + File.separator + "*");

			// execute makefile
			executeShellCommand("/bin/sh " + stagingDirectory + File.separator + "script.sh\n");

		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		return "success";
	}

	private void generateFiles(File fortranFile, String currentDir, File stagingDirectory, File fortranDir) throws Exception {
		// Parse the file
		J2FParser parser = new J2FParser();
		FunctionDescriptor fortranFunctionDescriptor = parser.parse(fortranFile);

		String fortranFileName = fortranFile.getName();

		// Generate names
		NameGenerator nameGenerator = new NameGenerator(fortranFileName, fortranFunctionDescriptor);

		// use the nameGenerator as a source of information for creating files
		FileGenerator fileGenerator = new FileGenerator(nameGenerator, fortranFunctionDescriptor);

		fileGenerator.generateJavaSourceFile(stagingDirectory);
		fileGenerator.generateCSourceFile(stagingDirectory);
		fileGenerator.generateFFESourceFile(stagingDirectory);
		
		String makefileSegment = fileGenerator.generateMakefileSegment(stagingDirectory);
		makeBuffer.append(makefileSegment);
		
		String scriptSegment = fileGenerator.generateScriptSegment(stagingDirectory, fortranFileName, currentDir, fortranDir);
		scriptBuffer.append(scriptSegment);

		List<String> objFiles = fileGenerator.generateObjectFileList();
		objectFileNameList.addAll(objFiles);

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
	
	
	public String generateMakefileHeading() throws Exception {

		// Generate input string
		StringBuffer heading = new StringBuffer();
		heading.append("products: libpeas.so\n\n");

		heading.append("libpeas.so: ");
		for (String objectFileName : objectFileNameList) {
			heading.append(objectFileName + " \\\n");
		}
		heading.deleteCharAt(heading.length()-2);
		heading.append("\tgcc --shared -o libpeas.so ");
		for (String objectFileName : objectFileNameList) {
			heading.append(objectFileName + " \\\n");
		}		
		heading.append(" -lgfortran \n\n");

		return heading.toString();
	}
	
	public String generateMakefileFooter() throws Exception {

		StringBuffer footer = new StringBuffer();

		footer.append("structures.mod: structures.f90 \n");
		footer.append("\tgfortran -c structures.f90 \n\n");

		footer.append("clean:\n");
		footer.append("\trm *.so *.class *.h *.o\n");

		// create and write to file
		return footer.toString();

	}
	
	public String generateScriptHeading(File stagingDirectory, String currentDir) throws Exception {

		StringBuffer content = new StringBuffer();
		content.append("#!/bin/sh\n");

		content.append("chmod a+x " + stagingDirectory + File.separator + "*\n");

		return content.toString();
	}	
		
	public String generateScriptFooter(File stagingDirectory, String currentDir) throws Exception {
		StringBuffer content = new StringBuffer();
		content.append("cd " + stagingDirectory.getAbsolutePath() + "\n");
		content.append("make" + "\n");

		// copy Java and library file to current directory
		content.append("cp " + "*.java" + " " + currentDir + "\n");
		content.append("cp libpeas.so /opt/apps/lib" + "\n");

		return content.toString();
	}

	
}

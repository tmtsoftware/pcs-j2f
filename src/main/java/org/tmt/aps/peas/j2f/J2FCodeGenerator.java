package org.tmt.aps.peas.j2f;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

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

				if (!fortranFile.isDirectory() && !fortranFile.getName().equals("logWrite.f90") && !fortranFile.getName().equals("structures.f90")) {
					System.out.println("fortranFile = " + fortranFile);
					generateFiles(fortranFile, currentDir, stagingDirectory, fortranDir);
				}
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
			System.out.println("Executing: " + stagingDirectory + File.separator + "script.sh ...");
			executeShellCommand("/bin/sh " + stagingDirectory + File.separator + "script.sh\n");
			System.out.println("Done with script.sh\n");

		} catch (Exception e) {
			e.printStackTrace();
			;
			return e.getMessage();
		}
		return "success";
	}

	private void generateFiles(File fortranFile, String currentDir, File stagingDirectory, File fortranDir) throws Exception {
		// Parse the file
		J2FParser parser = new J2FParser();
		FunctionDescriptor fortranFunctionDescriptor = parser.parse(fortranFile);

		if (fortranFunctionDescriptor == null) {
			// found a file that does require processing, skip it.
			return;
		}
		
		String fortranFileName = fortranFile.getName();

		// Generate names
		NameGenerator nameGenerator = new NameGenerator(fortranFileName, fortranFunctionDescriptor);

		// use the nameGenerator as a source of information for creating files
		FileGenerator fileGenerator = new FileGenerator(nameGenerator, fortranFunctionDescriptor);

		if (fortranFunctionDescriptor.isGenerateJni()) {
			fileGenerator.generateJavaSourceFile(stagingDirectory);
			fileGenerator.generateCSourceFile(stagingDirectory);
			fileGenerator.generateFFESourceFile(stagingDirectory);
		}
		
		String makefileSegment = fortranFunctionDescriptor.isGenerateJni() ? 
				fileGenerator.generateMakefileSegment(stagingDirectory) :
				fileGenerator.generateFortranOnlyMakefileSegment(stagingDirectory) ;
		makeBuffer.append(makefileSegment);

		String scriptSegment = fileGenerator.generateScriptSegment(stagingDirectory, fortranFileName, currentDir, fortranDir);
		scriptBuffer.append(scriptSegment);

		List<String> objFiles = fortranFunctionDescriptor.isGenerateJni() ?
				fileGenerator.generateObjectFileList() :
				fileGenerator.generateFortranOnlyObjectFileList();
		objectFileNameList.addAll(objFiles);

	}

	private static void executeShellCommand1(String command) {
		ProcessBuilder builder = new ProcessBuilder(command);

		try {
			final Process process = builder.start();
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
			System.out.println("Program terminated!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void executeShellCommand(String command) {

		try {
			Runtime rt = Runtime.getRuntime();
			Process proc = rt.exec(command);

			// any error message?
			StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), "ERR");

			// any output?
			StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), "OUT");

			// kick them off
			errorGobbler.start();
			outputGobbler.start();

			// any error???
			int exitVal = proc.waitFor();
			System.out.println("ExitValue: " + exitVal);
		} catch (Throwable t) {
			t.printStackTrace();
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
		heading.append("logWrite.o" + " \\\n");
		heading.deleteCharAt(heading.length() - 2);
		heading.append("\tgcc --shared -o libpeas.so ");
		for (String objectFileName : objectFileNameList) {
			heading.append(objectFileName + " \\\n");
		}
		heading.append("logWrite.o" + " \\\n");
		heading.append(" -lgfortran -llapack -lblas\n\n");

		return heading.toString();
	}

	public String generateMakefileFooter() throws Exception {

		StringBuffer footer = new StringBuffer();

		footer.append("structures.mod: structures.f90 \n");
		footer.append("\tgfortran -c structures.f90 \n\n");

		footer.append("mod_logWrite.mod: logWrite.f90 \n");
		footer.append("\tgfortran -c logWrite.f90 -fPIC \n\n");

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

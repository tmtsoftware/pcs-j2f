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
			//File stagingDirectory = createTempDir();
			File stagingDirectory = new File("/opt/apps/j2f/staging");
			if (! stagingDirectory.exists()){
				stagingDirectory.mkdir();
		    }
			
			// but since this is generating files, they will always be newer and have to be recompiled.  How to fix?
			
			
			
			System.out.println("currentDir = " + currentDir);
			System.out.println("stagingDirectory = " + stagingDirectory);
			System.out.println("fortranDir = " + fortranDir);

			for (File fortranFile : fortranDir.listFiles()) {

				if (!fortranFile.isDirectory() && !fortranFile.getName().equals("logWrite.f90") && !fortranFile.getName().equals("structures.f90")) {
					//System.out.println("fortranFile = " + fortranFile);
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

			//System.out.println("chmod a+x " + stagingDirectory + File.separator + "*.sh");
			//executeShellCommand("chmod a+x " + stagingDirectory + File.separator + "*.sh");

			// execute makefile
			System.out.println("Executing: " + stagingDirectory + File.separator + "script.sh ...");
			executeShellCommand("/bin/sh " + stagingDirectory + File.separator + "script.sh\n");
			System.out.println("Done with script.sh\n");

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

    
private String generateMakefileCompilerSection() {
    return
        "UNAME_S := $(shell uname -s)\n" +
        "\n" +
        "# Build mode:\n" +
        "#\n" +
        "# make TOOLCHAIN=gcc\n" +
        "# C + JNI + Fortran\n" +
        "#\n" +
        "# make TOOLCHAIN=fortran\n" +
        "# Fortran-oriented toolchain\n" +
        "#\n" +
        "TOOLCHAIN ?= gcc\n" +
        "\n" +
        "FC := gfortran\n" +
        "LD := gfortran\n" +
        "\n" +
        "PKG_CONFIG := pkg-config\n" +
        "\n" +
        "JAVA_HOME ?= $(shell /usr/libexec/java_home 2>/dev/null)\n" +
        "\n" +
        "ifeq ($(UNAME_S),Darwin)\n" +
        "\n" +
        "# ----------------------------------------------------------------------\n" +
        "# macOS\n" +
        "# ----------------------------------------------------------------------\n" +
        "\n" +
        "SHLIB_EXT := dylib\n" +
        "\n" +
        "# Use the SDK selected by xcrun rather than hard-coding an SDK version.\n" +
        "SDKROOT := $(shell xcrun --sdk macosx --show-sdk-path 2>/dev/null)\n" +
        "\n" +
        "# Both toolchains use JNI and Fortran.\n" +
        "# TOOLCHAIN currently identifies the build mode; compiler selection\n" +
        "# is kept explicit so it can be changed independently later.\n" +
        "\n" +
        "JNI_INC := -I$(JAVA_HOME)/include \\\n" +
        "           -I$(JAVA_HOME)/include/darwin\n" +
        "\n" +
        "ifeq ($(TOOLCHAIN),gcc)\n" +
        "\n" +
        "# C + JNI + Fortran build\n" +
        "CC := gcc\n" +
        "FC := gfortran\n" +
        "LD := gfortran\n" +
        "\n" +
        "SHLIB_LDFLAGS := -dynamiclib \\\n" +
        "                 -Wl,-undefined,dynamic_lookup\n" +
        "\n" +
        "else ifeq ($(TOOLCHAIN),fortran)\n" +
        "\n" +
        "# Fortran-oriented build\n" +
        "CC := gcc\n" +
        "FC := gfortran\n" +
        "LD := gfortran\n" +
        "\n" +
        "SHLIB_LDFLAGS := -dynamiclib\n" +
        "\n" +
        "else\n" +
        "\n" +
        "$(error Unknown TOOLCHAIN='$(TOOLCHAIN)'. Use gcc or fortran)\n" +
        "\n" +
        "endif\n" +
        "\n" +
        "else ifeq ($(UNAME_S),Linux)\n" +
        "\n" +
        "# ----------------------------------------------------------------------\n" +
        "# Linux\n" +
        "# ----------------------------------------------------------------------\n" +
        "\n" +
        "CC := gcc\n" +
        "FC := gfortran\n" +
        "LD := gfortran\n" +
        "\n" +
        "JNI_INC := -I$(JAVA_HOME)/include \\\n" +
        "           -I$(JAVA_HOME)/include/linux\n" +
        "\n" +
        "FORTRAN_INCLUDES := -I/usr/include -I/usr/local/include\n" +
        "\n" +
        "SHLIB_EXT := so\n" +
        "SHLIB_LDFLAGS := -shared\n" +
        "\n" +
        "else\n" +
        "\n" +
        "$(error Unsupported operating system: $(UNAME_S))\n" +
        "\n" +
        "endif\n" +
        "\n" +
        "CFLAGS += -fPIC $(JNI_INC)\n" +
        "\n" +
        "FFLAGS += -fPIC -ffree-form $(FORTRAN_INCLUDES)\n" +
        "FFLAGS += -fopenmp\n" +
        "\n" +
        "# On macOS, use the SDK selected by xcrun.\n" +
        "ifeq ($(UNAME_S),Darwin)\n" +
        "CFLAGS += -isysroot $(SDKROOT)\n" +
        "FFLAGS += -isysroot $(SDKROOT)\n" +
        "LDFLAGS += -isysroot $(SDKROOT)\n" +
        "endif\n" +
        "\n" +
        "# ----------------------------------------------------------------------\n" +
        "# External libraries\n" +
        "# ----------------------------------------------------------------------\n" +
        "\n" +
        "BLAS_LIBS   := $(shell $(PKG_CONFIG) --libs blas 2>/dev/null)\n" +
        "LAPACK_LIBS := $(shell $(PKG_CONFIG) --libs lapack 2>/dev/null)\n" +
        "FFTW_LIBS   := $(shell $(PKG_CONFIG) --libs fftw3 2>/dev/null)\n" +
        "\n" +
        "FFTW_CFLAGS := $(shell $(PKG_CONFIG) --cflags fftw3 2>/dev/null)\n" +
        "FORTRAN_CFLAGS := $(shell $(PKG_CONFIG) --cflags blas lapack fftw3 2>/dev/null)\n" +
        "\n" +
        "# C/JNI code needs FFTW headers.\n" +
        "CFLAGS += $(FFTW_CFLAGS)\n" +
        "\n" +
        "# Fortran code needs BLAS/LAPACK/FFTW headers.\n" +
        "FFLAGS += $(FORTRAN_CFLAGS)\n" +
        "\n" +
        "# Linker flags.\n" +
        "LDFLAGS += $(SHLIB_LDFLAGS)\n" +
        "\n" +
        "# Libraries.\n" +
        "LIBS += $(BLAS_LIBS)\n" +
        "LIBS += $(LAPACK_LIBS)\n" +
        "LIBS += $(FFTW_LIBS)\n" +
        "\n" +
        "$(info ==============================)\n" +
        "$(info TOOLCHAIN       = $(TOOLCHAIN))\n" +
        "$(info UNAME_S         = $(UNAME_S))\n" +
        "$(info SDKROOT         = $(SDKROOT))\n" +
        "$(info JAVA_HOME       = $(JAVA_HOME))\n" +
        "$(info CC              = $(CC))\n" +
        "$(info FC              = $(FC))\n" +
        "$(info LD              = $(LD))\n" +
        "$(info PKG_CONFIG      = $(PKG_CONFIG))\n" +
        "$(info CFLAGS          = $(CFLAGS))\n" +
        "$(info FFLAGS          = $(FFLAGS))\n" +
        "$(info LDFLAGS         = $(LDFLAGS))\n" +
        "$(info LIBS            = $(LIBS))\n" +
        "$(info ==============================)\n";
}
	public String generateMakefileHeading() throws Exception {

        StringBuffer heading = new StringBuffer();

        heading.append(generateMakefileCompilerSection() + "\n\n");
                
		// Generate input string
		
		heading.append("products: libpeas.$(SHLIB_EXT)\n\n");

		heading.append("libpeas.$(SHLIB_EXT): ");
		for (String objectFileName : objectFileNameList) {
			heading.append(objectFileName + " \\\n");
		}
		heading.append("logWrite.o" + " \\\n");
		heading.deleteCharAt(heading.length() - 2);
		heading.append("\t$(LD) $(LDFLAGS) -o libpeas.$(SHLIB_EXT) ");
		for (String objectFileName : objectFileNameList) {
			heading.append(objectFileName + " \\\n");
		}
		heading.append("logWrite.o" + " \\\n");
		heading.append(" $(LIBS) \n\n");

		return heading.toString();
	}

	public String generateMakefileFooter() throws Exception {

		StringBuffer footer = new StringBuffer();

		footer.append("structures.mod: structures.f90 \n");
		footer.append("\tgfortran -c structures.f90 \n\n");

		footer.append("mod_logwrite.mod: logWrite.f90 \n");
		footer.append("\tgfortran -c logWrite.f90 $(CFLAGS) \n\n");

		footer.append("clean:\n");
		footer.append("\trm *.so *.class *.h *.o\n");

		// create and write to file
		return footer.toString();

	}

	public String generateScriptHeading(File stagingDirectory, String currentDir) throws Exception {

		StringBuffer content = new StringBuffer();
		content.append("#!/bin/sh\n");

		content.append("chmod a+x " + stagingDirectory + File.separator + "*\n");
        
        content.append("export PKG_CONFIG_PATH=\"/usr/local/opt/openblas/lib/pkgconfig:$PKG_CONFIG_PATH\"\n\n");
        content.append("export MACOSX_DEPLOYMENT_TARGET=15.0\n\n");
        
        content.append("#!/bin/bash\n\n");
        content.append("UNAME_S=$(uname -s)\n\n");
        content.append("if [ \"$UNAME_S\" = \"Darwin\" ]; then\n");
        content.append("    # macOS\n");
        content.append("    SHLIB_EXT=\"dylib\"\n");
        content.append("elif [ \"$UNAME_S\" = \"Linux\" ]; then\n");
        content.append("    # Linux\n");
        content.append("    SHLIB_EXT=\"so\"\n");
        content.append("else\n");
        content.append("    echo \"Unsupported OS: $UNAME_S\"\n");
        content.append("    exit 1\n");
        content.append("fi\n\n");
        content.append("echo \"Shared library extension is: $SHLIB_EXT\"\n");

        


		return content.toString();
	}

	public String generateScriptFooter(File stagingDirectory, String currentDir) throws Exception {
		StringBuffer content = new StringBuffer();
		content.append("cd " + stagingDirectory.getAbsolutePath() + "\n");
        content.append("export CFLAGS=\"-mmacosx-version-min=15.0 ${CFLAGS}\"\n");
        content.append("export CXXFLAGS=\"-mmacosx-version-min=15.0 ${CXXFLAGS}\"\n");
        content.append("export LDFLAGS=\"-mmacosx-version-min=15.0 ${LDFLAGS}\"\n");
		
        content.append("make" + "\n");

		content.append("jar -cf peas-lang-interop.jar org" + "\n");
		// copy Java and library file to current directory
		content.append("cp " + "org/tmt/aps/peas/lang/interop/*.java ${GIT_HOME}/pcs-fortran/src/main/java/org/tmt/aps/peas/lang/interop" + "\n");
        content.append("mkdir -p /opt/apps/lib\n");
        content.append("mkdir -p /opt/apps/include\n");
		content.append("cp libpeas.$SHLIB_EXT /opt/apps/lib" + "\n");
		content.append("cp peas-lang-interop.jar /opt/apps/lib" + "\n");
		content.append("cp *.mod /opt/apps/include" + "\n");

		return content.toString();
	}

}

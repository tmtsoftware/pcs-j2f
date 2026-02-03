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

	public String generateMakefileHeading() throws Exception {

        StringBuffer heading = new StringBuffer();

        heading.append("UNAME_S := $(shell uname -s)\n\n");
        
        heading.append("CC ?= gcc\n\n");
        heading.append("FC ?= gfortran\n\n");
        
        heading.append("BLAS_LIBS := $(shell $(PKG_CONFIG) --libs blas)\n");
        heading.append("FFTW_LIBS := $(shell $(PKG_CONFIG) --libs fftw3)\n\n");
        
        
        
        

        heading.append("JAVA_HOME ?= $(shell /usr/libexec/java_home 2>/dev/null)\n\n");
        heading.append("ifeq ($(UNAME_S),Darwin)\n");
        
        heading.append("\t# macOS\n");
        heading.append("\tLD := clang\n");
        
        
        heading.append("\tJNI_INC = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin\n");
        heading.append("\tFORTRAN_INCLUDES=\n");
        heading.append("\tSHLIB_EXT     := dylib  \n");
        heading.append("\tSHLIB_LDFLAGS := -dynamiclib -Wl,-undefined,dynamic_lookup  \n");
        heading.append("\t# OpenMP (Homebrew LLVM + libomp)  \n");
        heading.append("\tOMP_CFLAGS  := -Xpreprocessor -fopenmp -I/usr/local/opt/libomp/include  \n");
        heading.append("\tOMP_LDFLAGS := -L/usr/local/opt/libomp/lib -Wl,-rpath,/usr/local/opt/libomp/lib -lomp\n");
        
        heading.append("\tGFORTRAN_LIBDIR := $(shell gfortran -print-file-name=libgfortran.dylib | xargs dirname)\n");
        heading.append("\tFORTRAN_LIBS := -L$(GFORTRAN_LIBDIR) -Wl,-rpath,$(GFORTRAN_LIBDIR) -lgfortran\n");
        
        heading.append("else ifeq ($(UNAME_S),Linux)\n");
        
        heading.append("\t# Linux\n");
        heading.append("\tLD := $FC\n");

        heading.append("\tJNI_INC = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux\n");
        heading.append("\tFORTRAN_INCLUDES = -I/usr/include -I/usr/local/include\n");
        heading.append("\tSHLIB_EXT     := so  \n");
        heading.append("\tSHLIB_LDFLAGS := -shared  \n");
        heading.append("\tOMP_CFLAGS  := -fopenmp  \n");
        heading.append("\tOMP_LDFLAGS := -fopenmp  \n");
        heading.append("\tFORTRAN_LIBS := \n");
        
        heading.append("endif\n\n");
        heading.append("CFLAGS += -fPIC $(JNI_INC)\n");
        
        heading.append("FFLAGS += -fPIC -ffree-form $(FORTRAN_INCLUDES)\n\n");
        heading.append("# handle external libraries\n\n");
        heading.append("FFTW_CFLAGS := $(shell pkg-config --cflags fftw3)\n");
        heading.append("FFTW_LIBS   := $(shell pkg-config --libs fftw3)\n\n");
        heading.append("PKGS = fftw3 blas lapack\n\n");
        heading.append("CFLAGS  += $(shell pkg-config --cflags $(PKGS))\n");
        heading.append("FFLAGS  += $(shell pkg-config --cflags $(PKGS))\n");
        
        heading.append("CFLAGS := $(filter-out -fopenmp,$(CFLAGS))\n\n");     
        

        heading.append("LDFLAGS += $(shell pkg-config --cflags $(PKGS))\n");
        heading.append("LDFLAGS += $(SHLIB_LDFLAGS) $(OMP_LDFLAGS)\n\n");
            
        heading.append("LIBS += $(BLAS_LIBS) $(FFTW_LIBS) $(OMP_LDFLAGS) $(FORTRAN_LIBS)\n\n");
        
		// Generate input string
		
		heading.append("products: libpeas.$(SHLIB_EXT)\n\n");

		heading.append("libpeas.$(SHLIB_EXT): ");
		for (String objectFileName : objectFileNameList) {
			heading.append(objectFileName + " \\\n");
		}
		heading.append("logWrite.o" + " \\\n");
		heading.deleteCharAt(heading.length() - 2);
		heading.append("\t$(LD) $(SHLIB_LDFLAGS) -o libpeas.$(SHLIB_EXT) ");
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
		content.append("cp " + "*.java" + " " + currentDir + "\n");
        content.append("mkdir -p /opt/apps/lib\n");
        content.append("mkdir -p /opt/apps/include\n");
		content.append("cp libpeas.$SHLIB_EXT /opt/apps/lib" + "\n");
		content.append("cp peas-lang-interop.jar /opt/apps/lib" + "\n");
		content.append("cp *.mod /opt/apps/include" + "\n");

		return content.toString();
	}

}

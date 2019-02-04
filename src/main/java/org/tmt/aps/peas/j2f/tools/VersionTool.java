package org.tmt.aps.peas.j2f.tools;

import java.io.File;
import java.io.FileFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * Stand-alone Java program that searches a Maven repository for versions of an artifact.
 * @author smichaels
 *
 */
public class VersionTool {

	/**
	 * Main program entry point
	 * @param args two elements are required, the first is the switch and the second is the directory path to search
	 * switch values are:
	 * -l lists all versions found
	 * -v returns the current version number
	 * -i returns the next version number available

	 */
	public static void main(String[] args) {
		
		DateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a z");
		
		String path = args[1];
		
		File dir = new File(path);
		FileFilter filter = new DirFileFilter();
		File[] versionDirs = dir.listFiles(filter);
		
		if (versionDirs == null || versionDirs.length == 0) {
			if (args[0].equals("-l")) {
				System.out.println("No versions are installed");
			}
			if (args[0].equals("-v")) {
				System.out.println("No versions are installed");
			}
			if (args[0].equals("-i")) {
				System.out.println("1.0");
			}
		} else {
		
		Arrays.sort(versionDirs, new Comparator<File>(){
		    public int compare(File f1, File f2)
		    {
		        return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
		    } });
		
		// if args[0] == "-l"
		// list them
		if (args[0].equals("-l")) {
			System.out.println("\n");
			for (File versionDir : versionDirs) {
				Date date = new Date(versionDir.lastModified());
				System.out.println("version: " + versionDir.getName() + "   " + sdf.format(date));
			}
			System.out.println("\n");
		}
		
		// if args[0] == "-v"
		// return current version
		if (args[0].equals("-v")) {
			System.out.println(versionDirs[0].getName());
		}
		
		
		// if args[0] == "-i"
		// return an incremented number
		if (args[0].equals("-i")) {
			String latest = versionDirs[0].getName();
			String majorRelease = latest.substring(0, latest.indexOf("."));
			String minorRelease = latest.substring(latest.indexOf(".")+1);
			int minorInt = new Integer(minorRelease);
			System.out.println(majorRelease + "." + (++minorInt));
		}
		}
		
	}

}

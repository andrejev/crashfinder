package de.hdu.pvs.crashfinder.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * 
 * @author Mohammad Ghanavati Created in November 2015
 */

public class Differencer {

	public void extractDiffJavaFile(String inputPath, String outputPath,
			String fileOutput) throws IOException {

		String output;
		File file = new File(fileOutput);
		Process p = Runtime.getRuntime().exec(
				"diff -ENwbur" + " " + inputPath + " " + outputPath);

		BufferedReader stdInput = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		PrintStream writer1 = new PrintStream(file, "UTF-8");
		boolean check = false;
		try {
			while ((output = stdInput.readLine()) != null) {
				if (output.startsWith("diff -ENwbur")
						&& output.endsWith(".java")) {
					writer1.println(output);
					check = true;
					continue;
				} else if (output.startsWith("diff -ENwbur")
						&& !output.endsWith(".java")) {
					check = false;
				} else if (check) {
					writer1.println(output);
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
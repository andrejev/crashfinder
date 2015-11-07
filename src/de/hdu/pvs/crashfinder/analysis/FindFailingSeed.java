package de.hdu.pvs.crashfinder.analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

public class FindFailingSeed {

	List<String> lines = new ArrayList<>();
	List<String> stackFrames = new ArrayList<>();
	Charset charset = Charset.forName("US-ASCII");
	private boolean passed = false;

	public Seed computeSeed(String failedLogFile) throws FileNotFoundException {
		BufferedReader br;
		String line;
		try {
			br = new BufferedReader(new FileReader(failedLogFile));
			while ((line = br.readLine()) != null) {
				lines.add(line.replaceAll("^\\s+", ""));
			}
		} catch (IOException x) {
			System.err.println("Stack trace log file not found");
		}

		for (int i = lines.size() - 1; i > 0; i--) {
			if (lines.get(i).startsWith("at ")) {
				stackFrames.add(lines.get(i).replace("at ", ""));
				passed = true;
			} else if (passed) {
				break;
			}
		}
		String rawSeed = stackFrames.get(stackFrames.size() - 2);

		String classFile = rawSeed.split(".java:")[0].split("\\(")[1];
		int lineNumber = Integer.parseInt(rawSeed.split(".java:")[1]
				.split("\\)")[0]);
		String packageName = rawSeed.split(classFile)[0];
		String seedClass = packageName + classFile;

		return new Seed(lineNumber, seedClass);

	}

	public Statement findSeedStatementFailing(String pathToStackTrace,
			Slicing slicing) throws IOException {
		int lineNumber = computeSeed(pathToStackTrace).getLineNumber();
		String seedClass = computeSeed(pathToStackTrace).getSeedClass();
		try {
			Statement result = slicing.extractStatementfromException(seedClass,
					lineNumber);
			return result;
		} catch (InvalidClassFileException e) {
			System.out.println(e.getStackTrace());
			return null;
		}
	}

	public class Seed {
		String seedClass;
		int lineNumber;

		public Seed(int lineNumber, String seedClass) {
			super();
			this.lineNumber = lineNumber;
			this.seedClass = seedClass;
		}

		public String getSeedClass() {
			return seedClass;
		}

		public void setSeedClass(String seed) {
			this.seedClass = seed;
		}

		public int getLineNumber() {
			return lineNumber;
		}

		public void setLineNumber(int lineNumber) {
			this.lineNumber = lineNumber;
		}
	}
}
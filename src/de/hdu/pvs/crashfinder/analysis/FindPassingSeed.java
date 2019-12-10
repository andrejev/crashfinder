package de.hdu.pvs.crashfinder.analysis;

import java.io.*;

import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

/**
 * 
 * @author Mohammad Ghanavati Created in November 2015
 */

public class FindPassingSeed {

	public String computeSeed(String failingSeed, String diffFile)
			throws IOException {
		ProcessBuilder pb = new ProcessBuilder("python", "src/resources/temp.py", ""
				+ failingSeed, "" + diffFile);
		Process p = pb.start();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		String ret = in.readLine();
		return ret;
	}
	
	public Statement findSeedStatement(String seed,
			Slicing slicing) throws IOException, InterruptedException {
		String[] splitSeed = seed.split(":");

		try {
			return slicing.extractStatementfromException(splitSeed[0],
					Integer.parseInt(splitSeed[1]));
		} catch (InvalidClassFileException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
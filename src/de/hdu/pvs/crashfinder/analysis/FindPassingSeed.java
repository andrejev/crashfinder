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

		String pyCode = "from unidiff import parse_unidiff, LINE_TYPE_ADD, LINE_TYPE_DELETE\n"
				+ "import sys\n"
				+ "\n"
				+ "def findSeed(failingSeed, diffFilePath):\n"
				+ "    \"\"\"\n"
				+ "    Finds seed statement for the passing version from diff file and the seed statement of the failing version\n"
				+ "    using unidiff module\n"
				+ "    return: seed statement for the passing version as a string 'srcFile:linenumber'\n"
				+ "    \"\"\"\n"
				+ "    srcFile = failingSeed.split(':')[0].replace('.', '/')\n"
				+ "    #print srcFile\n"
				+ "    failingSeedLineNum = int(failingSeed.split(':')[1])\n"
				+ "    #print failingSeedLineNum\n"
				+ "\n"
				+ "    parser = parse_unidiff(open(diffFilePath))\n"
				+ "    passingSeedLineNum = failingSeedLineNum\n"
				+ "    modifiedLines = []\n"
				+ "    for parsed in parser:\n"
				+ "        if srcFile in str(parsed):\n"
				+ "            #print str(parsed)\n"
				+ "            if '.java' in str(parsed) and srcFile in str(parsed):\n"
				+ "                for hunk in parsed:\n"
				+ "                    #print hunk.target_start\n"
				+ "                    if hunk.target_start < failingSeedLineNum:\n"
				+ "                        #print hunk\n"
				+ "                        \"\"\" hunk contains one block of modified code. List hunk.target_lines contains\n"
				+ "                            the lines in the target (new) version, and list hunk.target_types gives\n"
				+ "                            change type of each line (e.g. '+' == added). There are also hunk.target_length\n"
				+ "                            and corresponding fields hunk.source_*\n"
				+ "                        \"\"\"\n"
				+ "                        modifiedLines += hunk.target_types + hunk.source_types\n"
				+ "                        addedLines = modifiedLines.count('+')\n"
				+ "                        deletedLines = modifiedLines.count('-')\n"
				+ "                        passingSeedLineNum = failingSeedLineNum - addedLines + deletedLines\n"
				+ "    passingSeed = ('%s:%s' % (srcFile, passingSeedLineNum)).replace('/','.')\n"
				+ "    return passingSeed\n"
				+ "\n"
				+ "\n"
				+ "def main(diffFilePath,failingSeed):\n"
				+ "    passingSeed = findSeed(failingSeed, diffFilePath)\n"
				+ "    print passingSeed\n"
				+ "\n"
				+ "if __name__ == \"__main__\":\n"
				+ "    failingSeed = sys.argv[1]\n"
				+ "    diffFilePath = sys.argv[2]\n"
				+ "    main(diffFilePath,failingSeed)\n";

		BufferedWriter out = new BufferedWriter(new FileWriter("temp.py"));
		out.write(pyCode);
		out.close();

		ProcessBuilder pb = new ProcessBuilder("python", "temp.py", ""
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
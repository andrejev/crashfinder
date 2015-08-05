package de.hdu.pvs.crashfinder.instrument;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
//import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;

import de.hdu.pvs.crashfinder.analysis.FindSeed;
import de.hdu.pvs.crashfinder.analysis.IRStatement;
import de.hdu.pvs.crashfinder.analysis.Slicing;
import de.hdu.pvs.crashfinder.analysis.SlicingOutput;
import de.hdu.pvs.crashfinder.util.WALAUtils;

//import analysis.ShrikePoint;
import junit.framework.TestCase;

public class TestStmtInstrumenter extends TestCase {
	public void testSimpleInstrumenter() throws Exception {

		String dir = "/home/felix/hadoop/hdfs3856/4/share/hadoop/hdfs/";
		String classPath = dir + "hadoop-hdfs-3.0.0-SNAPSHOT.jar";
		String fileName = "/home/felix/workspace/dumpslice.txt";
		String diffout = "/home/felix/workspace/diffout.diff";
		String diff = "/home/felix/workspace/log.diff";
		String mainClass = "";
		String exclusionFile = "/home/felix/workspace/regressionFaultLocalizer/src/resources/JavaAllExclusions.txt";
		String failedLogFile = "src/resources/stackTraceFail.log";
		
		FindSeed computeSeed= new FindSeed();
		int lineNumber = computeSeed.computeSeed(failedLogFile).getLineNumber();
		String seedClass = computeSeed.computeSeed(failedLogFile).getSeedClass();
		
		BufferedReader br = null;
		String sCurrentLine;
		PrintWriter output = null;
		List<String> diffClass = new ArrayList<String>();
		List<String> matching = new ArrayList<String>();

		Slicing helper = new Slicing(classPath, mainClass, exclusionFile);
		helper.CallGraphBuilder();
		helper.setDataDependenceOptions(DataDependenceOptions.NONE);
		helper.setControlDependenceOptions(ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
		helper.setContextSensitive(true); // context-insensitive

		Statement s = helper.extractStatementfromException(seedClass, lineNumber);

		Collection<Statement> slice = null;
		System.out.println("--- backward ---");
		slice = helper.computeSlice(s);
		WALAUtils.dumpSliceToFile(slice, fileName);

		// SlicingOutput output1 = helper.outputSlice(s);
		try {
			output = new PrintWriter(
					new BufferedWriter(new FileWriter(diffout)));
			output.write("");

			br = new BufferedReader(new FileReader(diff));
			while ((sCurrentLine = br.readLine()) != null) {
				Pattern p = Pattern.compile("\\+++ /home/felix/2/(.*?).java");
				Matcher m = p.matcher(sCurrentLine);
				if (m.find()) {
					matching.add(m.group());
				}
			}
			for (String line : matching) {
				line = line.replaceAll("\\+++ /home/felix/2/", "");
				line = line.replaceAll("\\.java", "");
				System.out.println(line);
				line = line.replace("/", ".");
				// sCurrentLine = sCurrentLine.replace("/", ".");
				diffClass.add(line);
				output.printf("%s\r\n", line);
			}

		} catch (IOException e) {
			// oh no!
		} finally {
			if (output != null) {
				output.close();
			}
		}

		String fullClassName = null;
		Collection<Statement> sliceDiff = new LinkedHashSet<Statement>();
		for (Statement s1 : slice) {
			String extractedFullClassName = WALAUtils.getJavaFullClassName(s1.getNode()
					.getMethod().getDeclaringClass());
			if (extractedFullClassName.contains("$")) {

				String[] dollarReplace = extractedFullClassName.split("\\$");
				fullClassName = dollarReplace[0];
			} else {
				fullClassName = extractedFullClassName;
			}
			if (diffClass.contains(fullClassName)) {
				sliceDiff.add(s1);
			}
		}

		Collection<IRStatement> irs = Slicing.convert(sliceDiff);
		SlicingOutput output1 = new SlicingOutput(irs);
		// System.out.println(output1);

		// InstrumenterBench instrumenter = new InstrumenterBench();
		// EveryStmtInstrumenter instrumenter = new EveryStmtInstrumenter();
		RelatedStmtInstrumenter instrumenter = new RelatedStmtInstrumenter(
				output1);
		instrumenter.instrument(classPath, dir
				+ "hadoop-hdfs-3.0.0-SNAPSHOT-instrumented.jar");
		InstrumentStats.showInstrumentationStats();

	}
}
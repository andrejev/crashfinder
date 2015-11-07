package de.hdu.pvs.crashfinder.test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

import de.hdu.pvs.crashfinder.util.WALAUtils;
import de.hdu.pvs.crashfinder.analysis.Differencer;
import de.hdu.pvs.crashfinder.analysis.FindFailingSeed;
import de.hdu.pvs.crashfinder.analysis.Intersection;
import de.hdu.pvs.crashfinder.analysis.FindPassingSeed;
import de.hdu.pvs.crashfinder.analysis.Slicing;

public class TestSlicing {

	/**
	 * @param args
	 * @throws CancelException
	 * @throws IOException
	 * @throws WalaException
	 * @throws IllegalArgumentException
	 * @throws InvalidClassFileException
	 */
	public static void main(String[] args) throws IllegalArgumentException,
			WalaException, IOException, CancelException,
			InvalidClassFileException {

		String classPath = "/home/felix/hadoop/hdfs3856/2/share/hadoop/hdfs/hadoop-hdfs-3.0.0-SNAPSHOT.jar";
		String fileName = "dumpslice.txt";
		String mainClass = "Lorg/apache/hadoop/hdfs/server/namenode/NameNode";
		String exclusionFile = "src/resources/JavaAllExclusions.txt";
		String failedLogFile = "src/resources/log-stacktrace.txt";
		String inputPath = "/home/felix/.jenkins/jobs/pass/workspace";
		String outputPath = "/home/felix/.jenkins/jobs/fail/workspace";
		String fileOutput = "/home/felix/tmp.diff";
		Differencer computeDiff = new Differencer();
		computeDiff.extractDiffJavaFile(inputPath, outputPath, fileOutput);
		FindFailingSeed computeSeed = new FindFailingSeed();
		FindPassingSeed ComputePassingSeed = new FindPassingSeed();
		int lineNumber = computeSeed.computeSeed(failedLogFile).getLineNumber();
		String seedClass = computeSeed.computeSeed(failedLogFile)
				.getSeedClass();
		String failingSeed = seedClass + ":" + lineNumber;
		String passingSeed = ComputePassingSeed.computeSeed(failingSeed,
				fileOutput);
		System.out.println("failingSeed: " + failingSeed);
		System.out.println("passingSeed: " + passingSeed);

		Slicing helper = new Slicing(classPath, mainClass, exclusionFile);
		helper.CallGraphBuilder();
		helper.setExclusionFile(exclusionFile);
		helper.setDataDependenceOptions(DataDependenceOptions.NONE);
		helper.setControlDependenceOptions(ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
		helper.setContextSensitive(true); // context-insensitive

		Statement s = helper.extractStatementfromException(seedClass,
				lineNumber);
		int seedlineNum = WALAUtils.getStatementLineNumber(s);
		System.out.println("Seed line number is: " + seedlineNum);
		Collection<Statement> slice = null;
		System.out.println("--- backward ---");
		slice = helper.computeSlice(s);
		WALAUtils.dumpSliceToFile(slice, fileName);
		int sliceSize = helper.outputSlice(s).statements.size();
		System.out.println("Total number of statemnets in the slice: "
				+ sliceSize);

		Intersection intersection = new Intersection();
		List<String> matching = intersection.matchingSet(fileOutput);
		Collection<Statement> chop = intersection.intersection(matching, slice);
		System.out.println(chop.size());
	}
}

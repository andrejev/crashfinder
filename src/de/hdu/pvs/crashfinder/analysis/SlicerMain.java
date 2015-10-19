package de.hdu.pvs.crashfinder.analysis;

import java.io.IOException;
import java.util.Collection;

import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

import de.hdu.pvs.crashfinder.util.WALAUtils;
import de.hdu.pvs.crashfinder.analysis.FindSeed;

public class SlicerMain {


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
		FindSeed computeSeed= new FindSeed();
		int lineNumber = computeSeed.computeSeed(failedLogFile).getLineNumber();
		String seedClass = computeSeed.computeSeed(failedLogFile).getSeedClass();
		System.out.println(seedClass+":"+lineNumber);


		Slicing helper = new Slicing(classPath, mainClass, exclusionFile);
		helper.CallGraphBuilder();
		helper.setExclusionFile(exclusionFile);
		helper.setDataDependenceOptions(DataDependenceOptions.NONE);
		helper.setControlDependenceOptions(ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
		helper.setContextSensitive(true); // context-insensitive

		Statement s = helper.extractStatementfromException(seedClass, lineNumber);
		Collection<Statement> slice = null;
		System.out.println("--- backward ---");
		slice = helper.computeSlice(s);
		WALAUtils.dumpSliceToFile(slice, fileName);
		int sliceSize = helper.outputSlice(s).statements.size();
		System.out.println("Total number of statemnets in the slice: " + sliceSize);
	}
}

package de.hdu.pvs.crashfinder.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

import de.hdu.pvs.crashfinder.util.WALAUtils;

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

		String classPath = "../../hadoop/Hdfs3856/2/share/hadoop/hdfs/hadoop-hdfs-3.0.0-SNAPSHOT.jar";
		String fileName = "dumpslice.txt";
		String mainClass = "Lorg/apache/hadoop/hdfs/server/namenode/NameNode";
		String exclusionFile = "dat/JavaAllExclusions.txt";
		
		Slicing helper = new Slicing(classPath, mainClass, exclusionFile);
		helper.CallGraphBuilder();
		helper.setExclusionFile(exclusionFile);
		helper.setDataDependenceOptions(DataDependenceOptions.NONE);
		helper.setControlDependenceOptions(ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
		helper.setContextSensitive(true); // context-insensitive

		Statement s = helper
				.extractStatementfromException(
						"org.apache.hadoop.hdfs.server.namenode.FSNamesystem",
						759);
		Collection<Statement> slice = null;
		System.out.println("--- backward ---");
		slice = helper.computeSlice(s);
		WALAUtils.dumpSliceToFile(slice, fileName);
		int sliceSize = helper.outputSlice(s).statements.size();
		System.out.println("Total number of statemnets in the slice: " + sliceSize);
	}
}

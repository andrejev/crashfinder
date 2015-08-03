package de.hdu.pvs.crashfinder.experiments;

//import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
//import java.util.List;
//import java.util.Set;

import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.CancelException;

import de.hdu.pvs.crashfinder.analysis.IRStatement;
import de.hdu.pvs.crashfinder.analysis.Slicing;
import de.hdu.pvs.crashfinder.analysis.SlicingOutput;
import de.hdu.pvs.crashfinder.util.Log;


public class CommonUtils {

	private static Statement seed;

	public static Collection<SlicingOutput> getSlicingOutputs(String path,
			String mainClass, String exclusionFile) throws IllegalArgumentException, CancelException {
		return getSlicingOutputs(path, mainClass, exclusionFile, DataDependenceOptions.NO_BASE_NO_HEAP_NO_EXCEPTIONS,
				ControlDependenceOptions.NONE);
	}

	public static Collection<SlicingOutput> getSlicingOutputs(String path,
			String mainClass, String exclusionFile,	DataDependenceOptions dataDep, ControlDependenceOptions controlDep) throws IllegalArgumentException, CancelException {
		Slicing helper = new Slicing(path, mainClass, exclusionFile);
		// helper.setCGType(type);
		helper.setExclusionFile(exclusionFile);
		helper.setDataDependenceOptions(dataDep);
		helper.setControlDependenceOptions(controlDep);
		helper.setContextSensitive(false); // context-insensitive
		helper.CallGraphBuilder();

		Collection<SlicingOutput> outputs = new LinkedList<SlicingOutput>();
		SlicingOutput output = helper.outputSlice(seed);

		outputs.add(output);

		System.out.println("  statement in slice: " + output.statements.size());
		Log.logln("  statement in slice: " + output.statements.size());

		/*
		 * Set<IRStatement> branchStmts =
		 * SlicingOutput.extractBranchStatements(set);
		 * System.out.println("  branching statements: " + branchStmts.size());
		 * Log.logln("  branching statements: " + branchStmts.size());
		 * 
		 * dumpStatements(branchStmts);
		 */

		// save the slicer
		/*
		 * for (SlicingOutput output : outputs) {
		 * output.setConfigurationSlicer(helper); }
		 */

		return outputs;
	}

	public static void dumpStatements(Collection<IRStatement> stmts) {
		for (IRStatement stmt : stmts) {
			Log.logln("     >> " + stmt.toString());
		}
	}

}
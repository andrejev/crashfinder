package de.hdu.pvs.crashfinder.instrument;


	import java.util.Collection;

	import com.ibm.wala.ipa.slicer.Statement;
	import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
	import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;

import de.hdu.pvs.crashfinder.analysis.FindSeed;
import de.hdu.pvs.crashfinder.analysis.Slicing;
import de.hdu.pvs.crashfinder.analysis.SlicingOutput;
import de.hdu.pvs.crashfinder.util.WALAUtils;

import junit.framework.TestCase;

	public class TestNoChange extends TestCase {
		public void testSimpleInstrumenter() throws Exception {

			String dir = "/home/felix/hadoop/Hdfs3856/2/share/hadoop/hdfs/";
			String classPath = dir + "hadoop-hdfs-3.0.0-SNAPSHOT.jar";
			String fileName = "dumpslice.txt";

			String mainClass = "Lorg/apache/hadoop/hdfs/server/namenode/NameNode";
			String exclusionFile = "src/resources/JavaAllExclusions.txt";
			String failedLogFile = "src/resources/stackTraceFail.log";

			
			Slicing helper = new Slicing(classPath, mainClass, exclusionFile);
			helper.CallGraphBuilder();
			helper.setDataDependenceOptions(DataDependenceOptions.NO_BASE_NO_HEAP);
			helper.setControlDependenceOptions(ControlDependenceOptions.NO_EXCEPTIONAL_EDGES);
			helper.setContextSensitive(true); // context-insensitive

			FindSeed computeSeed= new FindSeed();
			int lineNumber = computeSeed.computeSeed(failedLogFile).getLineNumber();
			String seedClass = computeSeed.computeSeed(failedLogFile).getSeedClass();

			Statement s = helper.extractStatementfromException(seedClass, lineNumber);

			Collection<Statement> slice = null;
			System.out.println("--- backward ---");
			slice = helper.computeSlice(s);
			WALAUtils.dumpSliceToFile(slice, fileName);

			SlicingOutput output1 = helper.outputSlice(s);
			// System.out.println(output1);

			// InstrumenterBench instrumenter = new InstrumenterBench();
			// EveryStmtInstrumenter instrumenter = new EveryStmtInstrumenter();
			RelatedStmtInstrumenter instrumenter = new RelatedStmtInstrumenter(
					output1);
			instrumenter.instrument(classPath, dir
					+ "hadoop-mapreduce-client-jobclient-3.0.0-SNAPSHOT-tests-instrumented.jar");
			InstrumentStats.showInstrumentationStats();

		}
	}
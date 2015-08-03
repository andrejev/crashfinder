package de.hdu.pvs.crashfinder.instrument;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

//import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;

public abstract class AbstractInstrumenter {

	protected boolean disasm = false;
	protected final boolean verify = true;
	protected OfflineInstrumenter instrumenter;

	public static String PRE = "evaluating";
	public static String POST = "entering";
	public static String SEP = "#";

	// this is for separating context with other relevant information
	// such as line number, source text, etc.
	// see TraceAnalyzer for usage example

	public static String SUB_SEP = "%%"; //not used for context separation
	public static String INDEX_SEP = "_index_";

	public void instrument(String inputElement, String outputJar)
			throws Exception {
		System.out.println("start instrumentating");
		instrumenter = new OfflineInstrumenter();
		Writer w = new BufferedWriter(new FileWriter("report", false));
		instrumenter.addInputElement(inputElement);
		instrumenter.setOutputJar(new File(outputJar));
		instrumenter.setPassUnmodifiedClasses(true);
		instrumenter.beginTraversal();
		ClassInstrumenter ci;

		// do the instrumentation
		while ((ci = instrumenter.nextClass()) != null) {
			try {
				doClass(ci, w);
			} catch (Throwable e) {
				e.printStackTrace();
				continue;
			}
		}
		instrumenter.close();
	}

	public void setDisasm(boolean disasm) {
		this.disasm = disasm;
	}

	protected abstract void doClass(final ClassInstrumenter ci, Writer w)
			throws Exception;

}

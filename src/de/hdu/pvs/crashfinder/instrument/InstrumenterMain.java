package de.hdu.pvs.crashfinder.instrument;

import java.io.IOException;
//import java.util.Collection;
import java.util.Properties;

//import analysis.IRStatement;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.io.CommandLine;

import de.hdu.pvs.crashfinder.analysis.SlicingOutput;

/**
 * This class simply performs instrumentation
 * */
public class InstrumenterMain {

	public static boolean help = false;

	public static String original_jar = null;

	public static String instrumented_jar = null;

	protected Instrumenter instrumenter;

	private final SlicingOutput output = null;

	public static void main(String[] args) throws Exception {
		run(args);
		new InstrumenterMain().instrument();
	}

	public static void run(String[] args) throws WalaException,
			IllegalArgumentException, CancelException, IOException {

		// parse the command-line into a Properties object
		Properties p = CommandLine.parse(args);
		p.getProperty("original_jar");
		p.getProperty("instrumented_jar");

		// validate that the command-line has the expected format
		validateCommandLine(p);
	}

	private void instrument() throws Exception {
		instrumenter = new Instrumenter(output);
		instrumenter.instrument(original_jar, instrumented_jar);
		InstrumentStats.showInstrumentationStats();
	}

	static void validateCommandLine(Properties p) {
		if (p.get("original_jar") == null) {
			throw new UnsupportedOperationException(
					"expected command-line to include -original_jar");
		}
		if (p.get("instrumented_jar") == null) {
			throw new UnsupportedOperationException(
					"expected command-line to include -instrumented_jar");
		}
	}
}

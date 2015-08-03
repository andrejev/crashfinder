package de.hdu.pvs.crashfinder.diagnosis;

import java.io.IOException;
import java.util.Collection;

import java.util.Properties;

import de.hdu.pvs.crashfinder.analysis.SlicingOutput;
import de.hdu.pvs.crashfinder.experiments.CommonUtils;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.io.CommandLine;


public class OutputMain {
	
	//public static boolean help = false;

	//public static String stacktrace_file = null;
	
	//public static boolean cache_slice = false;
	
	//public static String diagnose_result_file = "./diagnosis_results.txt";
	
	public static String source_dir = null;
	
	public static String classpath_for_slicing = null;
	
	public static String main_for_slicing = null;
		
	public static String ingorable_class_file = "JavaAllExclusions.txt";
	
	public static void main(String[] args) throws Exception {
		run(args);
		new OutputMain().report();
	}

	public static void run(String[] args) throws WalaException,
			IllegalArgumentException, CancelException, IOException {

		// parse the command-line into a Properties object
		Properties p = CommandLine.parse(args);
		p.getProperty("source_dir");
		p.getProperty("classpath_for_slicing");
		p.getProperty("main_for_slicing");

		// validate that the command-line has the expected format
		validateCommandLine(p);
	}
	
	static void validateCommandLine(Properties p) {
		if (p.get("classpath_for_slicing") == null) {
			throw new UnsupportedOperationException(
					"expected command-line to include -classpath_for_slicing");
		}
		if (p.get("main_for_slicing") == null) {
			throw new UnsupportedOperationException(
					"expected command-line to include -main_for_slicing");
		}
		if (p.get("source_dir") == null) {
			throw new UnsupportedOperationException(
					"expected command-line to include -source_dir");
		}
	}
	
	private void report() throws Exception {
		//decide to perform slicing or not
		Collection<SlicingOutput> Slices = null;
		if(classpath_for_slicing != null && main_for_slicing != null) {
			Slices = CommonUtils.getSlicingOutputs(
					classpath_for_slicing,
					main_for_slicing,
					ingorable_class_file);
		}
	}
	
	}
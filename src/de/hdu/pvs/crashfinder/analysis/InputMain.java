package de.hdu.pvs.crashfinder.analysis;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.io.CommandLine;

//import java.io.FileNotFoundException;
//import java.util.Collection;
//import java.util.LinkedList;
//import java.util.List;

import de.hdu.pvs.crashfinder.diagnosis.OutputMain;
import de.hdu.pvs.crashfinder.experiments.CommonUtils;
//import edu.washington.cs.conf.diagnosis.MainAnalyzer;
//import analysis.Slicing;
//import instrument.InstrumentSchema;
//import instrument.InstrumentSchema.TYPE;
//import util.Utils;
/**
 * This class simply performs instrumentation
 * */
public class InputMain {
	
	public static void main(String[] args) throws Exception {
		run(args);
		new InputMain().analysis();
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

	private void analysis() throws Exception {
		//perform thin slicing, and then create the instrumentation schema
    	Collection<SlicingOutput> Slices = CommonUtils.getSlicingOutputs(
    			OutputMain.classpath_for_slicing,
    			OutputMain.main_for_slicing,
    			OutputMain.ingorable_class_file);
	}
    	/*//create the schema
    	InstrumentSchema schema = new InstrumentSchema();
		schema.setType(TYPE.SOURCE_PREDICATE);
		schema.addInstrumentationPoint(confSlices);*/
		
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


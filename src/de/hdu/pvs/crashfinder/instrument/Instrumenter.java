package de.hdu.pvs.crashfinder.instrument;

import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;


import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.Disassembler;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.Instruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MethodEditor;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.ThrowInstruction;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.MethodEditor.Output;
import com.ibm.wala.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassWriter;

import de.hdu.pvs.crashfinder.analysis.IRStatement;
import de.hdu.pvs.crashfinder.analysis.SlicingOutput;
import de.hdu.pvs.crashfinder.util.Globals;
import de.hdu.pvs.crashfinder.util.Utils;
import de.hdu.pvs.crashfinder.util.WALAUtils;


public class Instrumenter extends AbstractInstrumenter {

	protected boolean branch = true;
	protected boolean entry = false;
	protected boolean exit = false;
	protected boolean exception = false;
	private Set<String> skippedClasses = null;

	private Set<String> instrumentedClasses = null;

	private final Collection<SlicingOutput> slices;

	// All affected instruction methods and index
	private final Map<String, Set<Integer>> mapIndices;

	// protected boolean reduce_instr_point = false;

	static final String fieldName = "_instrument_enable_trace";

	static final Instruction getTracer = Util.makeGet(MethodTracer.class,
			"tracer");
	static final Instruction callTrace = Util.makeInvoke(MethodTracer.class,
			"trace", new Class[] { String.class });

	static final Instruction pushEntry = Util.makeInvoke(MethodTracer.class,
			"pushEntry", new Class[] { String.class });
	static final Instruction popExit = Util.makeInvoke(MethodTracer.class,
			"popExit", new Class[] { String.class });
	static final Instruction popExcepExit = Util.makeInvoke(MethodTracer.class,
			"popExceptionExit", new Class[] { String.class });

	/*
	 * public void setReduceInstrPoint(boolean reduce) { this.reduce_instr_point
	 * = reduce; if (this.reduce_instr_point) {
	 * System.err.println("Reduce the # of instrumentation points," +
	 * " must need further postprocessing."); } }
	 */

	public void turnOnContextInstrumentation() {
		this.entry = true;
		this.exit = true;
		this.exception = true;
	}

	public Instrumenter(SlicingOutput output) {
		slices = new LinkedList<SlicingOutput>();
		slices.add(output);
		// create the map for index
		mapIndices = new LinkedHashMap<String, Set<Integer>>();
		// Initialize the map
		this.initInstrumentMap(slices, mapIndices);
	}

	private void initInstrumentMap(Collection<SlicingOutput> slices,
			Map<String, Set<Integer>> mapIndices) {
		Utils.checkNotNull(mapIndices);
		for (SlicingOutput slice : slices) {
			for (IRStatement irs : slice.statements) {
				String methodSig = irs.getMethodSig();
				Integer index = irs.getInstructionIndex();
				if (!mapIndices.containsKey(methodSig)) {
					mapIndices.put(methodSig, new HashSet<Integer>());
				}
				mapIndices.get(methodSig).add(index);
			}
		}
	}
	
	// full name
	public void setSkippedClasses(Collection<String> classes) {
		skippedClasses = new LinkedHashSet<String>();
		skippedClasses.addAll(classes);
	}

	public void setInstrumentedClassPrefix(Collection<String> classes) {
		instrumentedClasses = new LinkedHashSet<String>();
		instrumentedClasses.addAll(classes);
	}

	@Override
	protected void doClass(final ClassInstrumenter ci, Writer w)
			throws Exception {
		final String className = ci.getReader().getName();
		w.write("Class: " + className + "\n");
		w.flush();

		for (int m = 0; m < ci.getReader().getMethodCount(); m++) {
			MethodData d = ci.visitMethod(m);
			// d could be null, e.g., if the method is abstract or native
			if (d != null) {
				w.write("Instrumenting " + ci.getReader().getMethodName(m)
						+ " " + ci.getReader().getMethodType(m) + ":\n");
				w.flush();
				if (disasm) {
					w.write("Initial ShrikeBT code:\n");
					(new Disassembler(d)).disassembleTo(w);
					w.flush();
				}
				if (verify) {
					Verifier v = new Verifier(d);
					v.verify();
				}

				MethodEditor me = new MethodEditor(d);
				me.beginPass();

				final String methodSig = WALAUtils.getMethodSignature(d);

				// profiling the predicates
				if (branch) {
					int length = me.getInstructions().length;
					for (int i = 0; i < length; i++) {
						IInstruction inst = me.getInstructions()[i];
						String instStr = inst.toString();
						if (this.mapIndices.containsKey(methodSig)) {
							System.out
									.println("inst: " + inst + " @ "
											+ methodSig
											+ this.mapIndices.get(i).size());

							// do instrumentation
							if (this.mapIndices.get(methodSig).contains(i)) {
								// methodSig is not a uniquely-identifiable, so
								// plus the instruction index
								final String msg = methodSig + SEP + instStr
										+ SEP + INDEX_SEP + i;
								me.insertBefore(i, new MethodEditor.Patch() {
									@Override
									public void emitTo(MethodEditor.Output w) {
										w.emit(getTracer);
										w.emit(ConstantInstruction
												.makeString(msg));
										w.emit(callTrace);
										InstrumentStats
												.addInsertedInstructions(1);
									}
								});
								me.insertAfter(i, new MethodEditor.Patch() {
									@Override
									public void emitTo(MethodEditor.Output w) {
										w.emit(getTracer);
										w.emit(ConstantInstruction
												.makeString(msg));
										w.emit(callTrace);
										InstrumentStats
												.addInsertedInstructions(1);
									}
								});
							}
						}

					}
				}

				// insert Tracer.pushEntry() to method entry points
				// insert Tracer.popExit() to both method return, and exception
				// handling points
				if (entry) {
					me.insertAtStart(new MethodEditor.Patch() {
						@Override
						public void emitTo(MethodEditor.Output w) {
							w.emit(getTracer);
							w.emit(ConstantInstruction.makeString(methodSig));
							w.emit(pushEntry);
							InstrumentStats.addInsertedInstructions(1);
						}
					});
				}
				if (exit) {
					IInstruction[] instr = me.getInstructions();
					for (int i = 0; i < instr.length; i++) {
						if (instr[i] instanceof ReturnInstruction) {
							me.insertBefore(i, new MethodEditor.Patch() {
								@Override
								public void emitTo(MethodEditor.Output w) {
									w.emit(getTracer);
									w.emit(ConstantInstruction
											.makeString(methodSig));
									w.emit(popExit);
									InstrumentStats.addInsertedInstructions(1);
								}
							});
						}
					}
				}
				if (exception) {
					me.addMethodExceptionHandler(null,
							new MethodEditor.Patch() {
								@Override
								public void emitTo(Output w) {
									w.emit(getTracer);
									w.emit(ConstantInstruction
											.makeString(methodSig));
									w.emit(popExcepExit);
									w.emit(ThrowInstruction.make(false));
									InstrumentStats.addInsertedInstructions(1);
								}
							});
				}

				// this updates the data d
				me.applyPatches();
				if (disasm) {
					w.write("Final ShrikeBT code:\n");
					(new Disassembler(d)).disassembleTo(w);
					w.write(Globals.lineSep);
					w.flush();
				}
			}
		}

		if (ci.isChanged()) {
			ClassWriter cw = ci.emitClass();
			cw.addField(ClassReader.ACC_PUBLIC | ClassReader.ACC_STATIC,
					fieldName, Constants.TYPE_boolean,
					new ClassWriter.Element[0]);
			instrumenter.outputModifiedClass(ci, cw);
		}
	}
}

package de.hdu.pvs.crashfinder.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;


//import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ipa.slicer.thin.CISlicer;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;

import de.hdu.pvs.crashfinder.util.Utils;
import de.hdu.pvs.crashfinder.util.WALAUtils;

/**
 * 
 * This file computes a slice and also maps the result to source code.
 * 
 * For creating a launcher, See the 'PDFSlice' launcher included in the
 * 'launchers' directory in WALA.
 * 
 * @see Slicer, PDFSlicer and SlicerTest from WALA
 * @author Mohammad Ghanavati date: 05.05.2013
 */
public class Slicing {

	public enum CG {
		RTA, ZeroCFA, ZeroContainerCFA, VanillaZeroOneCFA, ZeroOneCFA, ZeroOneContainerCFA, OneCFA, TwoCFA, CFA, TempZeroCFA
	}

	public final String classPath;
	public final String mainClass;
	private String exclusionFile;
	private boolean contextSensitive = true;
	private DataDependenceOptions dataOption = DataDependenceOptions.NO_BASE_NO_HEAP;
	private ControlDependenceOptions controlOption = ControlDependenceOptions.NONE;
	private CISlicer slicer = null;
	private AnalysisScope scope = null;
	private ClassHierarchy cha = null;
	private Iterable<Entrypoint> entrypoints = null;
	// private CallGraphBuilder builder = null;
	private AnalysisOptions options = null;
	private CallGraphBuilder cgb = null;
	private CallGraph cg = null;
	private PointerAnalysis pa = null;

	// private Statement s;

	// private String targetPackageName = null;

	public Slicing(String classPath, String mainClass, String exclusionFile) {
		this.classPath = classPath;
		this.mainClass = mainClass;
		this.exclusionFile = exclusionFile;
	}

	public void CallGraphBuilder() {
		try {
			// create an analysis scope representing the appJar as a J2SE
			// application
			this.scope = AnalysisScopeReader
					.makeJavaBinaryAnalysisScope(this.classPath,
							(new FileProvider()).getFile(this.exclusionFile));

			// build the class hierarchy
			this.cha = ClassHierarchy.make(scope);
			// this.entrypoints = com.ibm.wala.ipa.callgraph.impl.Util
			// .makeMainEntrypoints(scope, cha, mainClass);
			this.entrypoints = new AllApplicationEntrypoints(scope, cha);
			//this.options = CallGraphTestUtil.makeAnalysisOptions(scope,
			//		entrypoints);
			this.options =  new AnalysisOptions(scope, entrypoints);

			// build the call graph
			System.out.println("Building call graph...");
			System.out.println("Number of entry points: "
					+ Utils.countIterable(this.entrypoints));
			this.cgb = Util.makeZeroCFABuilder(options, new AnalysisCache(),
					cha, scope);
			this.cg = cgb.makeCallGraph(options, null);
			this.pa = cgb.getPointerAnalysis();
			System.err.println(CallGraphStats.getStats(this.cg));
		} catch (Throwable e) {
			throw new Error(e);
		}
	}

	public void setExclusionFile(String fileName) {
		this.exclusionFile = fileName;
	}

	public void setContextSensitive(boolean cs) {
		this.contextSensitive = cs;
	}

	public void setDataDependenceOptions(DataDependenceOptions op) {
		this.dataOption = op;
	}

	public void setControlDependenceOptions(ControlDependenceOptions op) {
		this.controlOption = op;
	}

	public ClassHierarchy getClassHierarchy() {
		return this.cha;
	}

	public CallGraph getCallGraph() {
		return this.cg;
	}

	public PointerAnalysis getPointerAnalysis() {
		return this.pa;
	}

	public SlicingOutput outputSlice(Statement seed)
			throws IllegalArgumentException, CancelException {
		long startT = System.currentTimeMillis();
		Collection<Statement> stmts = computeSlice(seed);
		System.out.println("Time cost for computation of backward slicing: "
				+ (System.currentTimeMillis() - startT) / 1000 + " s");
		Collection<IRStatement> irs = convert(stmts);
		SlicingOutput output = new SlicingOutput(irs);
		return output;
	}

	public Collection<Statement> computeSlice(Statement seed)
			throws IllegalArgumentException, CancelException {
		checkCG();
		// Statement s = this.seed;
		System.err.println("Statement: " + seed);

		// compute the slice as a collection of statements
		try {
			if (this.contextSensitive) {
				return computeContextSensitiveBackwardSlice(seed);
			} else {
				return this.computeContextInsensitiveBackwardThinSlice(seed);
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public Collection<Statement> computeContextSensitiveBackwardSlice(
			Statement seed) throws IllegalArgumentException, CancelException {
		return computeConetxtSensitiveSlice(seed, this.cg, this.pa,
				this.dataOption, this.controlOption);
	}

	public Collection<Statement> computeConetxtSensitiveSlice(Statement seed,
			CallGraph cg, PointerAnalysis pa, DataDependenceOptions dOptions,
			ControlDependenceOptions cOptions) throws IllegalArgumentException,
			CancelException {

		checkCG();
		System.err.println("Seed statement in context-sensitive slicing: "
				+ seed);
		System.err.println("Data dependence option: " + dOptions);
		System.err.println("Control dependence option: " + cOptions);

		// compute the slice as a collection of statements
		Collection<Statement> slice = null;
		slice = Slicer.computeBackwardSlice(seed, cg, pa, dOptions, cOptions);
		return slice;
	}

	public Collection<Statement> computeContextInsensitiveBackwardThinSlice(
			Statement seed) throws IllegalArgumentException, CancelException {
		return computeConetxtInsensitiveThinSlice(seed, this.cg, this.pa,
				this.dataOption, this.controlOption);
	}

	public Collection<Statement> computeConetxtInsensitiveThinSlice(
			Statement seed, CallGraph cg, PointerAnalysis pa,
			DataDependenceOptions dOptions, ControlDependenceOptions cOptions)
			throws IllegalArgumentException, CancelException {
		checkCG();
		System.err.println("Seed statement in context-insensitive slicing: "
				+ seed);
		System.err.println("Data dependence option: " + dOptions);
		System.err.println("Control dependence option: " + cOptions);

		// initialize the slice
		if (slicer == null) {
			slicer = new CISlicer(cg, pa, dOptions, cOptions);
		}

		Collection<Statement> slice = null;
		slice = slicer.computeBackwardThinSlice(seed);
		return slice;
	}

	private void checkCG() {
		if (this.cg == null) {
			throw new RuntimeException("Please call buildAnalysis() first.");
		}
	}

	public static void dumpSlice(Collection<Statement> slice) {
		dumpSlice(slice, new PrintWriter(System.err));
	}

	public static void dumpSlice(Collection<Statement> slice, PrintWriter w) {
		w.println("SLICE:\n");
		int i = 1;
		for (Statement s : slice) {
			String line = (i++) + "   " + s;
			w.println(line);
			w.flush();
		}
	}

	// dumping slice to a file
	public static void dumpSliceToFile(Collection<Statement> slice,
			String fileName) throws FileNotFoundException {
		File f = new File(fileName);
		FileOutputStream fo = new FileOutputStream(f);
		PrintWriter w = new PrintWriter(fo);
		dumpSlice(slice, w);
	}

	// finding the main method
	public static CGNode findMainMethod(CallGraph cg) {
		Descriptor d = Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V");
		Atom name = Atom.findOrCreateUnicodeAtom("main");
		return findMethod(cg, d, name);
	}

	/**
	 * @param cg
	 * @param d
	 * @param name
	 * @return
	 */
	// finding the method
	private static CGNode findMethod(CallGraph cg, Descriptor d, Atom name) {
		for (Iterator<? extends CGNode> it = cg.getSuccNodes(cg
				.getFakeRootNode()); it.hasNext();) {
			CGNode n = it.next();
			if (n.getMethod().getName().equals(name)
					&& n.getMethod().getDescriptor().equals(d)) {
				return n;
			}
		}
		// if it's not a successor of fake root, just iterate over everything
		for (CGNode n : cg) {
			if (n.getMethod().getName().equals(name)
					&& n.getMethod().getDescriptor().equals(d)) {
				return n;
			}
		}
		Assertions.UNREACHABLE("failed to find method " + name);
		return null;
	}

	public static CGNode findMethod(CallGraph cg, String name) {
		Atom a = Atom.findOrCreateUnicodeAtom(name);
		for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
			CGNode n = it.next();
			if (n.getMethod().getName().equals(a)) {
				return n;
			}
		}
		System.err.println("call graph " + cg);
		Assertions.UNREACHABLE("failed to find method " + name);
		return null;
	}

	public static Statement findCallTo(CGNode n, String methodName) {
		IR ir = n.getIR();
		for (Iterator<SSAInstruction> it = ir.iterateAllInstructions(); it
				.hasNext();) {
			SSAInstruction s = it.next();
			if (s instanceof SSAInvokeInstruction) {
				SSAInvokeInstruction call = (SSAInvokeInstruction) s;
				if (call.getCallSite().getDeclaredTarget().getName().toString()
						.equals(methodName)) {
					IntSet indices = ir
							.getCallInstructionIndices(((SSAInvokeInstruction) s)
									.getCallSite());
					Assertions.productionAssertion(indices.size() == 1,
							"expected 1 but got " + indices.size());
					return new NormalStatement(n, indices.intIterator().next());
				}
			}
		}
		Assertions.UNREACHABLE("failed to find call to " + methodName + " in "
				+ n);
		return null;
	}

	public Statement extractStatementfromException(String className, int lineNum)
			throws InvalidClassFileException {

		for (CGNode node : cg) {
			String fullClassName = WALAUtils.getJavaFullClassName(node
					.getMethod().getDeclaringClass());
			if (fullClassName.equals(className)) {
				Iterator<SSAInstruction> ssaIt = node.getIR()
						.iterateAllInstructions();
				while (ssaIt.hasNext()) {
					SSAInstruction inst = ssaIt.next();
					int i = WALAUtils.getInstructionIndex(node, inst);
					IBytecodeMethod method = (IBytecodeMethod) node.getIR()
							.getMethod();
					if (i == -1)
						continue;
					int bytecodeIndex = method.getBytecodeIndex(i);
					int sourceLineNum = method.getLineNumber(bytecodeIndex);
					if (sourceLineNum == lineNum) {
						// find it
						Statement s = new NormalStatement(node, i);
						return s;
					}
				}
			}
		}

		return null;
	}

	public static Collection<IRStatement> convert(Collection<Statement> stmts) {
		Collection<IRStatement> irs = new LinkedList<IRStatement>();

		for (Statement s : stmts) {
			if (s instanceof StatementWithInstructionIndex) {
				if (s.getNode().getMethod() instanceof ShrikeBTMethod) {
					try {
						IRStatement ir = new IRStatement(
								(StatementWithInstructionIndex) s);
						irs.add(ir);
					} catch (Throwable e) {
						// System.err.println("Error in IR: " + s);
						continue;
					}
				} else {
					// skip fake method
					// Log.logln("skip stmt: " + s + " in method: " +
					// s.getNode().getClass());
				}
			} else {
				// Log.logln("skip non-StatementWithInstructionIndex: " + s);
			}
		}
		return irs;
	}
}
package de.hdu.pvs.crashfinder.analysis;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;

import de.hdu.pvs.crashfinder.util.Globals;
import de.hdu.pvs.crashfinder.util.Utils;
import de.hdu.pvs.crashfinder.util.WALAUtils;

//import edu.washington.cs.conf.diagnosis.InvariantUtils;

public class SlicingOutput implements Serializable {
	private static final long serialVersionUID = 2670541082578233016L;
		
	public final boolean ignoreLibs = true;      
	
	//set is not serializable
	public final Set<IRStatement> statements;
	
	//not used in equals and compare
	public final Map<IRStatement, Integer> stmtDistances = new LinkedHashMap<IRStatement, Integer>();
	
	private Slicing slicer = null;
	
	public SlicingOutput(Collection<IRStatement> stmts) {
		this(stmts, null);
	}
	
	public SlicingOutput(Collection<IRStatement> stmts,
			String targetPackage) {
		this.statements = new LinkedHashSet<IRStatement>();
		if(targetPackage != null) {
			for(IRStatement s : stmts) {
				String fullMethod = WALAUtils.getFullMethodName(s.getStatement().getNode().getMethod());
				if(fullMethod.startsWith(targetPackage)) {
					this.statements.add(s);
				}
			}
		} else {
			this.statements.addAll(stmts);
		}
	}
	
	/*public int getSlicingDistance(IRStatement irs) {
		Utils.checkTrue(stmtDistances.containsKey(irs));
		return stmtDistances.get(irs);
	}
	*/
	public void setSlicing(Slicing slicer) {
		Utils.checkNotNull(slicer);
		this.slicer = slicer;
	}
	
	public Slicing getSlicing() {
		return this.slicer;
	}
	
	//the full name is in the form of: packagename.classname.methodname
	//the signature is in the form of: chord.project.Config.check(Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)
	public boolean includeStatement(String fullMethodName, int sourceLineNum) {
		for(IRStatement irs : this.statements) {
			String methodSig = irs.methodSig;
			int lineNum = irs.lineNumber;
			if(methodSig.startsWith(fullMethodName) && lineNum == sourceLineNum) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containStatement(String methodSig, int instructionIndex) {
		return this.getStatement(methodSig, instructionIndex) != null;
	}
	
	public boolean containStatement(String uniqueSig) {
		for(IRStatement irs : this.statements) {
			if(irs.getUniqueSignature().equals(uniqueSig)) {
				return true;
			}
		}
		return false;
	}
	
	public IRStatement getStatement(String methodSig, int instructionIndex) {
		for(IRStatement irs : this.statements) {
			if(irs.getInstructionIndex() == instructionIndex && irs.getMethodSig().equals(methodSig)) {
				return irs;
			}
		}
		return null;
	}
	
	public boolean findStatementByMethod(String methodSig) {
		for(IRStatement irs : this.statements) {
			if(irs.getMethodSig().equals(methodSig)) {
				return true;
			}
		}
		return false;
	}
	
	public Set<IRStatement> getNumberedStatements() {
		Set<IRStatement> numbered = new LinkedHashSet<IRStatement>();
		for(IRStatement ir : statements) {
			if(ir.hasLineNumber()) {
				numbered.add(ir);
			}
		}
		return numbered;
	}
	
	public Set<IRStatement> getNumberedBranches() {
		Set<IRStatement> branches = new LinkedHashSet<IRStatement>();
		for(IRStatement ir : statements) {
			if(ir.isBranch() && ir.hasLineNumber()) {
				branches.add(ir);
			}
		}
		return branches;
	}
	
	public Set<IRStatement> getNumberedBranchesInSource() {
		Set<IRStatement> branches = new LinkedHashSet<IRStatement>();
		for(IRStatement ir : statements) {
			if(ir.hasLineNumber() && ir.isBranchInSource()) {
				branches.add(ir);
			}
		}
		return branches;
	}
	
	public Set<ShrikePoint> getNumberedBranchShrikePoints() {
		return this.getShrikePoints(this.getNumberedBranches());
	}
	
	public Set<ShrikePoint> getNumberedBranchShrikePointsInSource() {
		//return this.getShrikePoints(this.getNumberedBranchesInSource());
		Set<IRStatement> irs = new LinkedHashSet<IRStatement>();
		
		for(IRStatement ir : this.statements) {
			if(!ir.hasLineNumber()) {
				continue;
			}
			if(ir.isBranch()) {
				irs.add(ir);
			} else {
				List<IRStatement> mappedIRs = this.getMappedBranchInSource(ir);
				if(!mappedIRs.isEmpty()) {
				    irs.addAll(mappedIRs);
				}
			}
		}
		Set<ShrikePoint> retPoints = this.getShrikePoints(irs);
		
		return retPoints;
	}
	
	/**
	 * A method designed specifically for speeding up processing full slicing results,
	 * since the slice is so large that it is hard to fit into memory
	 * */
	public static Set<IRStatement> filterStatementsForFullSliceResult(Collection<IRStatement> set) {
		Set<IRStatement> ret = new LinkedHashSet<IRStatement>();
		Set<String> existed = new LinkedHashSet<String>();
		
		for(IRStatement s : set) {
			if(s.shouldIgnore() || !s.isBranch()) {
				continue;
			}
			String sig = s.getUniqueSignature(); //method name + instruction string + instruction index
			if(existed.contains(sig)) {
				continue;
			}
			existed.add(sig);
			ret.add(s);
		}
		
		//reclaim memory
		existed.clear();
		
		return ret;
	}
	
	public static Set<IRStatement> excludeIgnorableStatements(Collection<IRStatement> set) {
		Set<IRStatement> filteredSet = new LinkedHashSet<IRStatement>();
		for(IRStatement s : set) {
			if(!s.shouldIgnore()) {
				filteredSet.add(s);
			}
		}
		return filteredSet;
	}
	
	public static Set<IRStatement> extractBranchStatements(Collection<IRStatement> set) {
		Set<IRStatement> branchSet = new LinkedHashSet<IRStatement>();
		for(IRStatement s : set) {
			if(s.isBranch()) {
				branchSet.add(s);
			}
		}
		return branchSet;
	}
	
	private List<IRStatement> getMappedBranchInSource(IRStatement ir) {
		CGNode node = ir.getStatement().getNode();
		int lineNum = ir.getLineNumber();
		Utils.checkTrue(lineNum != -1);
		List<IRStatement> matchedStmts = new LinkedList<IRStatement>();
		for(int index = 0; index < node.getIR().getInstructions().length; index++) {
			SSAInstruction ssa = node.getIR().getInstructions()[index];
			if(ssa instanceof SSAConditionalBranchInstruction) {
				NormalStatement stmt = new NormalStatement(node, index);
				int sNum = WALAUtils.getStatementLineNumber(stmt);
				if(sNum == lineNum) {
					IRStatement irs = new IRStatement(stmt);
					matchedStmts.add(irs);
				}
			}
		}
		
		return matchedStmts;
		
		//FIXME prune out the StringBuilder, example in RelMclsValAsgnInst, visitMoveInst
		
//		if(matchedStmts.size() > 1) {
//			System.err.println("In node: " + node);
//			System.err.println("ir: " + ir);
//			System.err.println("matched stmts: " + matchedStmts);
//			throw new Error("ir: " + ir + ", matched stmts: " + matchedStmts);
//		}
//		
//		if(matchedStmts.isEmpty()) {
//			return null;
//		} else {
//			return matchedStmts.get(0);
//		}
	}
	
	//has a corresponding line number mapping to the source code
	public Set<ShrikePoint> getNumberedShrikePoints() {
		return this.getShrikePoints(this.getNumberedStatements());
	}
	
	public Set<ShrikePoint> getAllShrikePoints() {
		return this.getShrikePoints(this.statements);
	}
	
	private Set<ShrikePoint> getShrikePoints(Collection<IRStatement> stmts) {
		Set<ShrikePoint> pts = new LinkedHashSet<ShrikePoint>();
		for(IRStatement stmt : stmts) {
			if(ignoreLibs) {
				if(stmt.shouldIgnore()) {
				    continue;
				}
			}
			pts.add(new ShrikePoint(stmt));
		}
		return pts;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(Globals.lineSep);
		sb.append(Globals.lineSep);
		sb.append("Total number of statements in the slice: " + this.statements.size());
		sb.append(Globals.lineSep);
		for(IRStatement s : statements) {
		    sb.append(s.toString());
		    sb.append(Globals.lineSep);
		}
		
		return sb.toString();
	}
}

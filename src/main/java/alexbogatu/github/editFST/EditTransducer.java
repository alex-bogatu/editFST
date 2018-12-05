package alexbogatu.github.editFST;

import java.util.List;

import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.MutableSymbolTable;
import com.github.steveash.jopenfst.io.Convert;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;

/**
 * Defines the edit FST (a.k.a. flower transducer).
 * Given an alphabet, it constructs an WFST for three operations: insert, delete, update.
 * If the alphabet is large, the flower transducer can become very large, e.g., 9215 transitions for the 95 ASCII characters alphabet. 
 * The solution is to factor the transducer into two components - note that this only works for the edit transducer (not for transducers describing other similarity distances).
 * See http://www.openfst.org/twiki/bin/view/FST/FstExamples for examples.
 * 
 * @author alex
 *
 */

public class EditTransducer {
	
	public final static String LEFTSTATE = "left";
	public final static String RIGHTSTATE = "right";
	
	public final static String INS = "<ins>";
	public final static String DEL = "<del>";
	public final static String SUB = "<sub>";
	
	
	private List<String> alphabet;
	private MutableFst leftFactor;
	private MutableFst rightFactor;
	private MutableSymbolTable symbolTable;
	
	private Double subweight = 1.0;
	private Double insweight = 1.0;
	private Double delweight = 1.0;
	private Double idweight = 0.0;
	
	/**
	 * Constructor - creates a new edit transducer given an alphabet.
	 * @param alphabet - the alphabet as a list of strings
	 * @param symbolTable - the in/out symbol table Edit(the same symbol tables is being used for input and for output)
	 */
	public EditTransducer(List<String> alphabet, MutableSymbolTable symbolTable) {
		this.alphabet = alphabet;
		this.symbolTable = symbolTable;
		this.leftFactor = this.generateLeftFactor();
		this.rightFactor = this.generateRightFactor();
	}
	
	
	/**
	 * Constructor - creates a new edit transducer given an alphabet and operations weights.
	 * @param alphabet - the alphabet as a list of strings
	 * @param symbolTable - the in/out symbol table (the same symbol tables is being used for input and for output)
	 * @param idw - identity weight (should be 0.0)
	 * @param subw - substitution weight (defaults to 1.0)
	 * @param insw - insertion weight (defaults to 1.0)
	 * @param delw - deletion weight (defaults to 1.0)
	 */
	
	public EditTransducer(List<String> alphabet, MutableSymbolTable symbolTable, Double idw, Double subw, Double insw, Double delw) {
		this.alphabet = alphabet;
		this.symbolTable = symbolTable;
		
		this.idweight = idw;
		this.subweight = subw;
		this.insweight = insw;
		this.delweight = delw;
		
		this.leftFactor = this.generateLeftFactor();
		this.rightFactor = this.generateRightFactor();
	}
	
//	private MutableFst generateTransducer() {
//		MutableFst transducer = new MutableFst(TropicalSemiring.INSTANCE, this.symbolTable, this.symbolTable);
//		transducer.useStateSymbols();
//		MutableState startState = transducer.newStartState(LEFTSTATE);
//		startState.setFinalWeight(0.0);
//		for (String s1 : this.alphabet) {
//			transducer.addArc(LEFTSTATE, s1, Fst.EPS, LEFTSTATE, 1.0);
//			transducer.addArc(LEFTSTATE, Fst.EPS, s1, LEFTSTATE, 1.0);
//			for (String s2 : this.alphabet) {
//				if (s1.equals(s2)) {
//					transducer.addArc(LEFTSTATE, s1, s2, LEFTSTATE, 0.0);
//				}
//				else {
//					transducer.addArc(LEFTSTATE, s1, s2, LEFTSTATE, 1.0);
//				}
//			}
//		}
//		return transducer;
//	}

	/**
	 * Generate the left factor of the edit transducer starting from the given alphabet
	 * @param alphabet
	 * @return a MutableFst
	 */
	
	private MutableFst generateLeftFactor() {
		MutableFst leftFactor = new MutableFst(TropicalSemiring.INSTANCE, this.symbolTable, this.symbolTable);
		leftFactor.useStateSymbols();
		
		MutableState startState = leftFactor.newStartState(LEFTSTATE);
		startState.setFinalWeight(0.0);
		
		// special transition
		leftFactor.addArc(LEFTSTATE, Fst.EPS, INS, LEFTSTATE, this.insweight/2);
		
		for (String s : this.alphabet) {
			// identity transitions
			leftFactor.addArc(LEFTSTATE, s, s, LEFTSTATE, this.idweight);
			
			// substitution transitions
			leftFactor.addArc(LEFTSTATE, s, SUB, LEFTSTATE, this.subweight/2);
			
			// deletion transitions
			leftFactor.addArc(LEFTSTATE, s, DEL, LEFTSTATE, this.delweight/2);
		}
		
		return leftFactor;
		
	}

	/**
	 * Generate the right factor of the edit transducer starting from the given alphabet
	 * @param alphabet
	 * @return a MutableFst
	 */
	
	private MutableFst generateRightFactor() {
		MutableFst rightFactor = new MutableFst(TropicalSemiring.INSTANCE, this.symbolTable, this.symbolTable);
		rightFactor.useStateSymbols();
		
		MutableState startState = rightFactor.newStartState(RIGHTSTATE);
		startState.setFinalWeight(0.0);
		
		// special transition
		rightFactor.addArc(RIGHTSTATE, DEL, Fst.EPS, RIGHTSTATE, this.delweight/2);
		
		for (String s : this.alphabet) {
			// identity transitions
			rightFactor.addArc(RIGHTSTATE, s, s, RIGHTSTATE, this.idweight);
			
			// substitution transitions
			rightFactor.addArc(RIGHTSTATE, SUB, s, RIGHTSTATE, this.subweight/2);
			
			// deletion transitions
			rightFactor.addArc(RIGHTSTATE, INS, s, RIGHTSTATE, this.insweight/2);
		}
		
		return rightFactor;
		
	}
	
	/**
	 * @return the alphabet
	 */
	public List<String> getAlphabet() {
		return this.alphabet;
	}

	/**
	 * @param alphabet the alphabet to set
	 */
	public void setAlphabet(List<String> alphabet) {
		this.alphabet = alphabet;
	}

	/**
	 * @return the leftFactor
	 */
	public MutableFst getLeftFactor() {
		return leftFactor;
	}

	/**
	 * @param leftFactor the leftFactor to set
	 */
	public void setLeftFactor(MutableFst leftFactor) {
		this.leftFactor = leftFactor;
	}

	/**
	 * @return the rightFactor
	 */
	public MutableFst getRightFactor() {
		return rightFactor;
	}

	/**
	 * @param rightFactor the rightFactor to set
	 */
	public void setRightFactor(MutableFst rightFactor) {
		this.rightFactor = rightFactor;
	}
	
	/**
	 * Exports the edit transducer to an openfst text format
	 * @param location - the path and basename of the resulting serialized objects - there will be two objects: basename.left and basename.right
	 */
	
	public void exportEditTransducer(String basename) {
		Convert.export(this.leftFactor, basename + ".left");
		Convert.export(this.rightFactor, basename + ".right");
	}
	

}

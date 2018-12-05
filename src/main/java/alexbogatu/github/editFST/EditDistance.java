package alexbogatu.github.editFST;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import com.github.steveash.jopenfst.Arc;
import com.github.steveash.jopenfst.Fst;
import com.github.steveash.jopenfst.MutableArc;
import com.github.steveash.jopenfst.MutableFst;
import com.github.steveash.jopenfst.MutableState;
import com.github.steveash.jopenfst.MutableSymbolTable;
import com.github.steveash.jopenfst.State;
import com.github.steveash.jopenfst.operations.ArcSort;
import com.github.steveash.jopenfst.operations.Compose;
import com.github.steveash.jopenfst.operations.ILabelCompare;
import com.github.steveash.jopenfst.operations.NShortestPaths;
import com.github.steveash.jopenfst.operations.RemoveEpsilon;
import com.github.steveash.jopenfst.operations.Reverse;
import com.github.steveash.jopenfst.semiring.TropicalSemiring;

/**
 * Computes the edit distance between two given strings using the edit transducer rather than dynamic programming. 
 * 
 * @author alex
 *
 */

public class EditDistance {
	
	private List<String> alphabet;
	private MutableSymbolTable symbolTable;
	private EditTransducer et;
	
	/**
	 * Creates an EditDistance object given an alphabet (or the ASCII alphabet) and its associated symbol table
	 * which contains the characters the strings to be compared consist of.
	 * With the alphabet, the corresponding edit transducer is being created.
	 * A new alphabet requires a new object - once the alphabet is created it cannot be changed.
	 */
	
	public EditDistance() {
		this.symbolTable = new MutableSymbolTable();
		List<String> charlist = new ArrayList<String>();
		for (int c=32; c<128; c++) {
			String stringRepr = Character.toString((char)c);
			charlist.add(stringRepr);
			this.symbolTable.put(stringRepr, c);
		}
		
		this.symbolTable.put(Fst.EPS, 0);
		this.symbolTable.put(EditTransducer.INS, 11);
		this.symbolTable.put(EditTransducer.DEL, 12);
		this.symbolTable.put(EditTransducer.SUB, 13);
		
		this.alphabet = Collections.unmodifiableList(charlist);
		this.et = new EditTransducer(this.alphabet, symbolTable);
	}
	
	public EditDistance(List<String> alphabet, MutableSymbolTable symbolTable) {
		this.alphabet = Collections.unmodifiableList(alphabet);
		this.symbolTable = symbolTable;
		
		this.symbolTable.put(Fst.EPS, 0);
		this.symbolTable.put(EditTransducer.INS, 11);
		this.symbolTable.put(EditTransducer.DEL, 12);
		this.symbolTable.put(EditTransducer.SUB, 13);
		
		this.et = new EditTransducer(this.alphabet, symbolTable);
	}
	
	/**
	 * Create a finite state transducer (as an acceptor) from the given string.
	 * @param inputString
	 */
	
	private MutableFst createAcceptor(String inputString) {
		MutableFst acceptor = new MutableFst(TropicalSemiring.INSTANCE, this.symbolTable, this.symbolTable);
		acceptor.useStateSymbols();
		
		
		List<String> states = new ArrayList<String>();
		for (char c : inputString.toCharArray()) {
			states.add(Character.toString(c));
		}
		MutableState startState = acceptor.newStartState("0");
		for (int i=0; i<states.size(); i++) {
			acceptor.addArc(Integer.toString(i), states.get(i), states.get(i), Integer.toString(i+1), 0.0);
		}
		
		acceptor.getState(states.size()).setFinalWeight(0.0);
		return acceptor;
	}
	
	/**
	 * Removes redundant transitions (between the same two states) from an FST - this might be replaceable with minimization.
	 * There is no theoretical guarantee for the soundness of this method
	 * @param redundantFst
	 * @return a new FST without redundant transitions
	 */
	
	private MutableFst removeRedundancy(MutableFst redundantFst) {
		MutableFst nonredundantFst = MutableFst.emptyWithCopyOfSymbols(redundantFst);
		
		for (int i = 0; i < redundantFst.getStateCount(); i++) {
		      State source = redundantFst.getState(i);
		      MutableState target = new MutableState(source.getArcCount());
		      target.setFinalWeight(source.getFinalWeight());
		      nonredundantFst.setState(i, target);
		}
		
		for (int i = 0; i < redundantFst.getStateCount(); i++) {
		      State source = redundantFst.getState(i);
		      
		      // Whn in equals out the weight should be 0.0 - identity tranformation
		      for (Arc arc : source.getArcs()) {
		    	  if (arc.getIlabel() == arc.getOlabel()) {
		    		  ((MutableArc)arc).setWeight(0.0);
		    	  }
		      }
		      
		      MutableState target = nonredundantFst.getState(i);
		      for (Arc sarc : new HashSet<Arc>(source.getArcs())) {
		    	  MutableState nextTargetState = nonredundantFst.getState(sarc.getNextState().getId());
		    	  nonredundantFst.addArc(target, sarc.getIlabel(), sarc.getOlabel(), nextTargetState, sarc.getWeight());
		      }
		    }
		    MutableState newStart = nonredundantFst.getState(redundantFst.getStartState().getId());
		    nonredundantFst.setStart(newStart);
		return nonredundantFst;
		
	}

	/**
	 * @return the alphabet
	 */
	public List<String> getAlphabet() {
		return alphabet;
	}

	/**
	 * @return the edit transducer
	 */
	public EditTransducer getEditTransducer() {
		return et;
	}
	
	/**
	 * Compares two strings using the edit distance.
	 * @param firstString
	 * @param secondString
	 * @return the edit (Levenstein) distance between the two strings.
	 */
	
	public float compare(String firstString, String secondString) {
		
		MutableFst firstAcceptor = this.createAcceptor(firstString);
		MutableFst secondAcceptor = this.createAcceptor(secondString);
		
		MutableFst firstComposed = Compose.compose(firstAcceptor, this.getEditTransducer().getLeftFactor(), TropicalSemiring.INSTANCE);
		ArcSort.sortBy(firstComposed, new ILabelCompare());
		MutableFst secondComposed = Compose.compose(this.getEditTransducer().getRightFactor(), secondAcceptor, TropicalSemiring.INSTANCE);
		ArcSort.sortBy(secondComposed, new ILabelCompare());
		
		MutableFst composedResult = Compose.compose(firstComposed, secondComposed, TropicalSemiring.INSTANCE);
		
		MutableFst sp = this.removeRedundancy(RemoveEpsilon.remove(Reverse.reverse(NShortestPaths.apply(composedResult, 1))));
		
		float distance = 0;
		
		for (int i=0; i<sp.getStateCount(); i++) {
			for (Arc a : sp.getState(i).getArcs()) {
				distance += a.getWeight();
			}
		}
		
		return 1.0f * distance/Math.max(firstString.length(), secondString.length());
		
	}

}

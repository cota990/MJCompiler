package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;

//import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;

public class ControlStructureCodeGenerator extends VisitorAdaptor {
	
	//public static int MatchedIf = 0, UnmatchedIf = 1, UnmatchedElse = 2, 
			//MatchedWhile = 3, UnmatchedWhile = 4, TernaryOp = 5;
	
	//Logger log = Logger.getLogger (getClass());
	
	private int suspend = 0;
	
	private int numOfFactors = 1;
	
	private List<Integer> exprStartAddress = new ArrayList<Integer> ();
	
	/** list of adresses which should jump out of statement
	 */
	private List<Integer> controlStructuresExprJumpFixupAddresses = new ArrayList<Integer> ();
	
	/** list of address fixups for conditional expressions (all false jumps put)
	 */
	private List<Integer> conditionalFactorsJumpAddresses = new ArrayList<Integer> ();
	
	/** list of address fixups for conditional factors (jump if true)
	 */
	private List<Integer> numOfFactorsInTerms = new ArrayList<Integer> ();
	
	/**
	 * @return the numOfFactorsInTerms
	 */
	public List<Integer> getNumOfFactorsInTerms() {
		return numOfFactorsInTerms;
	}
	
	/**
	 * @return the exprStartAddress
	 */
	public List<Integer> getExprStartAddress() {
		return exprStartAddress;
	}
	
	/**
	 * @return the conditionalFactorsJumpAddresses
	 */
	public List<Integer> getConditionalFactorsJumpAddresses() {
		return conditionalFactorsJumpAddresses;
	}
	
	/**
	 * @return the controlStructuresExprJumpFixupAddresses
	 */
	public List<Integer> getControlStructuresExprJumpFixupAddresses() {
		return controlStructuresExprJumpFixupAddresses;
	}
	
	/**
	 * expression code generator
	 */

	/**SingleExprFact; generate expression
	 * <br> load 1 and put not_equal jump
	 */
	public void visit(SingleExprFactor sef) {
		
		//log.info("SingleExprFactor");
		
		if (suspend == 0) {
			
			//log.info("SingleExprFactor not suspended");
		
			exprStartAddress.add(Code.pc);
			
			ExpressionCodeGenerator factorCodeGenerator = new ExpressionCodeGenerator();
			sef.getNonCondExpr().traverseBottomUp(factorCodeGenerator);
			
			// put jump
			Code.loadConst(1);
			Code.putFalseJump(Code.eq, 0);
			conditionalFactorsJumpAddresses.add(Code.pc - 2);
		
		}
		
		//log.info("SingleExprFactor done");
		
	}
	
	/** First expression of relational operator
	 */
	public void visit(FirstExpr fe) {
		
		if (suspend == 0) {
		
			exprStartAddress.add(Code.pc);
			
			ExpressionCodeGenerator factorCodeGenerator = new ExpressionCodeGenerator();
			fe.getNonCondExpr().traverseBottomUp(factorCodeGenerator);
		
		}
		
	}
	
	/** Second expression of relational operator
	 */
	public void visit(SecondExpr se) {
		
		if (suspend == 0) {
		
			ExpressionCodeGenerator factorCodeGenerator = new ExpressionCodeGenerator();
			se.getNonCondExpr().traverseBottomUp(factorCodeGenerator);
		
		}
		
	}
	
	/**
	 * conditional factor
	 */
	
	/**MultipleExprFact;
	 * <br> both expressions loaded; put jump based on relational operator
	 */
	public void visit(MultipleExprFactor mef) {
		
		if (suspend == 0) {
		
			if (mef.getRelop() instanceof Equals)
				Code.putFalseJump(Code.eq, 0);
			
			else if (mef.getRelop() instanceof NotEquals)
				Code.putFalseJump(Code.ne, 0);
			
			else if (mef.getRelop() instanceof GreaterThan)
				Code.putFalseJump(Code.gt, 0);
			
			else if (mef.getRelop() instanceof GreaterThanEquals)
				Code.putFalseJump(Code.ge, 0);
			
			else if (mef.getRelop() instanceof LessThan)
				Code.putFalseJump(Code.lt, 0);
			
			else if (mef.getRelop() instanceof LessThanEquals)
				Code.putFalseJump(Code.le, 0);
			
			conditionalFactorsJumpAddresses.add(Code.pc - 2);
		
		}
		
	}
	
	/**
	 * conditional term
	 */
	
	public void visit(And a) {
		
		if (suspend == 0)
			numOfFactors++;
		
	}
	
	public void visit(Or o) {
		
		if (suspend == 0) {
		
			numOfFactorsInTerms.add(numOfFactors);
		
			numOfFactors = 1;
		
		}
		
	}
	
	public void visit (CompositeFactorCondExpr cfce) {
		
		suspend--;
		
	}
	
	public void visit (MethodCallFactor mcf) {
		
		suspend--;
		
	}
	
	public void visit (CompositeFactorNonCondExpr cfnce) {
		
		suspend--;
		
	}
	
	public void visit (NewArrayFactorCondExpr nafce) {
		
		suspend--;
		
	}
	
	public void visit (ArrayDesignatorCondExpr adce) {
		
		suspend--;
		
	}
	
	public void visit (NewArrayFactorNonCondExpr nafnce) {
		
		suspend--;
		
	}
	
	public void visit (ArrayDesignatorNonCondExpr adnce) {
		
		suspend--;
		
	}
	
	public void visit(LeftParenthesis lp) {
		
		SyntaxNode parent = lp.getParent();
		
		if (parent instanceof CompositeFactorCondExpr
				|| parent instanceof MethodCallFactor
				|| parent instanceof CompositeFactorNonCondExpr )
			suspend++;
		
	}
	
	public void visit (LeftBracket lb) {
		
		SyntaxNode parent = lb.getParent();
		
		if (parent instanceof NewArrayFactorCondExpr
				|| parent instanceof NewArrayFactorNonCondExpr
				|| parent instanceof ArrayDesignatorNonCondExpr
				|| parent instanceof ArrayDesignatorCondExpr )
			suspend++;
		
	}
	
	public void fixupAdresses () {
		
		if (exprStartAddress.size() 
				== conditionalFactorsJumpAddresses.size()) {
			
			//log.info("conditionalFactorsJumpAddresses.size: " + conditionalFactorsJumpAddresses.size());
			//for (Integer adr : conditionalFactorsJumpAddresses)
				//log.info(adr);
			
			//log.info("numOfFactorsInTerms.size: " + numOfFactorsInTerms.size());
			//for (Integer adr : numOfFactorsInTerms)
				//log.info(adr);
			
			//log.info("exprStartAddress.size: " + exprStartAddress.size());
			//for (Integer adr : exprStartAddress)
				//log.info(adr);
						
			int condTermIndex = 0;
			int newAdrIndex = numOfFactorsInTerms.isEmpty() 
								? 0
								: numOfFactorsInTerms.get(condTermIndex);
			
			//log.info("newAdrIndex: " + newAdrIndex);
			
			int i;
			
			for (i = 0; i < conditionalFactorsJumpAddresses.size() - 1; i++) {
				
				//log.info("i: " + i);
				
				if (condTermIndex == numOfFactorsInTerms.size() ) {
					 
					//last term factors; each of these should jump after statement if false
					
					//log.info("last term factors");
					
					controlStructuresExprJumpFixupAddresses.add(conditionalFactorsJumpAddresses.get(i));
					
				}
				
				else {
					
					// jump if true to statement body; change operation in buffer and jump to curr pc; collect next index
					if (i + 1 == newAdrIndex) {
						
						//log.info("last factor in term");
						
						int operationAddress = conditionalFactorsJumpAddresses.get(i) - 1;
						
						switch (Code.buf [operationAddress]) {
						
							case 43: Code.buf [operationAddress] = 44; break;
							case 44: Code.buf [operationAddress] = 43; break;
							case 45: Code.buf [operationAddress] = 48; break;
							case 46: Code.buf [operationAddress] = 47; break;
							case 47: Code.buf [operationAddress] = 46; break;
							case 48: Code.buf [operationAddress] = 45; break;
							default: break;
						
						}
						
						Code.fixup(conditionalFactorsJumpAddresses.get(i));
						
						condTermIndex++;
						
						if (condTermIndex != numOfFactorsInTerms.size()) {
							newAdrIndex += numOfFactorsInTerms.get(condTermIndex);
							//log.info("newAdrIndex: " + newAdrIndex);
						}
						
					}
					
					// jump to next term
					else {
						
						//log.info("jump to next term");
						
						Code.put2(conditionalFactorsJumpAddresses.get(i),
								exprStartAddress.get(newAdrIndex) 
									- conditionalFactorsJumpAddresses.get(i) + 1);
						
					}
					
				}
				
			}
			
			controlStructuresExprJumpFixupAddresses.add(conditionalFactorsJumpAddresses.get(i));
			
		}
		
		//else
			//log.info("GRESKA!");
		
	}

}

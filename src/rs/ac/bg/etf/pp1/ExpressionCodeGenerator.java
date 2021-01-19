package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;

public class ExpressionCodeGenerator extends VisitorAdaptor {
	
	private int suspend = 0;
	
	/**
	 * factors, terms and expressions
	 */
	
	public void visit(MinusTermExpr mte) {
		
		if (suspend == 0)
			Code.put(Code.neg);
		
	}
	
	public void visit(MultipleTermExpr mte) {
		
		if (suspend == 0) {
		
			if (mte.getAddop() instanceof Add)
				Code.put(Code.add);
			
			else if (mte.getAddop() instanceof Sub)
				Code.put(Code.sub);
		
		}
		
	}
	
	public void visit(MultipleFactorTerm mft) {
		
		if (suspend == 0) {
		
			if (mft.getMulop() instanceof Mul)
				Code.put(Code.mul);
			
			else if (mft.getMulop() instanceof Div)
				Code.put(Code.div);
			
			else if (mft.getMulop() instanceof Mod)
				Code.put(Code.rem);
		
		}
		
	}
	
	/** Factor designator;
	 * <br> load designator, as it's value is used in expression calculation, not for storing
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.FactorDesignator)
	 */
	public void visit(FactorDesignator fd) {
		
		if (suspend == 0)
			Code.load (fd.getDesignator().obj);
		
	}
	
	/** Method call factor
	 * <br> load actual parameters
	 * <br> check if predefined method, put CALL
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MethodCallStatement)
	 */
	public void visit(MethodCallFactor mcf) {
		
		suspend--;
		
		if (suspend == 0) {
		
			if (!(mcf.getMethodDesignator().obj.getName().equals("ord")
					|| mcf.getMethodDesignator().obj.getName().equals("chr")
					|| mcf.getMethodDesignator().obj.getName().equals("len") )) {
			
				int destAdr = mcf.getMethodDesignator().obj.getAdr() - Code.pc;
				Code.put(Code.call);
				Code.put2(destAdr);
				
			}
			
			else {
				
				if (mcf.getMethodDesignator().obj.getName().equals("len"))
					Code.put (Code.arraylength);
				
			}
		
		}
	
	}
	
	/** New array factor statement (non cond expr)
	 * <br> expression already loaded, put NEWARRAY
	 * <br> if array is char array, load 0, else load 1
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.NewArrayFactor)
	 */
	public void visit(NewArrayFactorNonCondExpr naf) {
		
		suspend--;
		
		if (suspend == 0) {
		
			Code.put(Code.newarray);
			
			if (naf.struct.getElemType() == MyTabImpl.charType)
				Code.put(0);
			
			else
				Code.put(1);
		
		}
		
		
	}
	
	/** Constant factor
	 * <br> load value to expression stack
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ConstFactor)
	 */
	public void visit(ConstFactor cf) {
		
		if (suspend == 0)
			Code.load(cf.getConstValue().obj);
		
	}
	
	/**
	 *  designators
	 */
	
	public void visit(SimpleDesignator sd) {
		
		//Code.load(sd.obj);
		
	}
	
	public void visit(ArrayDesignatorNonCondExpr ad) {
		
		suspend--;
		
		if (suspend == 0) {
		
			Code.load(ad.getDesignator().obj);
			Code.put(Code.dup_x1);Code.put(Code.pop);
		
		}
		
	}
	
	/**
	 * conditional expressions starters
	 */
	
	public void visit (LeftParenthesis lp) {
		
		SyntaxNode parent = lp.getParent();
		
		if (suspend == 0) {
		
			if (parent instanceof CompositeFactorCondExpr) {
				
				suspend++;
				
				CompositeFactorCondExpr cfce = (CompositeFactorCondExpr) parent;
				
				ConditionalExpressionCodeGenerator conditionalExpressionGenerator = new ConditionalExpressionCodeGenerator();
				cfce.getCondExpr().traverseBottomUp(conditionalExpressionGenerator);
				
			}
			
			else if (parent instanceof MethodCallFactor) {
				
				suspend++;
				
				MethodCallFactor mcf = (MethodCallFactor) parent;
				
				ActualParameterCodeGenerator actualParametersGenerator = new ActualParameterCodeGenerator();
				mcf.getActParsOption().traverseBottomUp(actualParametersGenerator);
				
			}
			
			else if (parent instanceof CompositeFactorNonCondExpr) {
				
				suspend++;
				
				CompositeFactorNonCondExpr cfnce = (CompositeFactorNonCondExpr) parent;
				
				ExpressionCodeGenerator expressionGenerator = new ExpressionCodeGenerator();
				cfnce.getNonCondExpr().traverseBottomUp(expressionGenerator);
				
			}
		
		}
		
		else {
			
			if (parent instanceof CompositeFactorCondExpr
					|| parent instanceof MethodCallFactor
					|| parent instanceof CompositeFactorNonCondExpr )
				suspend++;
			
		}
			
		
	}
	
	public void visit (LeftBracket lb) {
		
		SyntaxNode parent = lb.getParent();
		
		if (suspend == 0) {
			
			if (parent instanceof NewArrayFactorCondExpr) {
				
				suspend++;
				
				NewArrayFactorCondExpr nafce = (NewArrayFactorCondExpr) parent;
				
				ConditionalExpressionCodeGenerator conditionalExpressionGenerator = new ConditionalExpressionCodeGenerator();
				nafce.getCondExpr().traverseBottomUp(conditionalExpressionGenerator);
				
			}
			
			else if (parent instanceof ArrayDesignatorCondExpr) {
				
				suspend++;
				
				ArrayDesignatorCondExpr adce = (ArrayDesignatorCondExpr) parent;
				
				ConditionalExpressionCodeGenerator conditionalExpressionGenerator = new ConditionalExpressionCodeGenerator();
				adce.getCondExpr().traverseBottomUp(conditionalExpressionGenerator);
				
				
			}
			
			else if (parent instanceof NewArrayFactorNonCondExpr) {
				
				suspend++;
				
				NewArrayFactorNonCondExpr nafnce = (NewArrayFactorNonCondExpr) parent;
				
				ExpressionCodeGenerator expressionGenerator = new ExpressionCodeGenerator();
				nafnce.getNonCondExpr().traverseBottomUp(expressionGenerator);
				
			}
			
			else if (parent instanceof ArrayDesignatorNonCondExpr) {
				
				suspend++;
				
				ArrayDesignatorNonCondExpr adnce = (ArrayDesignatorNonCondExpr) parent;
				
				ExpressionCodeGenerator expressionGenerator = new ExpressionCodeGenerator();
				adnce.getNonCondExpr().traverseBottomUp(expressionGenerator);
				
			}
			
		}
		
		else {
			
			if (parent instanceof NewArrayFactorCondExpr
					|| parent instanceof NewArrayFactorNonCondExpr
					|| parent instanceof ArrayDesignatorNonCondExpr
					|| parent instanceof ArrayDesignatorCondExpr )
				suspend++;
		}
		
	}
	
	/**
	 * conditional expressions enders
	 */
	
	public void visit (CompositeFactorCondExpr cfce) {
		
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
		
		if (suspend == 0) {
			
			Code.load(adce.getDesignator().obj);
			Code.put(Code.dup_x1);Code.put(Code.pop);
		
		}
		
	}

}

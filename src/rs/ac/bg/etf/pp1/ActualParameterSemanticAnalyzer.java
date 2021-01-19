package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.Obj;

public class ActualParameterSemanticAnalyzer extends VisitorAdaptor {
	
	private Obj calledMethod;
	
	private int actParsCount;
	
	private boolean countMode;
	
	private int numToIgnore;
	
	private ReportHelper reporter;

	public int getActParsCount() {
		
		return actParsCount;
	
	}

	public ActualParameterSemanticAnalyzer(Obj calledMethod, boolean countMode, ReportHelper reporter) {
		
		this.calledMethod = calledMethod;
		this.countMode = countMode;
		this.numToIgnore = 0;
		this.actParsCount = 0;
		this.reporter = reporter;
	
	}
	
	/**
	 * actual parameters
	 */
	
	/**Single actual parameter;
	 * <br> context check: ActPar = Expr
	 * <br> Expr must assignable to corresponding formal parameter (find in calledMethod locals)
	 * <br> Expr is already processed, check if != null
	 * <br> if there are more actual parameters than formal parameters, throw error
	 */
	public void visit(SingleActPar sap) {
		
		if (numToIgnore == 0) {
			
			if (actParsCount >= calledMethod.getLevel()
					|| countMode)
				actParsCount++;
			
			else if (sap.getExpr().struct != MyTabImpl.noType) {
				
				// search calledMethod for corresponding formal parameter
				Obj formPar = null;
				
				for (Obj objFound : calledMethod.getLocalSymbols()) {
					
					if (objFound.getFpPos() == actParsCount) {
						
						formPar = objFound;
						break;
						
					}
					
				}
				
				if (formPar != null
						&& !sap.getExpr().struct.assignableTo(formPar.getType()))
					reporter.reportSemanticError("Actual parameter on position " + actParsCount + " does not match formal parameter type", sap.getExpr());
					
				actParsCount++;
				
			}
		
		}
		else
			numToIgnore--;
		
	}
	
	/** MethodDesignator;
	 * <br> count how many parameters this call has, and ignore them
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MethodDesignator)
	 */
	public void visit(MethodDesignator md) {
		
		SyntaxNode parent = md.getParent(); //method call statement or factor
		ActualParameterSemanticAnalyzer actParsCounter = new ActualParameterSemanticAnalyzer(md.obj, true, reporter);
		
		if (parent instanceof MethodCallFactor)
			((MethodCallFactor) parent).getActParsOption().traverseBottomUp(actParsCounter);
		
		else if (parent instanceof MethodCallStatement)
			((MethodCallStatement) parent).getActParsOption().traverseBottomUp(actParsCounter);
		
		numToIgnore = actParsCounter.getActParsCount();
		
	}

}

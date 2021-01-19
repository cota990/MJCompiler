package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;

public class ActualParameterCodeGenerator extends VisitorAdaptor {
	
	private int actParsStarted = 0;
	
	/** For each actual parameter start new generator
	 */
	public void visit(SingleActPar sap) {
		
		if (actParsStarted == 0) {
		
			if (sap.getExpr() instanceof NoConditionalOperatorExpr) {
				
				ExpressionCodeGenerator parameterGenerator = new ExpressionCodeGenerator();
				
				sap.getExpr().traverseBottomUp(parameterGenerator);
				
			}
			
			else if (sap.getExpr() instanceof ConditionalOperatorExpr) {
				
				ConditionalExpressionCodeGenerator parameterGenerator = new ConditionalExpressionCodeGenerator();
				
				sap.getExpr().traverseBottomUp(parameterGenerator);
				
			}
		
		}
		
	}
	
	/**Method designator; if gets here, it means argument of this method is another method, so skip generating code until its done
	 */
	public void visit(MethodDesignator md) {
		
		SyntaxNode parent = md.getParent();
			
		if (parent instanceof MethodCallFactor) {
			
			actParsStarted++;
			
		}
		
	}
	
	/** method called, continue with generating
	 */
	public void visit(MethodCallFactor mcf) {
		
		actParsStarted--;
		
	}

}

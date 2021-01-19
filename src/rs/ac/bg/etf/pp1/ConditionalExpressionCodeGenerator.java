package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;
import rs.etf.pp1.mj.runtime.Code;

public class ConditionalExpressionCodeGenerator extends VisitorAdaptor {
	
	public void visit(CondExpr ce) {
		
		ControlStructureCodeGenerator conditionGenerator = new ControlStructureCodeGenerator();
		ce.getCondition().traverseBottomUp(conditionGenerator);
		
		conditionGenerator.fixupAdresses();
		
		if (ce.getFirstCondExpr().getExpr() instanceof NoConditionalOperatorExpr) {
			
			ExpressionCodeGenerator conditionCodeGenerator = new ExpressionCodeGenerator();
			ce.getFirstCondExpr().getExpr().traverseBottomUp(conditionCodeGenerator);
			
		}
		
		else if (ce.getFirstCondExpr().getExpr() instanceof ConditionalOperatorExpr) {
			
			ConditionalExpressionCodeGenerator conditionCodeGenerator = new ConditionalExpressionCodeGenerator();
			ce.getFirstCondExpr().getExpr().traverseBottomUp(conditionCodeGenerator);
			
		}
		
		Code.putJump(0);
		for (Integer address : conditionGenerator.getControlStructuresExprJumpFixupAddresses())
			Code.fixup(address);
		
		int jumpAdr = Code.pc - 2;
		
		if (ce.getSecondCondExpr().getExpr() instanceof NoConditionalOperatorExpr) {
			
			ExpressionCodeGenerator conditionCodeGenerator = new ExpressionCodeGenerator();
			ce.getSecondCondExpr().getExpr().traverseBottomUp(conditionCodeGenerator);
			
		}
		
		else if (ce.getSecondCondExpr().getExpr() instanceof ConditionalOperatorExpr) {
			
			ConditionalExpressionCodeGenerator conditionCodeGenerator = new ConditionalExpressionCodeGenerator();
			ce.getSecondCondExpr().getExpr().traverseBottomUp(conditionCodeGenerator);
			
		}
		
		Code.fixup(jumpAdr);
		
	}

}

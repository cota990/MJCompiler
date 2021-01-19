package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;

//import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {
	
	//Logger log = Logger.getLogger (getClass());
	
	private int mainPC;
	
	private int nVars = 0;
	
	private List<Integer> controlStructuresJumpAdresses = new ArrayList<Integer> ();
	
	private List<Integer> pendingFixup = new ArrayList<Integer> ();
	
	private List<Integer> doWhileStartAddresses = new ArrayList<Integer> ();
	
	private List<List<Integer>> breakFixupAdresses = new ArrayList<List<Integer>> ();

	public int getMainPC() {
		return mainPC;
	}

	public int getnVars() {
		return nVars;
	}
	
	/**
	 * global variables
	 */
	
	public void visit (SingleVarDecl svd) {
		
		nVars++;
		
	}
	
	/**
	 * main and other methods
	 */
	
	public void visit (MethodName mn) {
		
		if (mn.obj.getName().equals("main"))
			mainPC = Code.pc;
		
		mn.obj.setAdr(Code.pc);
		
		Code.put(Code.enter);
		Code.put(mn.obj.getLevel());
		Code.put(mn.obj.getLocalSymbols().size());
	
	}
	
	public void visit (MethodDecl md) {
		
		if (md.getMethodName().obj.getType() == MyTabImpl.noType) {
			
			Code.put(Code.exit); Code.put(Code.return_);
			
		}
		
		else {
			
			Code.put(Code.trap); Code.put(1);
			
		}
		
	}
	
	/**ReturnStatement;
	 * <br> generates code for return from function (also generate value) : EXIT command, RETURN command
	 */
	public void visit (ReturnStatement rs) {
		
		if (rs.getReturnExprOption() instanceof ReturnExpr) {
			
			ReturnExpr re = (ReturnExpr) rs.getReturnExprOption();
			
			if (re.getExpr() instanceof NoConditionalOperatorExpr) {
				
				ExpressionCodeGenerator returnExpressionCodeGenerator = new ExpressionCodeGenerator();
				re.getExpr().traverseBottomUp(returnExpressionCodeGenerator);
				
			}
			
			else if (re.getExpr() instanceof ConditionalOperatorExpr) {
				
				ConditionalExpressionCodeGenerator returnExpressionCodeGenerator = new ConditionalExpressionCodeGenerator();
				re.getExpr().traverseBottomUp(returnExpressionCodeGenerator);
				
			}
			
		}
		
		Code.put(Code.exit); Code.put(Code.return_);
	
	}
	
	/**
	 * statements
	 */
	
	/** Read statement;
	 * <br> process read designator and put it on expr stack;
	 * <br> check designator type:
	 * <br> if designator int: put read operation
	 * <br> if designator char: put bread operation
	 * <br> if designator bool: read char by char, check if "true" or "false"; if none generate error?
	 * <br> store designator
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ReadStatement)
	 */
	public void visit(ReadStatement rs) {
		
		ExpressionCodeGenerator readDesignatorCodeGenerator = new ExpressionCodeGenerator();
		
		rs.getDesignator().traverseBottomUp(readDesignatorCodeGenerator);
		
		//if (rs.getDesignator().obj.getType().getKind() != Struct.Array)
			//Code.put (Code.pop);
		
		if (rs.getDesignator().obj.getType() == MyTabImpl.intType)
			Code.put(Code.read);
		
		else if (rs.getDesignator().obj.getType() == MyTabImpl.charType)
			Code.put(Code.bread);
		
		else if (rs.getDesignator().obj.getType() == MyTabImpl.boolType) {
			
			// read first character; dup value for next check; check if 't'; if not jump to false check
			Code.put(Code.bread);
			Code.put(Code.dup);
			Code.loadConst(116);
			Code.putFalseJump(Code.eq, 0);
			
			int adr1 = Code.pc - 2;
			
			// pop dupped value; read next character; check if 'r'; if not jump to error 
			Code.put(Code.pop);
			Code.put(Code.bread);			
			Code.loadConst(114);
			Code.putFalseJump(Code.eq, 0);
			
			int adr2 = Code.pc - 2;
			
			// read next character; check if 'u'; if not jump to error
			Code.put(Code.bread);			
			Code.loadConst(117);
			Code.putFalseJump(Code.eq, 0);
			
			int adr3 = Code.pc - 2;
			
			// read next character; check if 'e'; if yes load 1 and finish; if not jump to error
			Code.put(Code.bread);			
			Code.loadConst(101);
			Code.putFalseJump(Code.eq, 0);
			
			int adr4 = Code.pc - 2;
			
			Code.loadConst(1);
			Code.putJump(0);
			
			int adr5 = Code.pc - 2;
			
			// fix first jump, and check if read character 'f'; if not jump to error
			Code.fixup(adr1);
			
			Code.loadConst(102);
			Code.putFalseJump(Code.eq, 0);
			
			int adr6 = Code.pc - 2;
			
			// read next character; check if 'a'; if not jump to error
			Code.put(Code.bread);			
			Code.loadConst(97);
			Code.putFalseJump(Code.eq, 0);
			
			int adr7 = Code.pc - 2;
			
			// read next character; check if 'l'; if not jump to error
			Code.put(Code.bread);			
			Code.loadConst(108);
			Code.putFalseJump(Code.eq, 0);
			
			int adr8 = Code.pc - 2;
			
			// read next character; check if 's'; if not jump to error
			Code.put(Code.bread);			
			Code.loadConst(115);
			Code.putFalseJump(Code.eq, 0);
			
			int adr9 = Code.pc - 2;
			
			// read next character; check if 'e'; if not jump to error
			Code.put(Code.bread);			
			Code.loadConst(101);
			Code.putFalseJump(Code.eq, 0);
			
			int adr10 = Code.pc - 2;
			
			Code.loadConst(0);
			Code.putJump(0);
			
			int adr11 = Code.pc - 2;
			
			// generate error
			Code.fixup(adr2); Code.fixup(adr3); Code.fixup(adr4); Code.fixup(adr6); 
			Code.fixup(adr7); Code.fixup(adr8); Code.fixup(adr9); Code.fixup(adr10);
			
			// TODO generate error
			
			// here it's jumped if bool is read
			Code.fixup(adr5);Code.fixup(adr11);
			
		}
		
		Code.store(rs.getDesignator().obj);
		
	}
	
	/**Print statement;
	 * <br> process print expression, and load argument, if needed; expr stack: ...,val,length
	 * <br> check expr type:
	 * <br> if designator int: put print operation
	 * <br> if designator char: put bprint operation
	 * <br> if designator bool: switch places of expression and length; check if expression 1; if yes print 'true', if not print 'false'
	 */
	public void visit(PrintStatement ps) {
		
		if (ps.getExpr() instanceof NoConditionalOperatorExpr) {
			
			ExpressionCodeGenerator printExpressionCodeGenerator = new ExpressionCodeGenerator();
			ps.getExpr().traverseBottomUp(printExpressionCodeGenerator);
		
		}
		
		else if (ps.getExpr() instanceof ConditionalOperatorExpr) {
			
			ConditionalExpressionCodeGenerator printExpressionCodeGenerator = new ConditionalExpressionCodeGenerator();
			ps.getExpr().traverseBottomUp(printExpressionCodeGenerator);
			
		}
		
		if (ps.getPrintOption() instanceof PrintArgument) {
			
			PrintArgument printArg = (PrintArgument) ps.getPrintOption();
			
			Code.loadConst(printArg.getN1());
			
		}
		
		else if (ps.getPrintOption() instanceof NoPrintArgument)
			Code.loadConst(1);
		
		if (ps.getExpr().struct == MyTabImpl.intType)
			Code.put(Code.print);
		
		else if (ps.getExpr().struct == MyTabImpl.charType)
			Code.put(Code.bprint);
		
		else if (ps.getExpr().struct == MyTabImpl.boolType) {
			
			// switch value and length places; and check if 1
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.loadConst(1);
			Code.putFalseJump(Code.eq, 0);
			
			int adr1 = Code.pc - 2;
			
			//if 1, print 'true'; when load 't', switch places with length; jump to finish
			Code.loadConst(116);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.bprint);
			Code.loadConst(114);
			Code.loadConst(1);
			Code.put(Code.bprint);
			Code.loadConst(117);
			Code.loadConst(1);
			Code.put(Code.bprint);
			Code.loadConst(101);
			Code.loadConst(1);
			Code.put(Code.bprint);
			
			Code.putJump(0);
			
			int adr2 = Code.pc - 2;
			
			Code.fixup(adr1);
			
			// print 'false'; when load 'f', switch places with length;			
			Code.loadConst(102);
			Code.put(Code.dup_x1);
			Code.put(Code.pop);
			Code.put(Code.bprint);
			Code.loadConst(97);
			Code.loadConst(1);
			Code.put(Code.bprint);
			Code.loadConst(108);
			Code.loadConst(1);
			Code.put(Code.bprint);
			Code.loadConst(115);
			Code.loadConst(1);
			Code.put(Code.bprint);
			Code.loadConst(101);
			Code.loadConst(1);
			Code.put(Code.bprint);
			
			Code.fixup(adr2);
		
		}
	
	}
	
	/** Assign statement
	 * <br>generate code for destination designator and source expression;
	 * <br>store designator
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.AssignStatement)
	 */
	public void visit(AssignStatement as) {
		
		ExpressionCodeGenerator destinationDesignatorCodeGenerator = new ExpressionCodeGenerator();
		
		as.getDesignator().traverseBottomUp(destinationDesignatorCodeGenerator);
		
		if (as.getSource() instanceof AssignSuccess) {
			
			AssignSuccess source = (AssignSuccess) as.getSource();
			
			if (source.getExpr() instanceof NoConditionalOperatorExpr) {
				
				ExpressionCodeGenerator sourceExpressionCodeGenerator = new ExpressionCodeGenerator();
				as.getSource().traverseBottomUp(sourceExpressionCodeGenerator);
				
			}
			
			else {
				
				ConditionalExpressionCodeGenerator sourceExpressionCodeGenerator = new ConditionalExpressionCodeGenerator();
				as.getSource().traverseBottomUp(sourceExpressionCodeGenerator);
				
			}
		
		}
		
		Code.store(as.getDesignator().obj);
	
	}
	
	/** Increment statement
	 * <br> generate code for destination designator; if designator is array, dup2
	 * <br> load 1, put ADD, store designator
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.IncrementStatement)
	 */
	public void visit(IncrementStatement is) {
		
		ExpressionCodeGenerator destinationDesignatorCodeGenerator = new ExpressionCodeGenerator();
		is.getDesignator().traverseBottomUp(destinationDesignatorCodeGenerator);
		
		if (is.getDesignator().obj.getKind() == Obj.Elem)
			Code.put(Code.dup2);
		
		Code.load(is.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(is.getDesignator().obj);
		
	}
	
	/** Decrement statement
	 * <br> destination designator already loaded
	 * <br> load 1, put SUB, store designator
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.DecrementStatement)
	 */
	public void visit(DecrementStatement ds) {
		
		ExpressionCodeGenerator destinationDesignatorCodeGenerator = new ExpressionCodeGenerator();
		ds.getDesignator().traverseBottomUp(destinationDesignatorCodeGenerator);
		
		if (ds.getDesignator().obj.getKind() == Obj.Elem)
			Code.put(Code.dup2);
		
		Code.load(ds.getDesignator().obj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(ds.getDesignator().obj);
		
	}
	
	/** Method call statement
	 * <br> load actual parameters; check if predefined method, put CALL; if method returns value, pop from stack
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MethodCallStatement)
	 */
	public void visit(MethodCallStatement mcs) {
		
		ActualParameterCodeGenerator actualParametersGenerator = new ActualParameterCodeGenerator();
		mcs.getActParsOption().traverseBottomUp(actualParametersGenerator);
		
		if (!(mcs.getMethodDesignator().obj.getName().equals("ord")
				|| mcs.getMethodDesignator().obj.getName().equals("chr")
				|| mcs.getMethodDesignator().obj.getName().equals("len") )) {
		
			int destAdr = mcs.getMethodDesignator().obj.getAdr() - Code.pc;
			Code.put(Code.call);
			Code.put2(destAdr);
			
		}
		
		else {
			
			if (mcs.getMethodDesignator().obj.getName().equals("len"))
				Code.put (Code.arraylength);
			
		}
		
		if (mcs.getMethodDesignator().obj.getType() != MyTabImpl.noType)
			Code.put(Code.pop);
		
	}
	
	/** If control structure begining;
	 * <br> should start generating condition;
	 * <br> after code is generated, put jump and do fixup
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.IfStart)
	 */
	public void visit (IfStart is) {
		
		SyntaxNode parent = is.getParent();
		
		IfStatementSyntaxCheck issc = null;
		
		if (parent instanceof MatchedIfStatement) {
			
			MatchedIfStatement mis = (MatchedIfStatement) parent;
			
			issc = mis.getIfStatementSyntaxCheck();
			
		}
		
		else if (parent instanceof UnmatchedIfStatement) {
			
			UnmatchedIfStatement uis = (UnmatchedIfStatement) parent;
			
			issc = uis.getIfStatementSyntaxCheck();
			
		}
		
		else if (parent instanceof UnmatchedElseStatement) {
			
			UnmatchedElseStatement ues = (UnmatchedElseStatement) parent;
			
			issc = ues.getIfStatementSyntaxCheck();
			
		}
		
		if (issc != null) {
			
			if (issc instanceof IfStatementSuccess) {
				
				StatementCondition sc = ((IfStatementSuccess) issc).getStatementCondition();
				
				if (sc instanceof ConditionalOperator) {
					
					//log.info("ConditionalOperator");
					
					ConditionalExpressionCodeGenerator conditionCodeGenerator = new ConditionalExpressionCodeGenerator();
					sc.traverseBottomUp(conditionCodeGenerator);
					
					Code.loadConst(1);
					Code.putFalseJump(Code.eq, 0);
					
					controlStructuresJumpAdresses.add(Code.pc - 2);
					
					pendingFixup.add(1);
					
				}
				
				else if (sc instanceof NoConditionalOperator) {
					
					//log.info("NoConditionalOperator");
					
					ControlStructureCodeGenerator conditionCodeGenerator = new ControlStructureCodeGenerator();
					sc.traverseBottomUp(conditionCodeGenerator);
					
					conditionCodeGenerator.fixupAdresses();
					
					//log.info("conditionCodeGenerator.getControlStructuresExprJumpFixupAddresses().size: " + conditionCodeGenerator.getControlStructuresExprJumpFixupAddresses().size());
					
					for (Integer address : conditionCodeGenerator.getControlStructuresExprJumpFixupAddresses())
						controlStructuresJumpAdresses.add(address);
					
					pendingFixup.add(conditionCodeGenerator.getControlStructuresExprJumpFixupAddresses().size());
					
				}
				
			}
			
		}
		
	}
	
	public void visit (UnmatchedIfStatement uis) {
		
		if (!controlStructuresJumpAdresses.isEmpty()
				&& !pendingFixup.isEmpty() ) {
			
			int cnt = pendingFixup.get(pendingFixup.size() - 1);
			
			for (int i = 0; i < cnt; i++) {
			
				int adr = controlStructuresJumpAdresses.remove(controlStructuresJumpAdresses.size() - 1);
				Code.fixup(adr);
			
			}
			
		}
		
		//else
			//log.info("GRESKA");
		
	}
	
	public void visit (ElseStart es) {
		
		Code.putJump(0);
		
		if (!controlStructuresJumpAdresses.isEmpty()
				&& !pendingFixup.isEmpty() ) {
			
			int cnt = pendingFixup.get(pendingFixup.size() - 1);
			
			for (int i = 0; i < cnt; i++) {
			
				int adr = controlStructuresJumpAdresses.remove(controlStructuresJumpAdresses.size() - 1);
				Code.fixup(adr);
			
			}
			
		}
		
		//else
			//log.info("GRESKA");
		
		controlStructuresJumpAdresses.add(Code.pc - 2);
		
		pendingFixup.add(1);
		
	}
	
	public void visit (MatchedIfStatement mis) {
		
		if (!controlStructuresJumpAdresses.isEmpty()
				&& !pendingFixup.isEmpty() ) {
			
			int cnt = pendingFixup.get(pendingFixup.size() - 1);
			
			for (int i = 0; i < cnt; i++) {
			
				int adr = controlStructuresJumpAdresses.remove(controlStructuresJumpAdresses.size() - 1);
				Code.fixup(adr);
			
			}
			
		}
		
		//else
			//log.info("GRESKA");
		
	}
	
	public void visit (UnmatchedElseStatement ues) {
		
		if (!controlStructuresJumpAdresses.isEmpty()
				&& !pendingFixup.isEmpty() ) {
			
			int cnt = pendingFixup.get(pendingFixup.size() - 1);
			
			for (int i = 0; i < cnt; i++) {
			
				int adr = controlStructuresJumpAdresses.remove(controlStructuresJumpAdresses.size() - 1);
				Code.fixup(adr);
			
			}
			
		}
		
		//else
			//log.info("GRESKA");
		
	}
	
	public void visit (DoWhileStart dws) {
		
		doWhileStartAddresses.add(Code.pc);
		
		breakFixupAdresses.add(new ArrayList<Integer> ());
		
	}
	
	/** While control structure begining;
	 * <br> should start generating condition;
	 * <br> after code is generated, put jump and do fixup
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.WhileConditionStart)
	 */
	public void visit (WhileConditionStart wcs) {
		
		SyntaxNode parent = wcs.getParent();
		
		StatementCondition sc = null;
		
		if (parent instanceof MatchedWhileStatement) {
			
			MatchedWhileStatement mws = (MatchedWhileStatement) parent;
			
			sc = mws.getStatementCondition();
			
		}
		
		else if (parent instanceof UnmatchedWhileStatement) {
			
			UnmatchedWhileStatement uws = (UnmatchedWhileStatement) parent;
			
			sc = uws.getStatementCondition();
			
		}
		
		if (sc != null) {
			
			if (sc instanceof ConditionalOperator) {
					
				//log.info("ConditionalOperator");
				
				ConditionalExpressionCodeGenerator conditionCodeGenerator = new ConditionalExpressionCodeGenerator();
				sc.traverseBottomUp(conditionCodeGenerator);
				
				Code.loadConst(1);
				Code.putFalseJump(Code.ne, doWhileStartAddresses.remove(doWhileStartAddresses.size() - 1));
				
			}
				
			else if (sc instanceof NoConditionalOperator) {
				
				//log.info("NoConditionalOperator");
				
				ControlStructureCodeGenerator conditionCodeGenerator = new ControlStructureCodeGenerator();
				sc.traverseBottomUp(conditionCodeGenerator);
				
				conditionCodeGenerator.fixupAdresses();
				Code.putJump(doWhileStartAddresses.remove(doWhileStartAddresses.size() - 1));
				
				//log.info("conditionCodeGenerator.getControlStructuresExprJumpFixupAddresses().size: " + conditionCodeGenerator.getControlStructuresExprJumpFixupAddresses().size());
				
				for (Integer address : conditionCodeGenerator.getControlStructuresExprJumpFixupAddresses())
					Code.fixup(address);
				
			}
			
			for (Integer address : breakFixupAdresses.remove(breakFixupAdresses.size() - 1))
				Code.fixup(address);
			
		}
		
	}
	
	public void visit (ContinueStatement cs) {
		
		Code.putJump(doWhileStartAddresses.get(doWhileStartAddresses.size() - 1));
		
	}
	
	public void visit (BreakStatement bs) {
		
		Code.putJump(0);
		
		breakFixupAdresses.get(breakFixupAdresses.size() - 1).add(Code.pc - 2);
		
	}
	
	/**
	 * logical conditions
	 */

}

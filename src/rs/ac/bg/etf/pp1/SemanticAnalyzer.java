package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticAnalyzer extends VisitorAdaptor {
	
	Logger log = Logger.getLogger (getClass());
	
	//private MyDumpSymbolTableVisitor symTableVisitor = new MyDumpSymbolTableVisitor ();
	
	private ReportHelper reporter = new ReportHelper();
	
	// represents currently processed state; 
	private ProcessingState state = ProcessingState.GLOBAL;
	
	// struct node which holds currently processed class
	private Struct currentClass = null;
	
	// obj node which holds currently processed method
	private Obj currentMethod = null;
	
	// obj node which holds inherited class method with same name as currently processed method
	private Obj inheritedMethod = null;
	
	// flag which checks if return statement is found in currentMethod
	private boolean returnFound;
	
	// flag which checks if global method main is declared
	private boolean mainFound = false;
	
	// counter which shows if there is active do-while loop
	private int whileCounter = 0;
	
	// counter which shows if there is active switch statement
	private int switchCounter = 0;
	
	private List<Set<Integer>> switchNumConsts = new ArrayList <Set <Integer>> ();
	
	public boolean isSemanticErrorFound() {
		
		return reporter.isSemanticErrorFound();
	
	}
	
	/**
	 * analysis start/end
	 */

	/** ProgramName processing;
	 * <br> ProgramName ::= IDENT
	 * <br> creates new obj node, inserts in symbol table and opens new scope
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ProgramName)
	 */
	public void visit (ProgramName programName) {
		
		programName.obj = MyTabImpl.insert(Obj.Prog, programName.getProgramName(), MyTabImpl.noType);
		MyTabImpl.openScope();
		
		reporter.reportSemanticDeclarationStart("program", programName, programName.getProgramName());
		
		state = ProcessingState.GLOBAL;
		
	}
	
	/** Program processing
	 * <br> Program ::= PROGRAM ProgramName GlobalDeclarationsList LEFT_BRACE GlobalMethodDeclarationsList RIGHT_BRACE
	 * <br> chains local variables, closes scope
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.Program)
	 */
	public void visit (Program program) {
		
		MyTabImpl.chainLocalSymbols(program.getProgramName().obj);
		MyTabImpl.closeScope();
		
		if (!mainFound) 
			reporter.reportSemanticError("No main method found!", null);
		
		reporter.reportSemanticDeclaration("program", program, program.getProgramName().obj);
		
		state = ProcessingState.GLOBAL;
		
	}
	
	/**
	 * symbolic constants
	 */
	
	/** NumberConst processing;
	 * <br> ConstValue ::= NUMBER
	 * <br> create new obj with assigned value
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.NumberConst)
	 */
	public void visit (NumberConst numberConst) {
		
		numberConst.obj = new Obj(Obj.Con, numberConst.getNumberConst().toString(), MyTabImpl.intType, 
									numberConst.getNumberConst(), Obj.NO_VALUE);
		
	}
	
	/** CharConst processing;
	 * <br> ConstValue ::= CHAR
	 * <br> create new obj with assigned value
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.CharConst)
	 */
	public void visit (CharConst charConst) {
		
		charConst.obj = new Obj(Obj.Con, "'" + charConst.getCharConst() + "'", MyTabImpl.charType,
									Integer.valueOf (charConst.getCharConst()), Obj.NO_VALUE);
	
	}
	
	/** BoolConst processing;
	 * <br> ConstValue ::= BOOL
	 * <br> create new obj with assigned value
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.BoolConst)
	 */
	public void visit (BoolConst boolConst) {
		
		boolConst.obj = new Obj(Obj.Con, boolConst.getBoolConst() ? "true" : "false", MyTabImpl.boolType,
									boolConst.getBoolConst() ? 1 : 0, Obj.NO_VALUE);
		
	}
	
	/**
	 *  type
	 */
	
	/** Type processing
	 * <br> Type ::= IDENT
	 * <br> check if IDENT is in symbol table; if not found report error
	 * <br> if found, check if found Obj is Type; if not report error
	 * <br> return found type in struct field of argument
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.Type)
	 */
	public void visit (Type type) {
		
		Obj foundType = MyTabImpl.find(type.getTypeName());
			
		if (foundType == MyTabImpl.noObj) {
			
			reporter.reportSemanticError(type.getTypeName() + " is not declared", type);
			type.struct = Tab.noType;
		
		}
		
		else {
			
			if (foundType.getKind() != Obj.Type) {
				
				reporter.reportSemanticError(type.getTypeName() + " must be declared as type", type);
				type.struct = Tab.noType;
				
			}
			
			else 
				type.struct = foundType.getType();
			
			reporter.reportSemanticDetection(type, "type", foundType);
			
		}
	
	}
	
	/**
	 *  global constants
	 */
	
	/** SingleConstDecl processing;
	 * <br> SingleConstDecl ::= IDENT ASSIGN ConstValue
	 * <br> check if IDENT already declared; if declared report error
	 * <br> fetch parent instance of ConstDecl to check type compatibility; if ConstDecl.type is not null and equals type of ConstValue, insert constant in symbol table
	 * <br>
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SingleConstDecl)
	 */
	public void visit (SingleConstDecl scd) {
		
		Obj objFound = MyTabImpl.find(scd.getConstName());
		
		if (objFound != Tab.noObj) 
			reporter.reportSemanticError(scd.getConstName() + " is already declared", scd);
		
		else {
			
			SyntaxNode typeNode = scd.getParent();
			
			while (typeNode.getClass() != ConstDecl.class) typeNode = typeNode.getParent();
			
			Struct constType = ((ConstDecl) typeNode).getType().struct;
			
			Obj constValue = scd.getConstValue().obj;
			
			if (constType != Tab.noType) {
				
				if (constType.equals(constValue.getType())) {
					
					Obj newObj = MyTabImpl.insert(Obj.Con, scd.getConstName(), constType);
					newObj.setAdr(constValue.getAdr());
					
					reporter.reportSemanticDeclaration("global constant", scd, newObj);
					
				}
				
				else
					reporter.reportSemanticError("Type of constant value must be equal to declared constant type of global constant " + scd.getConstName(), scd);
				
			}
			
		}
		
	}
	
	/**
	 * global variables
	 */
	
	/** SingleVarDecl processing;
	 * <br> SingleVarDecl ::= IDENT ArrayOption
	 * <br> check if IDENT already declared; if declared report error
	 * <br> fetch parent instance of MultipleVarDeclSuccess to collect type ; if type is not null, insert variable in symbol table
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SingleVarDecl)
	 */
	public void visit (SingleVarDecl svd) {
		
		Obj objFound = MyTabImpl.find(svd.getVarName());
		
		if (objFound != Tab.noObj)
			reporter.reportSemanticError(svd.getVarName() + " is already declared", svd);
		
		else {
			
			SyntaxNode typeNode = svd.getParent();
			
			while (typeNode.getClass() != MultipleVarDeclSuccess.class) typeNode = typeNode.getParent();
			
			Struct varType = ((MultipleVarDeclSuccess) typeNode).getType().struct;
			
			if (varType != Tab.noType) {
			
				if (svd.getArrayVariableOption() instanceof ArrayVariable) 
					varType = new Struct (Struct.Array, varType);
				
				Obj newObj = MyTabImpl.insert(Obj.Var, svd.getVarName(), varType);
				
				reporter.reportSemanticDeclaration("global variable", svd, newObj);
			
			}
			
		}
		
	}
	
	/**
	 *  class declarations
	 */
	
	/** ClassName processing;
	 * <br> ClassName ::= IDENT ExtendsOption
	 * <br> check if IDENT already declared; if declared report error
	 * <br> if not insert new type symbol in table, set currentClass and open new scope; insert virtual functions table pointer; 
	 * <br> if there is class inheritance, check if Type inheriting is class; if not, report error;
	 * <br> set elemType of Struct, and inherit all fields from parent class
	 * <br> set state to CLASS
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ClassName)
	 */
	public void visit (ClassName cn) {
		
		Obj objFound = MyTabImpl.find(cn.getClassName());
		
		if (objFound != Tab.noObj) {

			currentClass = new Struct (Struct.None);
			cn.obj = new Obj (Obj.Type, cn.getClassName(), currentClass);
			
			reporter.reportSemanticError(cn.getClassName() + " is already declared", cn);
		
		}
		
		else {
			
			currentClass = new Struct (Struct.Class);
			cn.obj = MyTabImpl.insert(Obj.Type, cn.getClassName(), currentClass);
			
			reporter.reportSemanticDeclarationStart("class", cn, cn.getClassName());
			
		}
		
		MyTabImpl.openScope();
		
		//Obj vft = MyTabImpl.insert(Obj.Fld, "__vft", MyTabImpl.intType);
		MyTabImpl.insert(Obj.Fld, "__vft", MyTabImpl.intType);
		
		if (cn.getExtendsOption() instanceof ClassInheritance) {
			
			ClassInheritance inheritanceCheck = (ClassInheritance) cn.getExtendsOption();
			
			if (inheritanceCheck.getExtendsSyntaxCheck() instanceof InheritanceSuccess) {
				
				InheritanceSuccess is = (InheritanceSuccess) inheritanceCheck.getExtendsSyntaxCheck();
				
				Struct parentClass = is.getType().struct;
				
				if (parentClass != MyTabImpl.noType) {
					
					if (parentClass.getKind() != Struct.Class)
						reporter.reportSemanticError("class can only extend other classes; " + is.getType().getTypeName() + " is not class!", is);
					
					else {
						
						currentClass.setElementType(parentClass);
						
						for (Obj fld : parentClass.getMembers()) {
							
							if (fld.getKind() == Obj.Fld &&
									!fld.getName().equals("__vft"))
								MyTabImpl.currentScope().addToLocals(fld);
						
						}
					
					}
					
				}
				
			}
			
		}
		
		state = ProcessingState.CLASS;
		
	}
	
	/** ClassDecl processing;
	 * <br> ClassDecl ::= CLASS ClassName LEFT_BRACE FieldsDeclarationsList ClassMethodDeclarationsOption RIGHT_BRACE
	 * <br> chain local symbols and close scope; 
	 * <br> set currentClass to null and state to GLOBAL
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ClassDecl)
	 */
	public void visit (ClassDecl cd) {
		
		if (currentClass.getElemType() != null
				&& currentClass.getElemType() != MyTabImpl.noType) {
			
			for (Obj parentMethod : currentClass.getElemType().getMembers()) {
				
				if (parentMethod.getKind() == Obj.Meth) {
					
					Obj methodFound = MyTabImpl.currentScope().findSymbol(parentMethod.getName());
					
					if (methodFound == null) {
						
						methodFound = new Obj (parentMethod.getKind(), parentMethod.getName(), parentMethod.getType(),
								parentMethod.getAdr(), parentMethod.getLevel());
						
						MyTabImpl.openScope();
						
						Obj thisPointer = MyTabImpl.insert(Obj.Var, "this", currentClass);
						thisPointer.setFpPos(-1);
						
						for (Obj parentLocal : parentMethod.getLocalSymbols()) {
							
							if (parentLocal.getName() != "this")
								MyTabImpl.currentScope().addToLocals(parentLocal);
								
						}
						
						MyTabImpl.chainLocalSymbols(methodFound);
						MyTabImpl.closeScope();
						
						MyTabImpl.currentScope().addToLocals(methodFound);
						
					}
				
				}
			
			}
			
		}
		
		MyTabImpl.chainLocalSymbols(currentClass);
		MyTabImpl.closeScope();
		
		if (currentClass.getKind() == Struct.Class)
			reporter.reportSemanticDeclaration("class", cd, cd.getClassName().obj);
		
		currentClass = null;
		
		state = ProcessingState.GLOBAL;
		
	}
	
	/**
	 * class fields
	 */
	
	/** SingleFieldDecl processing
	 * <br> SingleFieldDecl ::= IDENT ArrayOption
	 * <br> check if class declaration is in progress;
	 * <br> check if IDENT already declared; if declared report error (overriding fields is not allowed)
	 * <br> check if field has same name as inherited method; if does report error
	 * <br> fetch parent instance of MultipleFieldDeclSuccess to collect type ; if type is not null, insert field in symbol table
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SingleFieldDecl)
	 */
	public void visit (SingleFieldDecl sfd) {
		
		Obj objFound = MyTabImpl.currentScope().findSymbol(sfd.getFieldName());
			
		if (objFound != null)
			reporter.reportSemanticError(sfd.getFieldName() + " is already declared", sfd);
		
		else {
			
			if (currentClass.getElemType() != null) {
				
				objFound = currentClass.getElemType().getMembersTable().searchKey(sfd.getFieldName());
				
				if (objFound != null
						&& objFound.getKind() == Obj.Meth)
					reporter.reportSemanticError(sfd.getFieldName() + " already declared as method in parent class; can't redeclare as field", sfd);
				
			}
			
			if (objFound == null) {
				
				SyntaxNode typeNode = sfd.getParent();
				
				while (typeNode.getClass() != MultipleFieldDeclSuccess.class) typeNode = typeNode.getParent();
				
				Struct fieldType = ((MultipleFieldDeclSuccess) typeNode).getType().struct;
				
				if (fieldType != Tab.noType) {
				
					if (sfd.getArrayVariableOption() instanceof ArrayVariable) 
						fieldType = new Struct (Struct.Array, fieldType);
					
					Obj newObj = MyTabImpl.insert(Obj.Fld, sfd.getFieldName(), fieldType);
					
					reporter.reportSemanticDeclaration("field", sfd, newObj);
				
				}
			
			}
		
		}
		
	}
	
	/**
	 *  methods (class and global)
	 */
	
	/** NoVoidReturn processing;
	 * <br> ReturnType :== Type
	 * <br> type already checked, pass that as result
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ReturnType)
	 */
	public void visit (NoVoidReturn nvr) {
		
		nvr.struct = nvr.getType().struct;
		
	}
	
	/** VoidReturn processing;
	 * <br> ReturnType ::= VOID
	 * <br> return Tab.noType
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.VoidReturn)
	 */
	public void visit (VoidReturn vr) {
		
		vr.struct = MyTabImpl.noType;
		
	}
	
	/** MethodName processing;
	 * <br> MethodName ::= ReturnType IDENT
	 * <br> check if global or class method (if class, check current class declaration)
	 * <br> check if IDENT already declared; if declared report error
	 * <br> fetch type from returnType property; if type is not null, insert method obj in symbol table, set currentMethod, returnFound and open scope;
	 * <br> if class method, insert implicit param this; check for inheritence (type check)
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MethodName)
	 */
	public void visit (MethodName mn) {
		
		Obj objFound = MyTabImpl.currentScope().findSymbol(mn.getMethodName());
		
		Struct methodType = mn.getReturnType().struct;
			
		if (objFound != null) {
			
			reporter.reportSemanticError(mn.getMethodName() + " is already declared", mn);
			
			mn.obj = new Obj (Obj.Meth, mn.getMethodName(), methodType);
			
		}
			
		else 
			mn.obj = MyTabImpl.insert(Obj.Meth, mn.getMethodName(), methodType);
		
		mn.obj.setLevel(0);
		currentMethod = mn.obj;
		returnFound = false;
		MyTabImpl.openScope();
		
		if (state == ProcessingState.CLASS) {
						
			Obj thisPointer = MyTabImpl.insert(Obj.Var, "this", currentClass);
			thisPointer.setFpPos(-1);
			reporter.reportSemanticDeclarationStart("class method", mn, mn.getMethodName());
			
			if (currentClass.getElemType() != null) {
				
				objFound = currentClass.getElemType().getMembersTable().searchKey(mn.getMethodName());
				
				if (objFound != null
						&& objFound.getKind() == Obj.Meth )
					inheritedMethod = objFound;
				
				if (inheritedMethod != null
						&& !methodType.assignableTo(inheritedMethod.getType()))
					reporter.reportSemanticError("Can't redefine inherited method's return type", mn);
				
			}
			
		}
					
		else if (state == ProcessingState.GLOBAL) 
			reporter.reportSemanticDeclarationStart("global method", mn, mn.getMethodName());
		
		if (state == ProcessingState.GLOBAL)
			state = ProcessingState.GLOBAL_METHOD;
		
		else if (state == ProcessingState.CLASS)
			state = ProcessingState.CLASS_METHOD;
		
	}
	
	/** MethodRightParenthesis processing;
	 * <br> MethodRightParenthesis ::= RIGHT_PARENTHESIS
	 * <br> only used for class methods, to check for inheritence, if number of formal arguments match
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MethodRightParenthesis)
	 */
	public void visit (MethodRightParenthesis mrp) {
		
		if (state == ProcessingState.CLASS_METHOD
				&& inheritedMethod != null) {
			
			if (currentMethod.getLevel() != inheritedMethod.getLevel())
				reporter.reportSemanticError("Can't redefine inherited method's number of formal parameters", mrp.getParent());
			
		}
		
	}
	
	/** MethodDecl processing;
	 * <br> MethodDecl ::= MethodName LEFT_PARENTHESIS FormParsOption RIGHT_PARENTHESIS LocalVarDeclarationsList LEFT_BRACE StatementList RIGHT_BRACE
	 * <br> check for return statement in non-void methods
	 * <br> if global method main, check if void without parameters
	 * <br> chain symbols in currentMethod and close scope
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MethodDecl)
	 */
	public void visit (MethodDecl md) {
		
		if (currentMethod.getType() != Tab.noType
				&& !returnFound)
			reporter.reportSemanticError("No return statement found in non-void method " + md.getMethodName().getMethodName(), null);
		
		if (state == ProcessingState.GLOBAL_METHOD
				&& currentMethod.getName().equals("main")) {
				
			if (currentMethod.getType() != Tab.noType)
				reporter.reportSemanticError("Global method main must be declared as void", md);
				
			if (currentMethod.getLevel() != 0)
				reporter.reportSemanticError("Global method main must be declared without formal parameters", md);
				
			mainFound = true;
		
		}
			
		MyTabImpl.chainLocalSymbols(currentMethod);
		MyTabImpl.closeScope();
			
		if (state == ProcessingState.GLOBAL_METHOD)
			reporter.reportSemanticDeclaration("global method", md, currentMethod);
		
		else if (state == ProcessingState.CLASS_METHOD)
			reporter.reportSemanticDeclaration("class method", md, currentMethod);
			
		currentMethod = null;
		inheritedMethod = null;
		returnFound = false;
		
		if (state == ProcessingState.GLOBAL_METHOD)
			state = ProcessingState.GLOBAL;
		
		else if (state == ProcessingState.CLASS_METHOD)
			state = ProcessingState.CLASS;
		
	}
	
	/** SingleFormPar processing;
	 * <br> SingleFormPar ::= Type IDENT ArrayVariableOption
	 * <br> check if IDENT already declared; if declared report error
	 * <br> if class method overrides another method, check if type matches
	 * <br> insert new symbol and increase level of currentMethod
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SingleFormPar)
	 */
	public void visit (SingleFormPar sfp) {
		
		Obj objFound = MyTabImpl.currentScope().findSymbol(sfp.getFormParName());
		
		if (objFound != null)
			reporter.reportSemanticError(sfp.getFormParName() + " is already declared", sfp);
		
		else {
			
			Struct formalParameterType = sfp.getType().struct;
			
			if (state == ProcessingState.CLASS_METHOD
					&& inheritedMethod != null) {
				
				Struct inheritedFormalParameterType = null;
				
				for (Obj local : inheritedMethod.getLocalSymbols()) {
					
					if (local.getFpPos() == currentMethod.getLevel()) {
						
						inheritedFormalParameterType = local.getType();
						break;
						
					}
					
				}
				
				if (inheritedFormalParameterType != null
						&& !formalParameterType.assignableTo(inheritedFormalParameterType))
					reporter.reportSemanticError("Can't redefine inherited method's type of formal parameter on position " + currentMethod.getLevel(), sfp);
				
			}
			
			if (formalParameterType != Tab.noType) {
				
				if (sfp.getArrayVariableOption() instanceof ArrayVariable)
					formalParameterType = new Struct (Struct.Array, formalParameterType);
				
				Obj newObj = MyTabImpl.insert(Obj.Var, sfp.getFormParName(), formalParameterType);
				newObj.setFpPos(currentMethod.getLevel());
				currentMethod.setLevel(currentMethod.getLevel() + 1);
				
				reporter.reportSemanticDeclaration("formal parameter", sfp, newObj);
				
			}
			
		}
		
	}
	
	/** SingleLocalVarDecl processing;
	 * <br> SingleLocalVarDecl ::= IDENT ArrayOption
	 * <br> check if IDENT already declared in scope; if declared report error
	 * <br> fetch parent instance of LocalVarDecl to collect type ; if type is not null, insert variable in symbol table
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SingleLocalVarDecl)
	 */
	public void visit (SingleLocalVarDecl slvd) {
		
		Obj objFound = MyTabImpl.currentScope().findSymbol(slvd.getLocalVarName());
		
		if (objFound != null)
			reporter.reportSemanticError(slvd.getLocalVarName() + " is already declared", slvd);
		
		else {
			
			SyntaxNode typeNode = slvd.getParent();
			
			while (typeNode.getClass() != LocalVarDecl.class) typeNode = typeNode.getParent();
			
			Struct localVarType = ((LocalVarDecl) typeNode).getType().struct;
			
			if (localVarType != Tab.noType) {
			
				if (slvd.getArrayVariableOption() instanceof ArrayVariable) 
					localVarType = new Struct (Struct.Array, localVarType);
				
				Obj newObj = MyTabImpl.insert(Obj.Var, slvd.getLocalVarName(), localVarType);
				newObj.setFpPos(-1);
				
				reporter.reportSemanticDeclaration("local variable", slvd, newObj);
			
			}
			
		}
		
	}
	
	/**
	 * statements
	 */
	
	/** AssignStatement processing;
	 * <br> AssignStatement ::= Designator Assignop Source
	 * <br> Designator must be Obj.Var, Fld or Elem kind
	 * <br> Source must be assignable to Designator
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.AssignStatement)
	 */
	public void visit (AssignStatement as) {
		
		if (as.getDesignator().obj != Tab.noObj
				&& as.getSource().struct != Tab.noType) {
			
			Obj designator = as.getDesignator().obj;
			Struct source = as.getSource().struct;
			
			//log.info(designator == null);
			//log.info(source == null);
			
			if (designator.getKind() != Obj.Var
					&& designator.getKind() != Obj.Fld
					&& designator.getKind() != Obj.Elem)
				reporter.reportSemanticError(designator.getName() + " must be global/local variable, class field or array element", as.getDesignator());
			
			if (!source.assignableTo(designator.getType()))
				reporter.reportSemanticError("Expression after assign operator is not assignable to destination designator", as.getSource());
			
		}
		
	}
	
	/** MethodCallStatement processing;
	 * <br> MethodCallStatement ::= Designator LEFT_PARENTHESIS ActPars RIGHT_PARENTHESIS
	 * <br> Designator must be Obj.Meth
	 * <br> if designator is meth, check actual and formal parameters
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MethodCallStatement)
	 */
	public void visit (MethodCallStatement mcs) {
		
		if (mcs.getMethodDesignator().obj != Tab.noObj) {
			
			Obj designator = mcs.getMethodDesignator().obj;
			
			//if (designator.getKind() != Obj.Meth)
				//reporter.reportSemanticError(designator.getName() + " must be global or class method", mcs.getDesignator());
			
			ActualParameterSemanticAnalyzer actParsCounter = new ActualParameterSemanticAnalyzer(designator, true, reporter);
			mcs.getActParsOption().traverseBottomUp(actParsCounter);
			
			if (actParsCounter.getActParsCount()
					!= mcs.getMethodDesignator().obj.getLevel())
				reporter.reportSemanticError("Numbers of actual and formal parameters do not match", mcs.getActParsOption());
			
			else {
			
				ActualParameterSemanticAnalyzer actParsAnalyzer = new ActualParameterSemanticAnalyzer(designator, false, reporter);
				mcs.getActParsOption().traverseBottomUp(actParsAnalyzer);
			
			}
			
		}
		
	}
	
	/** IncrementStatement
	 * <br> IncrementStatement ::= Designator INC
	 * <br> Designator must be Obj.Var, Fld or Elem kind
	 * <br> Designator must be Struct.Int
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.IncrementStatement)
	 */
	public void visit (IncrementStatement is) {
		
		if (is.getDesignator().obj != Tab.noObj) {
			
			Obj designator = is.getDesignator().obj;
			Struct type = is.getDesignator().obj.getType();
			
			if (designator.getKind() != Obj.Var
					&& designator.getKind() != Obj.Fld
					&& designator.getKind() != Obj.Elem)
				reporter.reportSemanticError(designator.getName() + " must be global/local variable, class field or array element", is.getDesignator());
			
			if (type.getKind() != Struct.Int)
				reporter.reportSemanticError(designator.getName() + " must be int", is.getDesignator());
			
		}
		
	}
	
	/** DecrementStatement
	 * <br> DecrementStatement ::= Designator DEC
	 * <br> Designator must be Obj.Var, Fld or Elem kind
	 * <br> Designator must be Struct.Int 
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.DecrementStatement)
	 */
	public void visit (DecrementStatement ds) {
		
		if (ds.getDesignator().obj != Tab.noObj) {
			
			Obj designator = ds.getDesignator().obj;
			Struct type = ds.getDesignator().obj.getType();
			
			if (designator.getKind() != Obj.Var
					&& designator.getKind() != Obj.Fld
					&& designator.getKind() != Obj.Elem)
				reporter.reportSemanticError(designator.getName() + " must be global/local variable, class field or array element", ds.getDesignator());
			
			if (type.getKind() != Struct.Int)
				reporter.reportSemanticError(designator.getName() + " must be int", ds.getDesignator());
			
		}
		
	}
	
	/** Do processing;
	 * <br> Do ::= DO
	 * <br> starts new do-while loop, used for break and continue check
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.Do)
	 */
	public void visit (DoWhileStart d) {
		
		whileCounter++;
	
	}
	
	/** Switch processing;
	 * <br> Switch ::= SWITCH
	 * <br> starts new switch statement, used for break check
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.Switch)
	 */
	public void visit (SwitchStart s) {
		
		switchCounter++;
		
		switchNumConsts.add(new HashSet<Integer> ());
		
	}
	
	/** MatchedWhileStatement processing;
	 * <br> MatchedWhileStatement ::= Do MatchedStatement WHILE LEFT_PARENTHESIS StatementCondition RIGHT_PARENTHESIS SEMICOLON
	 * <br> ends do-while loop, used for break and continue check
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MatchedWhileStatement)
	 */
	public void visit (MatchedWhileStatement mws) {
		
		whileCounter--;
		
	}
	
	/** UnmatchedWhileStatement processing;
	 * <br> UnmatchedWhileStatement ::= Do UnmatchedStatement WHILE LEFT_PARENTHESIS StatementCondition RIGHT_PARENTHESIS SEMICOLON
	 * <br> ends do-while loop, used for break and continue check
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.UnmatchedWhileStatement)
	 */
	public void visit (UnmatchedWhileStatement uws) {
		
		whileCounter--;
		
	}
	
	/** MatchedSwitchStatement processing;
	 * <br> MatchedSwitchStatement ::= Switch LEFT_PARENTHESIS Expr RIGHT_PARENTHESIS LEFT_BRACE CaseStatementList RIGHT_BRACE
	 * <br> ends switch statement, used for break check
	 * <br> Expr must be Struct.Int
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MatchedSwitchStatement)
	 */
	public void visit (MatchedSwitchStatement mss) {
		
		if (mss.getExpr().struct != MyTabImpl.noType) {
			
			if (mss.getExpr().struct != MyTabImpl.intType)
				reporter.reportSemanticError("Expression in switch statement must be int", mss.getExpr());
			
		}
		
		switchCounter--;
		switchNumConsts.remove(switchCounter);
		
	}
	
	/** CaseStatement processing;
	 * <br> CaseStatement ::= CASE NUMBER COLON StatementList
	 * <br> NUMBER must be unique in each CASE statement
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.CaseStatement)
	 */
	public void visit (CaseStatement cs) {
		
		if (switchNumConsts.get(switchCounter - 1).contains(cs.getN1()))
			reporter.reportSemanticError("Number constant in case statement must be unique", cs);
		
		else
			switchNumConsts.get(switchCounter - 1).add(cs.getN1());
			
	}
	
	/** BreakStatement processing;
	 * <br> BreakStatement ::= BREAK SEMICOLON
	 * <br> must be inside do-while or switch statement
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.BreakStatement)
	 */
	public void visit (BreakStatement bs) {
		
		if (whileCounter == 0
				&& switchCounter == 0)
			reporter.reportSemanticError("Break statement must be inside do-while loop or switch statement", bs);
		
	}
	
	/** ContinueStatement processing;
	 * <br> ContinueStatement ::= CONTINUE SEMICOLON
	 * <br> must be inside do-while statement
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ContinueStatement)
	 */
	public void visit (ContinueStatement cs) {
		
		if (whileCounter == 0)
			reporter.reportSemanticError("Continue statement must be inside do-while loop statement", cs);
		
	}
	
	/** ReturnStatement processing;
	 * <br> ReturnStatement ::= RETURN ExprOption SEMICOLON
	 * <br> if there is Expr, must be equal to currentMethod type
	 * <br> if there is no Expr, currentMethod must be void
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ReturnStatement)
	 */
	public void visit (ReturnStatement rs) {
		
		returnFound = true;
		
		if (rs.getReturnExprOption() instanceof ReturnExpr ) {
			
			ReturnExpr returnExpr = (ReturnExpr) rs.getReturnExprOption();
			
			if (returnExpr.getExpr().struct != MyTabImpl.noType) {
				
				if (currentMethod.getType() != null
						&& !returnExpr.getExpr().struct.equals(currentMethod.getType()))
					reporter.reportSemanticError("Expression in return statement must be same type as declared method", rs);
			
			}
			
		}
		
		else if (rs.getReturnExprOption() instanceof NoReturnExpr) {
			
			if (currentMethod.getType() != MyTabImpl.noType)
				reporter.reportSemanticError("Only void methods can have return statement without expression", rs);
			
		}
		
	}
	
	/** ReadStatement processing;
	 * <br> ReadStatement ::= READ LEFT_PARENTHESIS Designator RIGHT_PARENTHESIS SEMICOLON
	 * <br> Designator must be Obj.Var, Fld or Elem
	 * <br> Designator must be Struct.Int, Char or Bool
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ReadStatement)
	 */
	public void visit (ReadStatement rs) {
		
		if (rs.getDesignator().obj != MyTabImpl.noObj) {
			
			Obj designator = rs.getDesignator().obj;
			Struct type = rs.getDesignator().obj.getType();
			
			if (designator.getKind() != Obj.Var
					&& designator.getKind() != Obj.Fld
					&& designator.getKind() != Obj.Elem)
				reporter.reportSemanticError(designator.getName() + " must be global/local variable, class field or array element", rs.getDesignator());
			
			if (type.getKind() != Struct.Int
					&& type.getKind() != Struct.Char
					&& type.getKind() != Struct.Bool)
				reporter.reportSemanticError(designator.getName() + " must be int, char or bool", rs.getDesignator());
			
		}
		
	}
	
	/** PrintStatement processing;
	 * <br> PrintStatement ::= PRINT LEFT_PARENTHESIS Expr PrintOption RIGHT_PARENTHESIS SEMICOLON
	 * <br> Expr must be Struct.Int, Char or Bool
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.PrintStatement)
	 */
	public void visit (PrintStatement ps) {
		
		if (ps.getExpr().struct != MyTabImpl.noType) {
			
			Struct type = ps.getExpr().struct;
			
			if (type.getKind() != Struct.Int
					&& type.getKind() != Struct.Char
					&& type.getKind() != Struct.Bool)
				reporter.reportSemanticError("Expression in print statement must be int, char or bool", ps.getExpr());
			
		}
		
	}
	
	/**
	 * logical conditions
	 */
	
	/** ConditionalOperator processing;
	 * <br> ConditionalOperator ::= CondExpr
	 * <br> type of CondExpr must be Struct.Bool
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ConditionalOperator)
	 */
	public void visit (ConditionalOperator co) {
		
		if (co.getCondExpr().struct != MyTabImpl.noType) {
			
			if (co.getCondExpr().struct != MyTabImpl.boolType) {
			
				reporter.reportSemanticError("Expression in logical condition must be bool", co);
				co.struct = MyTabImpl.noType;
			
			}
			
			else
				co.struct = MyTabImpl.boolType;
			
		}
		
		else
			co.struct = MyTabImpl.boolType;
		
	}
	
	/** NoConditionalOperator processing;
	 * <br> NoConditionalOperator ::= Condition
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.NoConditionalOperator)
	 */
	public void visit (NoConditionalOperator nco) {
		
		nco.struct = nco.getCondition().struct;
		
	}
	
	/** MultipleTermCondition processing;
	 * <br> MultipleTermCondition ::= Condition OR CondTerm
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MultipleTermCondition)
	 */
	public void visit (MultipleTermCondition mtc) {
		
		if (mtc.getCondition().struct != MyTabImpl.noType
				&& mtc.getCondTerm().struct != MyTabImpl.noType)
			mtc.struct = mtc.getCondition().struct;
		
		else
			mtc.struct = MyTabImpl.noType;
		
	}
	
	/** SingleTermCondition processing;
	 * <br> SingleTermCondition ::= CondTerm
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SingleTermCondition)
	 */
	public void visit (SingleTermCondition stc) {
		
		stc.struct = stc.getCondTerm().struct;
		
	}
	
	/** MultipleFactTerm
	 * <br> MultipleFactTerm ::= CondTerm AND CondFact
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MultipleFactTerm)
	 */
	public void visit (MultipleFactTerm mft) {
		
		if (mft.getCondTerm().struct != MyTabImpl.noType
				&& mft.getCondFact().struct != MyTabImpl.noType)
			mft.struct = mft.getCondTerm().struct;
		
		else
			mft.struct = MyTabImpl.noType;
		
	}
	
	/** SingleFactTerm processing;
	 * <br> SingleFactTerm ::= CondFact
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SingleFactTerm)
	 */
	public void visit (SingleFactTerm sft) {
		
		sft.struct = sft.getCondFact().struct;
		
	}
	
	/** MultipleExprFactor processing;
	 * <br> MultipleExprFactor ::= FirstExpr Relop SecondExpr
	 * <br> FirstExpr and SecondExpr must be compatible
	 * <br> if FirstExpr and SecondExpr are Struct.Array or Struct.Class, Relop must be != or ==
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MultipleExprFactor)
	 */
	public void visit (MultipleExprFactor mef) {
		
		if (mef.getFirstExpr().struct != MyTabImpl.noType
				&& mef.getSecondExpr().struct != MyTabImpl.noType) {
			
			if (!mef.getFirstExpr().struct.compatibleWith(
					mef.getSecondExpr().struct)) {
				
				reporter.reportSemanticError("Expressions around relation operator must be compatible", mef);
				mef.struct = MyTabImpl.noType;
				
			}
			
			else {
				
				if (mef.getFirstExpr().struct.getKind() == Struct.Class
						|| mef.getFirstExpr().struct.getKind() == Struct.Array) {
					
					if (!(mef.getRelop() instanceof Equals
							|| mef.getRelop() instanceof NotEquals)) {
						
						reporter.reportSemanticError("Only == and != are allowed for class and array expressions", mef);
						mef.struct = MyTabImpl.noType;
						
					}
					
					else
						mef.struct = MyTabImpl.boolType;
					
				}
				
				else
					mef.struct = MyTabImpl.boolType;
			}
		}
		
		else
			mef.struct = MyTabImpl.noType;
		
	}
	
	/** SingleExprFactor processing;
	 * <br> SingleExprFactor ::= Expr
	 * <br> Expr must be Struct.Bool
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SingleExprFactor)
	 */
	public void visit (SingleExprFactor sef) {
		
		if (sef.getNonCondExpr().struct != MyTabImpl.noType) {
			
			if (sef.getNonCondExpr().struct != MyTabImpl.boolType) {
				
				reporter.reportSemanticError("Expression in logical condition must be Bool", sef);
				sef.struct = MyTabImpl.noType;
				
			}
			
			else
				sef.struct = sef.getNonCondExpr().struct;
			
		}
		
		else
			sef.struct = MyTabImpl.noType;
		
	}
	
	/**
	 * expressions
	 */
	
	public void visit (AssignSuccess as) {
		
		as.struct = as.getExpr().struct;
		
	}
	
	/** CondExpr processing;
	 * <br> CondExpr ::= Condition QUESTION_MARK FirstCondExpr COLON SecondCondExpr
	 * <br> FirstCondExpr and SecondCondExpr must be of same Struct
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.CondExpr)
	 */
	public void visit (CondExpr ce) {
		
		if (ce.getFirstCondExpr().struct != MyTabImpl.noType
				&& ce.getSecondCondExpr().struct != MyTabImpl.noType) {
			
			if (!ce.getFirstCondExpr().struct.equals(
					ce.getSecondCondExpr().struct)) {
				
				reporter.reportSemanticError("Expressions around : in conditional operator must have same type", ce);
				ce.struct = MyTabImpl.noType;
			
			}
			
			else
				ce.struct = ce.getFirstCondExpr().struct;
		}
		
		else
			ce.struct = MyTabImpl.noType;
		
	}
	
	/** FirstCondExpr processing;
	 * <br> FirstCondExpr ::= Expr
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.FirstCondExpr)
	 */
	public void visit (FirstCondExpr fce) {
		
		fce.struct = fce.getExpr().struct;
		
	}
	
	/** SecondCondExpr processing;
	 * <br> SecondCondExpr ::= Expr
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SecondCondExpr)
	 */
	public void visit (SecondCondExpr sce) {
		
		sce.struct = sce.getExpr().struct;
		
	}
	
	/** FirstExpr processing;
	 * <br> FirstExpr ::= Expr
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.FirstExpr)
	 */
	public void visit (FirstExpr fe) {
		
		fe.struct = fe.getNonCondExpr().struct;
		
	}
	
	/** SecondExpr processing;
	 * <br> SecondExpr ::= Expr
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SecondExpr)
	 */
	public void visit (SecondExpr se) {
		
		se.struct = se.getNonCondExpr().struct;
		
	}
	
	/** ConditionalOperatorExpr processing;
	 * <br> ConditionalOperatorExpr ::= Expr
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ConditionalOperatorExpr)
	 */
	public void visit (ConditionalOperatorExpr coe) {
		
		coe.struct = coe.getCondExpr().struct;
		
	}
	
	/** NoConditionalOperatorExpr processing;
	 * <br> NoConditionalOperatorExpr ::= Expr
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.NoConditionalOperatorExpr)
	 */
	public void visit (NoConditionalOperatorExpr ncoe) {
		
		ncoe.struct = ncoe.getNonCondExpr().struct;
		
	}
	
	/** MultipleTermExpr processing:
	 * <br> MultipleTermExpr ::= Expr Addop Term
	 * <br> Expr and Term must be Struct.Int
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MultipleTermExpr)
	 */
	public void visit (MultipleTermExpr mte) {
		
		if (mte.getNonCondExpr().struct != MyTabImpl.noType
				&& mte.getTerm().struct != MyTabImpl.noType) {
			
			if (mte.getNonCondExpr().struct != MyTabImpl.intType
					|| mte.getTerm().struct != MyTabImpl.intType) {
				
				String addop = "";
				
				if (mte.getAddop() instanceof Add)
					addop = "+";
				
				else if (mte.getAddop() instanceof Sub)
					addop = "-";
				
				if (mte.getNonCondExpr().struct != MyTabImpl.intType)
					reporter.reportSemanticError("Expression before " + addop + " must be int", mte.getNonCondExpr());
				
				if (mte.getTerm().struct != MyTabImpl.intType)
					reporter.reportSemanticError("Term after " + addop + " must be int", mte.getTerm());
				
				mte.struct = MyTabImpl.noType;
				
			}
			
			else
				mte.struct = mte.getNonCondExpr().struct;
			
		}
		
		else
			mte.struct = MyTabImpl.noType;
		
	}
	
	/** SingleTermExpr processing;
	 * <br> SingleTermExpr ::= Term
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SingleTermExpr)
	 */
	public void visit (SingleTermExpr ste) {
		
		ste.struct = ste.getTerm().struct;
		
	}
	
	/** MinusTermExpr processing;
	 * <br> MinusTermExpr ::= SUB Term
	 * <br> Term must be Struct.Int
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MinusTermExpr)
	 */
	public void visit (MinusTermExpr mte) {
		
		if (mte.getTerm().struct != MyTabImpl.noType) {
			
			if (mte.getTerm().struct != MyTabImpl.intType) {
				
				if (mte.getTerm().struct != MyTabImpl.intType)
					reporter.reportSemanticError("Term after - must be int", mte.getTerm());
				
				mte.struct = MyTabImpl.noType;
				
			}
			
			else
				mte.struct = mte.getTerm().struct;
			
		}
		
		else
			mte.struct = MyTabImpl.noType;
		
	}
	
	/**
	 * terms
	 */
	
	/** MultipleFactorTerm processing;
	 * <br> MultipleFactorTerm ::= Term Mulop Factor
	 * <br> Term and Factor must be Struct.Int
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MultipleFactorTerm)
	 */
	public void visit (MultipleFactorTerm mft) {
		
		if (mft.getTerm().struct != MyTabImpl.noType
				&& mft.getFactor().struct != MyTabImpl.noType) {
			
			if (mft.getTerm().struct != MyTabImpl.intType
					|| mft.getFactor().struct != MyTabImpl.intType) {
				
				String mulop = "";
				
				if (mft.getMulop() instanceof Mul)
					mulop = "*";
				
				else if (mft.getMulop() instanceof Div)
					mulop = "/";
				
				else if (mft.getMulop() instanceof Mod)
					mulop = "%";
				
				if (mft.getTerm().struct != MyTabImpl.intType)
					reporter.reportSemanticError("Term before " + mulop + " must be int", mft.getTerm());
				
				if (mft.getFactor().struct != MyTabImpl.intType)
					reporter.reportSemanticError("Factor after " + mulop + " must be int", mft.getFactor());
				
				mft.struct = MyTabImpl.noType;
				
			}
			
			else
				mft.struct = mft.getTerm().struct;
			
		}
		
		else
			mft.struct = MyTabImpl.noType;
		
	}
	
	/** SingleFactorTerm processing;
	 * <br> SingleFactorTerm ::= Factor
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SingleFactorTerm)
	 */
	public void visit (SingleFactorTerm sft) {
		
		sft.struct = sft.getFactor().struct;
		
	}
	
	/**
	 * factors
	 */
	
	/** FactorDesignator processing;
	 * <br> FactorDesignator ::= Designator
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.FactorDesignator)
	 */
	public void visit (FactorDesignator fd) {
		
		if (fd.getDesignator().obj != MyTabImpl.noObj)
			fd.struct = fd.getDesignator().obj.getType();
		
		else
			fd.struct = MyTabImpl.noType;
		
	}
	
	/** MethodCallFactor processing;
	 * <br> MethodCallFactor ::= Designator LEFT_PARENTHESIS ActPars RIGHT_PARENTHESIS
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MethodCallFactor)
	 */
	public void visit (MethodCallFactor mcf) {
		
		if (mcf.getMethodDesignator().obj != MyTabImpl.noObj) {
			
			mcf.struct = mcf.getMethodDesignator().obj.getType();
			
			ActualParameterSemanticAnalyzer actParsCounter = new ActualParameterSemanticAnalyzer(mcf.getMethodDesignator().obj, true, reporter);
			mcf.getActParsOption().traverseBottomUp(actParsCounter);
			
			if (actParsCounter.getActParsCount()
					!= mcf.getMethodDesignator().obj.getLevel())
				reporter.reportSemanticError("Numbers of actual and formal parameters do not match", mcf.getActParsOption());
			
			else {
			
				ActualParameterSemanticAnalyzer actParsAnalyzer = new ActualParameterSemanticAnalyzer(mcf.getMethodDesignator().obj, false, reporter);
				mcf.getActParsOption().traverseBottomUp(actParsAnalyzer);
			
			}
			
		}
		
		else
			mcf.struct = MyTabImpl.noType;
	}
	
	/** ConstFactor processing;
	 * <br> ConstFactor ::= ConstValue
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ConstFactor)
	 */
	public void visit (ConstFactor cf) {
		
		cf.struct = cf.getConstValue().obj.getType();
		
	}
	
	/** NewObjectFactor processing;
	 * <br> NewObjectFactor ::= NEW Type
	 * <br> Type must be Struct.Class
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.NewObjectFactor)
	 */
	public void visit (NewObjectFactor nof) {
		
		nof.struct = nof.getType().struct;
		
		if (nof.getType().struct != MyTabImpl.noType) {
			
			if (nof.getType().struct.getKind() != Struct.Class) {
				
				nof.struct = MyTabImpl.noType;
				reporter.reportSemanticError("Only classes can be instantiated", nof.getType());
				
			}
			
		}
		
	}
	
	/** NewArrayFactorNonCondExpr processing;
	 * <br> NewArrayFactorNonCondExpr ::= NEW Type LEFT_BRACKET NonCondExpr RIGHT_BRACKET
	 * <br> Expr must be Struct.Int
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.NewArrayFactor)
	 */
	public void visit (NewArrayFactorNonCondExpr naf) {
		
		if (naf.getType().struct != MyTabImpl.noType
				&& naf.getNonCondExpr().struct != MyTabImpl.noType) {
			
			if (naf.getNonCondExpr().struct.getKind() != Struct.Int) {
				
				reporter.reportSemanticError("Expression between brackets must be int", naf.getNonCondExpr());
				naf.struct = MyTabImpl.noType;
				
			}
			
			else
				naf.struct = new Struct (Struct.Array, naf.getType().struct);
			
		}
		
		else
			naf.struct = MyTabImpl.noType;
		
	}
	
	/** NewArrayFactorCondExpr processing;
	 * <br> NewArrayFactorCondExpr ::= NEW Type LEFT_BRACKET CondExpr RIGHT_BRACKET
	 * <br> Expr must be Struct.Int
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.NewArrayFactor)
	 */
	public void visit (NewArrayFactorCondExpr naf) {
		
		if (naf.getType().struct != MyTabImpl.noType
				&& naf.getCondExpr().struct != MyTabImpl.noType) {
			
			if (naf.getCondExpr().struct.getKind() != Struct.Int) {
				
				reporter.reportSemanticError("Expression between brackets must be int", naf.getCondExpr());
				naf.struct = MyTabImpl.noType;
				
			}
			
			else
				naf.struct = new Struct (Struct.Array, naf.getType().struct);
			
		}
		
		else
			naf.struct = MyTabImpl.noType;
		
	}
	
	/** CompositeFactorNonCondExpr processing;
	 * <br> CompositeFactor ::= LEFT_PARENTHESIS NonCondExpr RIGHT_PARENTHESIS
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.CompositeFactor)
	 */
	public void visit (CompositeFactorNonCondExpr cf) {
		
		cf.struct = cf.getNonCondExpr().struct;
		
	}
	
	/** CompositeFactorCondExpr processing;
	 * <br> CompositeFactor ::= LEFT_PARENTHESIS CondExpr RIGHT_PARENTHESIS
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.CompositeFactor)
	 */
	public void visit (CompositeFactorCondExpr cf) {
		
		cf.struct = cf.getCondExpr().struct;
		
	}
	
	/**
	 * designators
	 */
	
	/** MethodDesignator processing;
	 * <br> MethodDesignator ::= Designator
	 * <br> Designator must be Obj.Meth
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.MethodDesignator)
	 */
	public void visit (MethodDesignator md) {
		
		if (md.getDesignator().obj != MyTabImpl.noObj) {
			
			Obj designator = md.getDesignator().obj;
			
			if (designator.getKind() != Obj.Meth) {
				
				reporter.reportSemanticError(designator.getName() + " must be method", md.getDesignator());
				md.obj = MyTabImpl.noObj;
				
			}
			
			else 
				md.obj = designator;
			
		}
		
		else
			md.obj = MyTabImpl.noObj;
		
	}
	
	/** SimpleDesignator processing;
	 * <br> SimpleDesignator ::= IDENT
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.SimpleDesignator)
	 */
	public void visit (SimpleDesignator sd) {
		
		sd.obj = MyTabImpl.find(sd.getDesignatorName());
		
		if (sd.obj == MyTabImpl.noObj)
			reporter.reportSemanticError(sd.getDesignatorName() + " is not declared", sd);
		
		else {
			
			StringBuilder type = new StringBuilder ();
			if (sd.obj.getKind() == Obj.Con)
				type.append("global constant");
			
			else if (sd.obj.getKind() == Obj.Meth)
				type.append("method call");
			
			else if (sd.obj.getKind() == Obj.Var) {
				
				if (sd.obj.getLevel() == 0)
					type.append("global variable");
				
				else {
					
					if (sd.obj.getFpPos() == -1)
						type.append("local variable");
					
					else
						type.append("method formal parameter");
					
				}
			}
			
			reporter.reportSemanticDetection(sd, type.toString(), sd.obj);
			
		}
		
	}
	
	/** ArrayDesignatorNonCondExpr processing;
	 * <br> ArrayDesignatorNonCondExpr ::= Designator LEFT_BRACKET NonCondExpr RIGHT_BRACKET
	 * <br> Designator must be Struct.Array
	 * <br> Expr must be Struct.Int
	 * <br> Designator and Expr already processed, must check if Designator != Tab.noObj && Expr != Tab.noType
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ArrayDesignator)
	 */
	public void visit (ArrayDesignatorNonCondExpr ad) {
		
		if (ad.getDesignator().obj != MyTabImpl.noObj
				&& ad.getNonCondExpr().struct != MyTabImpl.noType) {
			
			Obj designator = ad.getDesignator().obj;
			Struct exprType = ad.getNonCondExpr().struct;
			
			if (designator.getType().getKind() != Struct.Array
					|| exprType.getKind() != Struct.Int) {
				
				if (designator.getType().getKind() != Struct.Array)
					reporter.reportSemanticError(designator.getName() + " must be array", ad.getDesignator());
				
				if (exprType.getKind() != Struct.Int)
					reporter.reportSemanticError("Expression between brackets must be int", ad.getNonCondExpr());
				
				ad.obj = MyTabImpl.noObj;
				
			}
			
			else {
				
				ad.obj = new Obj (Obj.Elem, "Elem of " + designator.getName(), 
						designator.getType().getElemType(), designator.getAdr(), designator.getLevel());
				
				reporter.reportSemanticDetection(ad, "element of array", ad.obj);
				
			}
			
		}
		
		else
			ad.obj = MyTabImpl.noObj;
		
	}
	
	/** ArrayDesignatorCondExpr processing;
	 * <br> ArrayDesignatorCondExpr ::= Designator LEFT_BRACKET CondExpr RIGHT_BRACKET
	 * <br> Designator must be Struct.Array
	 * <br> Expr must be Struct.Int
	 * <br> Designator and Expr already processed, must check if Designator != Tab.noObj && Expr != Tab.noType
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ArrayDesignator)
	 */
	public void visit (ArrayDesignatorCondExpr ad) {
		
		if (ad.getDesignator().obj != MyTabImpl.noObj
				&& ad.getCondExpr().struct != MyTabImpl.noType) {
			
			Obj designator = ad.getDesignator().obj;
			Struct exprType = ad.getCondExpr().struct;
			
			if (designator.getType().getKind() != Struct.Array
					|| exprType.getKind() != Struct.Int) {
				
				if (designator.getType().getKind() != Struct.Array)
					reporter.reportSemanticError(designator.getName() + " must be array", ad.getDesignator());
				
				if (exprType.getKind() != Struct.Int)
					reporter.reportSemanticError("Expression between brackets must be int", ad.getCondExpr());
				
				ad.obj = MyTabImpl.noObj;
				
			}
			
			else {
				
				ad.obj = new Obj (Obj.Elem, "Elem of " + designator.getName(), 
						designator.getType().getElemType(), designator.getAdr(), designator.getLevel());
			}
			
		}
		
		else
			ad.obj = MyTabImpl.noObj;
		
	}
	
	/** ClassDesignator processing;
	 * <br> ClassDesignator ::= Designator PERIOD IDENT
	 * <br> Designator must be Struct.Class
	 * <br> IDENT must be Obj.Fld or Meth
	 * @see rs.ac.bg.etf.pp1.ast.VisitorAdaptor#visit(rs.ac.bg.etf.pp1.ast.ClassDesignator)
	 */
	public void visit (ClassDesignator cd) {
		
		if (cd.getDesignator().obj != MyTabImpl.noObj) {
			
			Obj designator = cd.getDesignator().obj;
			
			if (designator.getType().getKind() != Struct.Class) {
				
				reporter.reportSemanticError(designator.getName() + " must be class", cd.getDesignator());
				
				cd.obj = MyTabImpl.noObj;
				
			}
			
			else {
				
				Struct classStruct = designator.getType();
				
				Obj objFound = classStruct.getMembersTable().searchKey(cd.getFieldOrMethodName());
				
				if (objFound == null
						|| (objFound.getKind() != Obj.Fld
							&& objFound.getKind() != Obj.Meth)) {
					
					reporter.reportSemanticError(cd.getFieldOrMethodName() + " must be class field or method", cd);
					cd.obj = MyTabImpl.noObj;
					
				}
				
				else 
					cd.obj = objFound;
				
			}
			
		}
		
		else
			cd.obj = MyTabImpl.noObj;
		
	}

}

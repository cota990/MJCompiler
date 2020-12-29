package rs.ac.bg.etf.pp1;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.visitors.DumpSymbolTableVisitor;

public class ReportHelper extends DumpSymbolTableVisitor {
	
	Logger log = Logger.getLogger (getClass());
	
	private boolean semanticErrorFound = false;
	
	public boolean isSemanticErrorFound () {
		
		return semanticErrorFound;
		
	}
	
	public void visitStructNode(Struct structToVisit) {
		
		switch (structToVisit.getKind()) {
		case Struct.None:
			output.append("notype");
			break;
		case Struct.Int:
			output.append("int");
			break;
		case Struct.Char:
			output.append("char");
			break;
		case Struct.Bool:
			output.append("bool");
			break;
		case Struct.Array:
			output.append("Arr of ");
			
			switch (structToVisit.getElemType().getKind()) {
			case Struct.None:
				output.append("notype");
				break;
			case Struct.Int:
				output.append("int");
				break;
			case Struct.Char:
				output.append("char");
				break;
			case Struct.Class:
				output.append("Class");
				break;
			}
			break;
		case Struct.Class:
			output.append("Class [");
			for (Obj obj : structToVisit.getMembers()) {
				obj.accept(this);
			}
			output.append("]");
			break;
		}

	}
	
	public String getOutput () {
		
		String ret = output.toString();
		output = new StringBuilder();
		return ret;
		
	}
	
	public void reportSemanticDeclarationStart (String description, SyntaxNode node, String symbolName) {
		
		output.
			append("Started processing declaration of ").
			append(description).
			append(" ").
			append(symbolName).
			append(" on line ").
			append(node.getLine());
		
		log.info(getOutput());
		
	}
	
	public void reportSemanticDeclaration (String description, SyntaxNode node, Obj symbolTableNode) {
		
		output.
			append("Declared ").
			append(description).
			append(" ").
			append(symbolTableNode.getName()).
			append(" on line ").
			append(node.getLine());
		
		log.info(getOutput());
		
		symbolTableNode.accept(this);
		log.info(getOutput());
	
	}
	
	public void reportSemanticDetection (SyntaxNode node, Obj symbolTableNode) {
		
		output.
			append("Detected usage of symbol: ").
			append(symbolTableNode.getName()).
			append(" on line ").
			append(node.getLine());
		
		log.info(getOutput());
		
		symbolTableNode.accept(this);
		log.info(getOutput());
	
	}
	
	public void reportSemanticError (String message, SyntaxNode node) {
		
		output.append("Semantic error");
		
		if (node != null 
				&& node.getLine() != 0) {
			
			output.
				append(" on line ").
				append(node.getLine());
		
		}
		
		output.
			append(": ").
			append(message);
		
		log.error(getOutput());
		
		semanticErrorFound = true;
		
	}

}

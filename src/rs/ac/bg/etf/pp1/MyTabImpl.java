package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Scope;
import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.visitors.SymbolTableVisitor;

public class MyTabImpl extends Tab {
	
	// standardni tipovi
	public static final Struct boolType = new Struct(Struct.Bool);
	
	private static int currentLevel; // nivo ugnezdavanja tekuceg opsega
	
	public static void init() {
		Scope universe = currentScope = new Scope(null);
		
		universe.addToLocals(new Obj(Obj.Type, "int", intType));
		universe.addToLocals(new Obj(Obj.Type, "char", charType));
		universe.addToLocals(new Obj(Obj.Type, "bool", boolType));
		universe.addToLocals(new Obj(Obj.Con, "eol", charType, 10, 0));
		universe.addToLocals(new Obj(Obj.Con, "null", nullType, 0, 0));
		
		universe.addToLocals(chrObj = new Obj(Obj.Meth, "chr", charType, 0, 1));
		{
			openScope();
			currentScope.addToLocals(new Obj(Obj.Var, "i", intType, 0, 1));
			chrObj.setLocals(currentScope.getLocals());
			closeScope();
		}
		
		universe.addToLocals(ordObj = new Obj(Obj.Meth, "ord", intType, 0, 1));
		{
			openScope();
			currentScope.addToLocals(new Obj(Obj.Var, "ch", charType, 0, 1));
			ordObj.setLocals(currentScope.getLocals());
			closeScope();
		} 
		
		
		universe.addToLocals(lenObj = new Obj(Obj.Meth, "len", intType, 0, 1));
		{
			openScope();
			currentScope.addToLocals(new Obj(Obj.Var, "arr", new Struct(Struct.Array, noType), 0, 1));
			lenObj.setLocals(currentScope.getLocals());
			closeScope();
		}
		
		currentLevel = -1;
	
	}
	
	public static int getCurrentLevel () {
		
		return currentLevel;
		
	}
	
	/**
	 * Otvaranje novog opsega
	 */
	public static void openScope() {
		currentScope = new Scope(currentScope);
		currentLevel++;
	}

	/**
	 * Zatvaranje opsega
	 */
	public static void closeScope() {
		currentScope = currentScope.getOuter();
		currentLevel--;
	}
	
	/**
	 * Pravi se novi Obj cvor sa prosledjenim atributima kind, name i type, pa se
	 * zatim ubacuje u tabelu simbola. Povratna vrednost: - novostvoreni cvor, ako
	 * cvor sa tim imenom nije vec postojao u tabeli simbola. - postojeci cvor iz
	 * tabele simbola, ako je doslo do greske jer smo pokusali da u tabelu simbola
	 * za opseg ubacimo cvor sa imenom koje vec postoji.
	 */
	public static Obj insert(int kind, String name, Struct type) {
		// create a new Object node with kind, name, type
		Obj newObj = new Obj(kind, name, type, 0, ((currentLevel != 0)? 1 : 0)); 
		
		// append the node to the end of the symbol list
		if (!currentScope.addToLocals(newObj)) {
			Obj res = currentScope.findSymbol(name);
			return (res != null) ? res : noObj;
		}
		else 
			return newObj;
	}
	
	public static void dump(SymbolTableVisitor stv) {
		System.out.println("=====================SYMBOL TABLE DUMP=========================");
		if (stv == null)
			stv = new MyDumpSymbolTableVisitor();
		for (Scope s = currentScope; s != null; s = s.getOuter()) {
			s.accept(stv);
		}
		System.out.println(stv.getOutput());
	}
	
	public static void dump () {
		
		dump (null);
		
	}

}

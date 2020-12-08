package rs.ac.bg.etf.pp1;

/**
 * user defined class for lexical analysis generator
 *
 */
public class sym_old {
	
	// keywords
	
	public static final int PROGRAM = 1;
	public static final int BREAK = 2;
	public static final int CLASS = 3;
	public static final int ENUM = 4;
	public static final int ELSE = 5;
	public static final int CONST = 6;
	public static final int IF = 7;
	public static final int SWITCH = 8;
	public static final int DO = 9;
	public static final int WHILE = 10;
	public static final int NEW = 11;
	public static final int PRINT = 12;
	public static final int READ = 13;
	public static final int RETURN = 14;
	public static final int VOID = 15;
	public static final int EXTENDS = 16;
	public static final int CONTINUE = 17;
	public static final int CASE = 18;
	
	// identifiers
	
	public static final int IDENT = 19;
	
	// constants
	
	public static final int NUMBER = 20;
	public static final int CHAR = 21;
	public static final int BOOL = 22;
	
	// operators
	
	public static final int ADD = 23;
	public static final int SUB = 24;
	public static final int MUL = 25; 
	public static final int DIV = 26;
	public static final int MOD = 27;
	public static final int EQUALS = 28;
	public static final int NOT_EQUALS = 29;
	public static final int GREATER_THAN = 30;
	public static final int GREATER_THAN_EQUALS = 31;
	public static final int LESS_THAN = 32;
	public static final int LESS_THAN_EQUALS = 33;
	public static final int AND = 34;
	public static final int OR = 35;
	public static final int ASSIGN = 36;
	public static final int INC = 37;
	public static final int DEC = 38;
	public static final int SEMICOLON = 39;
	public static final int COMMA = 40;
	public static final int PERIOD = 41;
	public static final int LEFT_PARENTHESIS = 42;
	public static final int RIGHT_PARENTHESIS = 43;
	public static final int LEFT_BRACKET = 44;
	public static final int RIGHT_BRACKET = 45;
	public static final int LEFT_BRACE = 46;
	public static final int RIGHT_BRACE = 47;
	public static final int QUESTION_MARK = 48;
	public static final int COLON = 49;
	
	// eof symbol
	
	public static final int EOF = -1;

}

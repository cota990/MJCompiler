package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

// cup compatible, includes info about lines and columns

%cup
%line
%column

// specific state for processing comments

%xstate COMMENT

// end-of-file method

%eofval{
	
	return newSymbol(sym.EOF);
	
%eofval}

%{

	// utility method for token creation
	// args : type - sym.java constant
	private Symbol newSymbol(int type) {
		
		return new Symbol(type, yyline+1, yycolumn);
		
	}
	
	// utility method for token creation
	// args : type - sym.java constant
	//	      value - Object connected with token (types: Integer for number, Character for char, Boolean for bool, String for keywords, identifiers and operators)
	private Symbol newSymbol(int type, Object value) {
		
		return new Symbol(type, yyline+1, yycolumn, value);
	
	}

%}

%%

// ignore whitespaces

" " 	{ }
"\b" 	{ }
"\t" 	{ }
"\r\n" 	{ }
"\f" 	{ }

// keywords

"program"   { return newSymbol(sym.PROGRAM, yytext());  }
"break"     { return newSymbol(sym.BREAK, yytext());    }
"class"     { return newSymbol(sym.CLASS, yytext());    }
"enum"      { return newSymbol(sym.ENUM, yytext());     }
"else"      { return newSymbol(sym.ELSE, yytext());     }
"const"     { return newSymbol(sym.CONST, yytext());    }
"if"        { return newSymbol(sym.IF, yytext());       }
"switch"    { return newSymbol(sym.SWITCH, yytext());   }
"do"        { return newSymbol(sym.DO, yytext());       }
"while"     { return newSymbol(sym.WHILE, yytext());    }
"new"       { return newSymbol(sym.NEW, yytext());      }
"print"     { return newSymbol(sym.PRINT, yytext());    }
"read"      { return newSymbol(sym.READ, yytext());     }
"return"    { return newSymbol(sym.RETURN, yytext());   }
"void"      { return newSymbol(sym.VOID, yytext());     }
"extends"   { return newSymbol(sym.EXTENDS, yytext());  }
"continue"  { return newSymbol(sym.CONTINUE, yytext()); }
"case"      { return newSymbol(sym.CASE, yytext());     }

// operators

"+"         { return newSymbol(sym.ADD, yytext());                 }
"-"         { return newSymbol(sym.SUB, yytext());                 }
"*"         { return newSymbol(sym.MUL, yytext());                 }
"/"         { return newSymbol(sym.DIV, yytext());                 }
"%"         { return newSymbol(sym.MOD, yytext());                 }
"=="        { return newSymbol(sym.EQUALS, yytext());              }
"!="        { return newSymbol(sym.NOT_EQUALS, yytext());          }
">"         { return newSymbol(sym.GREATER_THAN, yytext());        }
">="        { return newSymbol(sym.GREATER_THAN_EQUALS, yytext()); }
"<"         { return newSymbol(sym.LESS_THAN, yytext());           }
"<="        { return newSymbol(sym.LESS_THAN_EQUALS, yytext());    }
"&&"        { return newSymbol(sym.AND, yytext());                 }
"||"        { return newSymbol(sym.OR, yytext());                  }
"="         { return newSymbol(sym.ASSIGN, yytext());              }
"++"        { return newSymbol(sym.INC, yytext());                 }
"--"        { return newSymbol(sym.DEC, yytext());                 }
";"         { return newSymbol(sym.SEMICOLON, yytext());           }
","         { return newSymbol(sym.COMMA, yytext());               }
"."         { return newSymbol(sym.PERIOD, yytext());              }
"("         { return newSymbol(sym.LEFT_PARENTHESIS, yytext());    }
")"         { return newSymbol(sym.RIGHT_PARENTHESIS, yytext());   }
"["         { return newSymbol(sym.LEFT_BRACKET, yytext());        }
"]"         { return newSymbol(sym.RIGHT_BRACKET, yytext());       }
"{"         { return newSymbol(sym.LEFT_BRACE, yytext());          }
"}"         { return newSymbol(sym.RIGHT_BRACE, yytext());         }
"?"         { return newSymbol(sym.QUESTION_MARK, yytext());       }
":"         { return newSymbol(sym.COLON, yytext());               }

// constants

[0-9]+              { return newSymbol(sym.NUMBER, new Integer (yytext()));           }
"\'"[\x20-\x7E]"\'" { return newSymbol(sym.CHAR, new Character (yytext().charAt(1))); }
"true"              { return newSymbol(sym.BOOL, new Boolean (true));                 }
"false"             { return newSymbol(sym.BOOL, new Boolean (false));                }

// identifiers

([a-z]|[A-Z])[a-z|A-Z|0-9|_]* 	{ return newSymbol (sym.IDENT, yytext()); }

// comments

"//" 		     { yybegin(COMMENT);   }
<COMMENT> .      { yybegin(COMMENT);   }
<COMMENT> "\r\n" { yybegin(YYINITIAL); }

// errors

. { System.err.println("Lexical error:  (" + yytext() + ") at line: " + (yyline + 1) + " column: " + yycolumn); }
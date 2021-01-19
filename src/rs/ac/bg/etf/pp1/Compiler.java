package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;

import java_cup.runtime.Symbol;
import rs.ac.bg.etf.pp1.ast.Program;
import rs.etf.pp1.mj.runtime.Code;

public class Compiler {
	
	public static void tsdump() {
		
		MyTabImpl.dump();
		
	}
	
	public static void main(String[] args) throws Exception {
		
		Reader br = null;
		
		//redirect streams if provided
		
		//redirect streams
		PrintStream stdout = System.out;
		PrintStream stderr = System.err;
		
		if (args.length < 2) {
			
			System.err.println("Illegal number of parameters: <input_file> <output_obj_file> <optional_system_out_file> <optional_system_err_file>");
			return;
			
		}
		
		if (args.length > 2) {
			
			if (args.length > 3) {
				
				File stdOutFile = new File (args[2]), stdErrFile = new File(args[3]);
				if (stdOutFile.exists()) stdOutFile.delete();
				if (stdErrFile.exists()) stdErrFile.delete();
				
				System.setOut(new PrintStream(stdOutFile));
				System.setErr(new PrintStream(stdErrFile));
			
			}
			
			else {
				
				File stdOutFile = new File (args[2]);
				if (stdOutFile.exists()) stdOutFile.delete();
				System.setOut(new PrintStream(stdOutFile));
				
			}
		
		}
		
		try {
			
			File sourceCode = new File(args[0]);		
			System.out.println("Compiling source file: " + sourceCode.getAbsolutePath());
			
			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);
			
			System.out.println("Scaning: ");
			
			// start parsing
			
			MJParser parser = new MJParser(lexer);
			Symbol symbol = parser.parse();
			
			System.out.println("Finished scaning");
			
			if (lexer.errorFound)
				System.out.println("Lexical errors found!");
			
			else
				System.out.println("No lexical errors found!");
			
			if (parser.syntaxErrorFound()) {
				
				System.err.println("Errors found in syntax analysis: ");
				//log.info("NumOfErrors: " + parser.getErrorMessages().size());
				//log.info("NumOfDescriptions: " + parser.getErrorDetailedDescriptions().size());
				
				if (parser.getErrorMessages().size() != parser.getErrorDetailedDescriptions().size())
					System.err.println("Number of errors does not match number of descriptions!!");
				
				else {
					
					for (int i = 0; i < parser.getErrorMessages().size(); i++) {
						
						StringBuilder errorBuilder = new StringBuilder ();
						errorBuilder.append(parser.getErrorMessages().get(i))
									.append(": ")
									.append(parser.getErrorDetailedDescriptions().get(i));
						
						System.err.println(errorBuilder.toString());
						
					}
						
				}
				
				System.out.println("Syntax errors found!");
				
			}
			
			else System.out.println("No syntax errors found!");
			
			System.out.println("===============================================================");
			
			if (!lexer.errorFound
					&& !parser.syntaxErrorFound()) {
				
				System.err.println("No lexical errors found!");
				System.err.println("No syntax errors found!");
				
			}
			
			System.err.println("===============================================================");
			
			// root of syntax tree, used for semantic analysis and code generation
			
			Program program = (Program)(symbol.value);
			
			// syntax tree output
			
			System.out.println("Syntax tree output: ");
			System.out.println(program.toString("   "));
			
			System.out.println("===============================================================");
			
			// begin semantic analysis; initialize symbol table; after analysis, check for main found function
			
			System.out.println("Semantic analysis: ");
			
			MyTabImpl.init();
			
			SemanticAnalyzer analyzer = new SemanticAnalyzer();
			program.traverseBottomUp(analyzer); 
			
			System.out.println("Finished semantic analysis");
			
			if (analyzer.isSemanticErrorFound())
				System.out.println("Semantic errors found!");
			
			else {
				
				System.err.println("No semantic errors found!");
				System.out.println("No semantic errors found!");
				
			}

			System.err.println("===============================================================");
			System.out.println("===============================================================");
			
			tsdump();
			
			// if no lexical, syntax and semantic errors are found, proceed to code generation
			
			if (!lexer.errorFound
					&& !parser.syntaxErrorFound()
						&& !analyzer.isSemanticErrorFound()) {
				
				File objectFile = new File (args[1]);
				if (objectFile.exists()) objectFile.delete();
				
				System.out.println("Generating code...");
				
				CodeGenerator cg = new CodeGenerator();
				
				// begin code generation
				
				program.traverseBottomUp(cg);
				
				Code.dataSize = cg.getnVars();
				Code.mainPc = cg.getMainPC();
				Code.write(new FileOutputStream(objectFile));
				
				System.out.println("Code successfully generated. Executive file: " + objectFile.getAbsolutePath());
								
			}
			
			else {
				
				System.out.println("Errors found; no code generated!");
				System.err.println("Errors found; no code generated!");
				
			}
			
			System.out.println("Compilation completed!");
			
		} finally {
			
			if (br != null) 
				try { 
				
					br.close(); 
					
				} 
			
				catch (IOException e) { 
					
					System.err.println(e.getMessage()); 
					
				}
		
		}
		
		// reset streams
		
		System.setOut(stdout);
		System.setErr(stderr);
		
	}

}

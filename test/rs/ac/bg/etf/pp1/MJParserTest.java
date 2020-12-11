package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import java_cup.runtime.Symbol;
import rs.ac.bg.etf.pp1.util.Log4JUtils;

public class MJParserTest {
	
	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	public static void main(String[] args) throws Exception {
		
		Logger log = Logger.getLogger(MJLexerTest.class);
		Reader br = null;
		
		File sourceCode = new File("test/program.mj");	
		log.info("Compiling source file: " + sourceCode.getAbsolutePath());
		
		br = new BufferedReader(new FileReader(sourceCode));
		
		Yylex lexer = new Yylex(br);
		
		MJParser parser = new MJParser(lexer);
		
		try {
			
			Symbol symbol = parser.parse();
			
			Program prog = (Program)(symbol.value); 
			
			if (lexer.errorFound)
				log.info("Errors found in lexical analysis");
			
			else
				log.info("No errors found in lexical analysis");
			
			if (parser.syntaxErrorFound()) {
				
				log.error("Errors found in syntax analysis: ");
				//log.info("NumOfErrors: " + parser.getErrorMessages().size());
				//log.info("NumOfDescriptions: " + parser.getErrorDetailedDescriptions().size());
				
				if (parser.getErrorMessages().size() != parser.getErrorDetailedDescriptions().size())
					log.info("Number of errors does not match number of descriptions!!");
				
				else {
					
					for (int i = 0; i < parser.getErrorMessages().size(); i++) {
						
						StringBuilder errorBuilder = new StringBuilder ();
						errorBuilder.append(parser.getErrorMessages().get(i))
									.append(": ")
									.append(parser.getErrorDetailedDescriptions().get(i));
						
						log.error(errorBuilder.toString());
						
					}
						
				}
				
				log.info("Errors found in syntax analysis");
				
			}
			
			else log.info("No errors found in syntax analysis");
			
			log.info(prog.toString("    "));
		
		} catch (Exception e) {
			
			log.info("PARSING INTERRUPTED");
			log.info(e.getMessage());
			
		}
		
	}

}

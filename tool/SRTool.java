package tool;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import parser.SimpleCLexer;
import parser.SimpleCParser;
import parser.SimpleCParser.ProcedureDeclContext;
import parser.SimpleCParser.ProgramContext;

import java.io.FileInputStream;
import java.io.IOException;

public class SRTool {

    private static final int POOL_SIZE = 2;

	public static void main(String[] args) throws IOException, InterruptedException {
        String filename = args[0];
		ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(filename));
        SimpleCLexer lexer = new SimpleCLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		SimpleCParser parser = new SimpleCParser(tokens);
		ProgramContext ctx = parser.program();
		if(parser.getNumberOfSyntaxErrors() > 0) {
			System.exit(1);
		}
		Typechecker tc = new Typechecker();
		tc.visit(ctx);
		tc.resolve();
		if(tc.hasErrors()) {
			System.err.println("Errors were detected when typechecking " + filename + ":");
			for(String err : tc.getErrors()) {
				System.err.println("  " + err);
			}
			System.exit(1);
		}

        // prepare proc details
        Scopes globalScope = new Scopes();
        globalScope.openScope();
        tc.getGlobals().forEach(globalScope::add);
        SummarisationVisitor summarisationVisitor = new SummarisationVisitor(globalScope);

        for(ProcedureDeclContext proc : ctx.procedures) {
            summarisationVisitor.visit(proc);
        }

        // Verify
        HoudiniVerifier verifier = new HoudiniVerifier(POOL_SIZE, tc.getGlobals(),
                summarisationVisitor.getProcDetails());

        System.out.println(verifier.verify());
        System.exit(0);
    }
}

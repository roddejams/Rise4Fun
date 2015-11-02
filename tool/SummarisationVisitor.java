package tool;

import parser.SimpleCBaseVisitor;
import parser.SimpleCParser;
import parser.SimpleCParser.AssignStmtContext;
import parser.SimpleCParser.EnsuresContext;
import parser.SimpleCParser.ProcedureDeclContext;
import parser.SimpleCParser.RequiresContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SummarisationVisitor extends SimpleCBaseVisitor<Void>
{
    private final Map<String, ProcDetail> procDetails;
    private final Set<String> globals;
    private ProcDetail detail;

    public SummarisationVisitor(Set<String> globals) {
        procDetails = new HashMap<>();
        this.globals = globals;
    }

    @Override
    public Void visitProcedureDecl(ProcedureDeclContext ctx) {
        detail = new ProcDetail();
        for (SimpleCParser.PrepostContext preCond : ctx.contract) {
            if (preCond.requires() != null) {
                detail.addPreCond(preCond.requires());
            }
        }

        for (SimpleCParser.PrepostContext postCond : ctx.contract) {
            if (postCond.ensures() != null) {
                detail.addPostCond(postCond.ensures());
            }
        }

        for(SimpleCParser.FormalParamContext param : ctx.formals) {
            detail.addArgument(param.ident.getText());
        }

        for(SimpleCParser.StmtContext stmt : ctx.stmts) {
            visit(stmt);
        }

        procDetails.put(ctx.name.getText(), detail);
        return null;
    }

    //calculate modset - can only contain global variables
    @Override
    public Void visitAssignStmt(AssignStmtContext ctx) {
        String varName = ctx.lhs.ident.getText();

        if(globals.contains(varName)) {
            detail.addToModset(ctx.lhs.ident.getText());
        }

        return null;
    }

    public Map<String, ProcDetail> getProcDetails() {
        return procDetails;
    }
}

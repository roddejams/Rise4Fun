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

    public SummarisationVisitor(Set<String> globals) {
        procDetails = new HashMap<>();
        this.globals = globals;
    }


    @Override
    public Void visitProcedureDecl(ProcedureDeclContext ctx) {

        for (SimpleCParser.PrepostContext preCond : ctx.contract) {
            if (preCond.requires() != null) {
                visitRequires(preCond.requires());
            }
        }

        for (SimpleCParser.PrepostContext postCond : ctx.contract) {
            if (postCond.ensures() != null) {
                visitEnsures(postCond.ensures());
            }
        }

        return null;
    }

    @Override
    public Void visitRequires(RequiresContext ctx) {


        return null;
    }

    @Override
    public Void visitEnsures(EnsuresContext ctx) {
        return null;
    }

    @Override
    public Void visitAssignStmt(AssignStmtContext ctx) {
        return null;
    }

    public Map<String, ProcDetail> getProcDetails() {
        return procDetails;
    }
}

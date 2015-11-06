package tool;

import parser.SimpleCParser;
import parser.SimpleCParser.ProcedureDeclContext;

import java.util.HashMap;
import java.util.Map;

public class SummarisationVisitor extends ModsetCalculatorVisitor {
    private ProcDetail detail;

    public SummarisationVisitor(Scopes globals) {
        super(globals, new HashMap<>());
        //Scopes initalScope = new Scopes();
        //globals.forEach(name -> initalScope.add(name));
    }

    @Override
    public Void visitProcedureDecl(ProcedureDeclContext ctx) {
        detail = new ProcDetail();

        internalScopes.openScope();

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

        ctx.stmts.forEach(this::visit);

        internalScopes.closeScope();

        procDetails.put(ctx.name.getText(), detail);
        return null;
    }

    @Override
    public Void visitCallStmt(SimpleCParser.CallStmtContext ctx) {
        detail.addCalledProc(ctx.callee.getText());
        return null;
    }

    public Map<String, ProcDetail> getProcDetails() {
        return procDetails;
    }
}

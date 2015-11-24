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
        detail = new ProcDetail(ctx);

        internalScopes.openScope();

        ctx.contract.forEach(this::visit);

        for(SimpleCParser.FormalParamContext param : ctx.formals) {
            detail.addArgument(param.ident.getText());
        }

        ctx.stmts.forEach(this::visit);

        internalScopes.closeScope();

        modset.forEach(detail::addToModset);
        procDetails.put(ctx.name.getText(), detail);
        return null;
    }

    @Override
    public Void visitRequires(SimpleCParser.RequiresContext ctx) {
        detail.addPreCond(ctx);
        return null;
    }

    @Override
    public Void visitEnsures(SimpleCParser.EnsuresContext ctx) {
        detail.addPostCond(ctx);
        return null;
    }

    @Override
    public Void visitCandidateRequires(SimpleCParser.CandidateRequiresContext ctx) {
        detail.addCandidatePrecond(ctx);
        return null;
    }

    @Override
    public Void visitCandidateEnsures(SimpleCParser.CandidateEnsuresContext ctx) {
        detail.addCandidatePostcond(ctx);
        return null;
    }

    @Override
    public Void visitCallStmt(SimpleCParser.CallStmtContext ctx) {
        super.visitCallStmt(ctx);

        detail.addCalledProc(ctx.callee.getText());
        return null;
    }

    @Override
    public Void visitCandidateInvariant(SimpleCParser.CandidateInvariantContext ctx) {
        detail.addCandidateInvariant(ctx);
        return null;
    }

    @Override
    public Void visitWhileStmt(SimpleCParser.WhileStmtContext ctx) {
        super.visitWhileStmt(ctx);

        if (ctx.invariantAnnotations.isEmpty()) {
            detail.checkWithBMC(ctx, 1);
        }
        return null;
    }

    public Map<String, ProcDetail> getProcDetails() {
        return procDetails;
    }
}

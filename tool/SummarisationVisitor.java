package tool;

import parser.SimpleCBaseVisitor;
import parser.SimpleCParser;
import parser.SimpleCParser.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SummarisationVisitor extends SimpleCBaseVisitor<Void>
{
    private final Map<String, ProcDetail> procDetails;
    private final Set<String> globals;
    private ProcDetail detail;
    private final Scopes scopes;

    public SummarisationVisitor(Set<String> globals) {
        procDetails = new HashMap<>();
        this.globals = globals;
        scopes = new Scopes();
    }

    @Override
    public Void visitVarDecl(VarDeclContext ctx) {
        scopes.add(ctx.ident.getText());
        return super.visitVarDecl(ctx);
    }

    @Override
    public Void visitProcedureDecl(ProcedureDeclContext ctx) {
        detail = new ProcDetail(ctx);

        scopes.openScope();

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

        scopes.closeScope();

        procDetails.put(ctx.name.getText(), detail);
        return null;
    }

    //calculate modset - can only contain global variables
    @Override
    public Void visitAssignStmt(AssignStmtContext ctx) {
        String varName = ctx.lhs.ident.getText();

        if(scopes.getVariable(varName) == null && globals.contains(varName)) {
            detail.addToModset(varName);
        }

        return null;
    }

    //add havoc'd globals to modeset
    @Override
    public Void visitHavocStmt(SimpleCParser.HavocStmtContext ctx) {
        String varName = ctx.var.ident.getText();

        if(scopes.getVariable(varName) == null && globals.contains(varName)) {
            detail.addToModset(varName);
        }

        return null;
    }

    @Override
    public Void visitCallStmt(SimpleCParser.CallStmtContext ctx) {
        detail.addCalledProc(ctx.callee.getText());
        return null;
    }

    @Override
    public Void visitIfStmt(IfStmtContext ctx) {

        scopes.openScope();
        visit(ctx.thenBlock);
        scopes.closeScope();

        if (ctx.elseBlock != null) {
            scopes.openScope();
            visit(ctx.elseBlock);
            scopes.closeScope();
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(WhileStmtContext ctx) {
        // TODO
        return super.visitWhileStmt(ctx);
    }

    @Override
    public Void visitBlockStmt(BlockStmtContext ctx) {
        scopes.openScope();
        visit(ctx.stmt);
        scopes.closeScope();
        return null;
    }

    public Map<String, ProcDetail> getProcDetails() {
        return procDetails;
    }
}

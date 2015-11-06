package tool;

import parser.SimpleCBaseVisitor;
import parser.SimpleCParser;
import parser.SimpleCParser.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ModsetCalculatorVisitor extends SimpleCBaseVisitor<Void> {

    private final Set<String> modset;

    private final Scopes initialScope;
    protected final Scopes internalScopes;

    protected final Map<String, ProcDetail> procDetails;

    public ModsetCalculatorVisitor(Scopes initialScope, Map<String, ProcDetail> procDetails) {
        this.initialScope = initialScope;
        this.procDetails = procDetails;
        modset = new HashSet<>();
        internalScopes = new Scopes();
    }

    @Override
    public Void visitProcedureDecl(ProcedureDeclContext ctx) {
        internalScopes.openScope();
        ctx.stmts.forEach(this::visit);
        internalScopes.closeScope();
        return null;
    }

    @Override
    public Void visitVarDecl(VarDeclContext ctx) {
        internalScopes.add(ctx.ident.getText());
        return super.visitVarDecl(ctx);
    }

    @Override
    public Void visitAssignStmt(AssignStmtContext ctx) {
        String varName = ctx.lhs.ident.getText();

        if(initialScope.getVariable(varName) != null && internalScopes.getVariable(varName) == null) {
            modset.add(varName);
        }

        return null;
    }

    //add havoc'd globals to modeset
    @Override
    public Void visitHavocStmt(SimpleCParser.HavocStmtContext ctx) {
        String varName = ctx.var.ident.getText();

        if(initialScope.getVariable(varName) != null && internalScopes.getVariable(varName) == null) {
            modset.add(varName);
        }

        return null;
    }

    @Override
    public Void visitCallStmt(CallStmtContext ctx) {
        modset.addAll(procDetails.get(ctx.callee.getText()).getModset());
        return null;
    }

    @Override
    public Void visitIfStmt(IfStmtContext ctx) {
        internalScopes.openScope();
        visit(ctx.thenBlock);
        internalScopes.closeScope();

        if (ctx.elseBlock != null) {
            internalScopes.openScope();
            visit(ctx.elseBlock);
            internalScopes.closeScope();
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(WhileStmtContext ctx) {
        internalScopes.openScope();
        visit(ctx.body);
        internalScopes.closeScope();
        return null;
    }

    @Override
    public Void visitBlockStmt(BlockStmtContext ctx) {
        internalScopes.openScope();
        visit(ctx.stmt);
        internalScopes.closeScope();
        return null;
    }

    public Set<String> getModset() {
        return modset;
    }
}

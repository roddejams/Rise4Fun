package tool;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import parser.SimpleCBaseVisitor;
import parser.SimpleCParser;
import parser.SimpleCParser.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class SMTGeneratorVisitor extends SimpleCBaseVisitor<String> {

    private final Stack<String> predicates;

    private boolean checkingIfLogicExpr;
    private final Stack<Boolean> isLogicExpr;

    private boolean checkingIfIntegerExpr;
    private final Stack<Boolean> isIntegerExpr;

    private boolean inCallSummarisation;
    private Map<String, String> functionArgumentMap = new HashMap<>();
    private String functionReturnTemp;

    private String result = "";
    private boolean resultIsLogicExpr;

    private static final Map<String, String> smtBinFuncs = new HashMap<>();

    static {
        // fill in binMap
        smtBinFuncs.put("||", "or %s");
        smtBinFuncs.put("&&", "and %s");
        smtBinFuncs.put("|", "bvor %s");
        smtBinFuncs.put("^", "bvxor %s");
        smtBinFuncs.put("&", "bvand %s");
        smtBinFuncs.put("==", "= %s");
        smtBinFuncs.put("!=", "not (= %s");
        smtBinFuncs.put("<", "bvslt %s");
        smtBinFuncs.put("<=", "bvsle %s");
        smtBinFuncs.put(">", "bvsgt %s");
        smtBinFuncs.put(">=", "bvsge %s");
        smtBinFuncs.put("<<", "ite (bvsgt %s (_ bv32 32)) %s (bvshl %s %s)");
        smtBinFuncs.put(">>", "ite (bvsgt %s (_ bv32 32)) %s (bvashr %s %s)");
        smtBinFuncs.put("+", "bvadd %s");
        smtBinFuncs.put("-", "bvsub %s");
        smtBinFuncs.put("*", "bvmul %s");
        smtBinFuncs.put("/", "ite (= %s (_ bv0 32)) %s (bvsdiv %s %s)");
        smtBinFuncs.put("%", "ite (= %s (_ bv0 32)) %s (bvsmod %s %s)");
    }

    private Map<String, Integer> mapping = new HashMap<>();
    private final Map<String, Integer> fresh = new HashMap<>();
    private Map<String, Integer> oldGlobals = new HashMap<>();
    private final Set<String> globals;
    private final Set<String> asserts;
    private final Set<String> assumptions;
    private final Scopes scopes;
    private final Map<String, ProcDetail> procDetails;
    private final Set<String> invariants;

    public SMTGeneratorVisitor(Set<String> globals, Map<String, ProcDetail> procDetails) {
        this.globals = globals;
        this.procDetails = procDetails;

        this.asserts = new HashSet<>();
        invariants = new HashSet<>();

        this.assumptions = new HashSet<>();
        assumptions.add("true");

        isLogicExpr = new Stack<>();
        isIntegerExpr = new Stack<>();

        predicates = new Stack<>();
        predicates.push("true");

        scopes = new Scopes();
        scopes.openScope();
    }

    private Integer fresh(String varName) {
        if (!fresh.containsKey(varName)) {
            fresh.put(varName, 0);
        }
        Integer currentVal = fresh.get(varName);
        fresh.put(varName, currentVal + 1);
        return currentVal;
    }

    private String visitIntegerExpr(ParserRuleContext ctx) {
        checkingIfIntegerExpr = true;
        String visitedExpr = visit(ctx);
        return isIntegerExpr.pop() ? visitedExpr : String.format("(tobv32 %s)", visitedExpr);
    }

    private String visitLogicalExpr(ParserRuleContext ctx) {
        checkingIfLogicExpr = true;
        String visitedExpr = visit(ctx);
        return isLogicExpr.pop() ? visitedExpr : String.format("(tobool %s)", visitedExpr);
    }

    private void checkIntegerReturnType() {
        if (checkingIfLogicExpr) {
            isLogicExpr.push(false);
            checkingIfLogicExpr = false;
        }
        if (checkingIfIntegerExpr) {
            isIntegerExpr.push(true);
            checkingIfIntegerExpr = false;
        }
    }

    private void checkLogicalReturnType() {
        if (checkingIfLogicExpr) {
            isLogicExpr.push(true);
            checkingIfLogicExpr = false;
        }
        if (checkingIfIntegerExpr) {
            isIntegerExpr.push(false);
            checkingIfIntegerExpr = false;
        }
    }

    private String generateExpr(List<? extends ParserRuleContext> ctxs, List<Token> ops) {
        List<String> opstrs = ops.stream().map(Token::getText).collect(Collectors.toList());
        String expr = "";

        String lhs;
        String rhs;

        int opIdx = ops.size() - 1;

        if(ctxs.size() > 2) {
            lhs = generateExpr(ctxs.subList(0, ctxs.size()-1), ops.subList(0, opIdx));
            rhs = visitIntegerExpr(ctxs.get(ctxs.size() - 1));
        } else {
            lhs = visitIntegerExpr(ctxs.get(0));
            rhs = visitIntegerExpr(ctxs.get(1));
        }

        expr += "(" + String.format(smtBinFuncs.get(opstrs.get(ops.size()-1)), lhs) + " " + rhs + ")";

        if(isNotExpression(opstrs.get(ops.size()-1))) {
            expr += ")";
        }

        return expr;
    }

    private String generateRelExpr(List<? extends ParserRuleContext> ctxs, List<Token> ops) {
        List<String> opstrs = ops.stream().map(Token::getText).collect(Collectors.toList());
        String expr = "";

        String lhs;
        String rhs;

        int opIdx = ops.size() - 1;

        if(ctxs.size() > 2) {
            lhs = generateRelExpr(ctxs.subList(0, ctxs.size() - 1), ops.subList(0, opIdx));
            lhs = String.format("(tobv32 %s)", lhs);
            rhs = visitIntegerExpr(ctxs.get(ctxs.size() - 1));
        } else {
            lhs = visitIntegerExpr(ctxs.get(0));
            rhs = visitIntegerExpr(ctxs.get(1));
        }

        expr += "(" + String.format(smtBinFuncs.get(opstrs.get(ops.size()-1)), lhs) + " " + rhs + ")";

        return expr;
    }

    private String generateLogicalExpr(List<? extends ParserRuleContext> ctxs, List<Token> ops) {
        List<String> opstrs = ops.stream().map(Token::getText).collect(Collectors.toList());
        String expr = "";

        String lhs;
        String rhs;

        int opIdx = ops.size() - 1;

        if(ctxs.size() > 2) {
            lhs = generateLogicalExpr(ctxs.subList(0, ctxs.size() - 1), ops.subList(0, opIdx));
            rhs = visitLogicalExpr(ctxs.get(ctxs.size() - 1));
        } else {
            lhs = visitLogicalExpr(ctxs.get(0));
            rhs = visitLogicalExpr(ctxs.get(1));
        }

        expr += "(" + String.format(smtBinFuncs.get(opstrs.get(ops.size()-1)), lhs) + " " + rhs + ")";

        if(isNotExpression(opstrs.get(ops.size()-1))) {
            expr += ")";
        }
        return expr;
    }

    private boolean isNotExpression(String expr) {
        return expr.equals("!=");
    }

    private String getScopeFreeVar(String varName) {
        int varId = fresh(varName);
        mapping.put(varName, varId);
        return "(declare-fun " + varName + varId + " () (_ BitVec 32))\n";
    }

    private String declareVar(String var) {
        String varName = scopes.add(var);
        int varId = fresh(varName);
        mapping.put(varName, varId);
        return "(declare-fun " + varName + varId + " () (_ BitVec 32))\n";
    }

    private String generateAsserts() {
        if (!asserts.isEmpty()) {
            String expr = "(assert (not \n \t";
            String assertCheckDefs = "";
            String assertChecks = "";
            String assertCheckGets = "";

            int idx = 0;
            int numAnds = asserts.size() - 1;

            int i = 0;

            //Check for global variables named assertCheck to avoid clash
            if(mapping.containsKey("assertCheck")) {
                i = mapping.get("assertCheck") + 1;
            }

            for (String assertStmt : asserts) {
                assertCheckDefs += String.format("(declare-fun assertCheck%s () Bool) \n", i);
                assertCheckGets += String.format("(get-value ( assertCheck%s ))\n", i);

                assertChecks += String.format("(assert (= assertCheck%s %s))\n", i, assertStmt);

                if (numAnds > idx) {
                    expr += "(and " + "assertCheck" + i + " ";
                    ++idx;
                } else {
                    expr += "assertCheck" + i;
                }
                i++;
            }
            for (int j = 0; j < numAnds; ++j) {
                expr += ")";
            }

            expr += "\n))\n";
            expr += assertChecks;
            expr += "\n(check-sat)\n";

            return assertCheckDefs + expr + assertCheckGets;
        }
        return "(assert false)" +
                "\n(check-sat)\n";
    }

    @Override
    public String visitVarDecl(SimpleCParser.VarDeclContext ctx) {
        return declareVar(ctx.ident.getText());
    }

    @Override
    public String visitProcedureDecl(SimpleCParser.ProcedureDeclContext ctx) {
        StringBuilder builder = new StringBuilder();

        // register global variables and proc args
        for (String var : globals) {
            builder.append(declareVar(var));
            builder.append("\n");
        }

        // proc scope
        scopes.openScope();

        oldGlobals = copyMap(mapping);

        for (FormalParamContext procArg : ctx.formals) {
            builder.append(declareVar(procArg.ident.getText()));
            builder.append("\n");
        }

        for (SimpleCParser.PrepostContext preCond : ctx.contract) {
            if (preCond.requires() != null) {
                visitRequires(preCond.requires());
            }
        }

        for (SimpleCParser.StmtContext stmt : ctx.stmts) {
            builder.append(visitStmt(stmt));
        }

        builder.append("\n");

        checkingIfLogicExpr = true;
        result = visit(ctx.returnExpr);
        resultIsLogicExpr = isLogicExpr.pop();

        for (SimpleCParser.PrepostContext postCond : ctx.contract) {
            if (postCond.ensures() != null) {
                visitEnsures(postCond.ensures());
            }
        }

        builder.append(generateAsserts());

        scopes.closeScope();

        return builder.toString();
    }

    @Override
    public String visitRequires(SimpleCParser.RequiresContext ctx) {
        String requiredCondition = visitLogicalExpr(ctx.condition);

        if(inCallSummarisation) {
            String assumeWithPred = String.format("(=> (and %s %s) %s)", buildAssumptions(), buildPredicate(), requiredCondition);
            asserts.add(assumeWithPred);
        } else {
            assumptions.add(requiredCondition);
        }
        return "";
    }

    @Override
    public String visitEnsures(SimpleCParser.EnsuresContext ctx) {
        String ensuredCondition = visitLogicalExpr(ctx.condition);

        if(inCallSummarisation) {
            assumptions.add(ensuredCondition);
        } else {
            String assumeWithPred = String.format("(=> (and %s %s) %s)", buildAssumptions(), buildPredicate(), ensuredCondition);
            asserts.add(assumeWithPred);
        }
        return "";
    }

    @Override
    public String visitAssignStmt(SimpleCParser.AssignStmtContext ctx) {

        String rhs = visitIntegerExpr(ctx.rhs);

        String varName = scopes.getVariable(ctx.lhs.ident.getText());
        int newId = fresh(varName);
        mapping.put(varName, newId);

        String varDecl = "(declare-fun " + varName + newId + " () (_ BitVec 32))\n";
        return varDecl + "(assert (= " + varName + newId + " " + rhs + "))\n";
    }

    @Override
    public String visitAssertStmt(SimpleCParser.AssertStmtContext ctx) {
        String assertCondition = visitLogicalExpr(ctx.condition);

        String assertWithPred = String.format("(=> (and %s %s) %s)", buildAssumptions(), buildPredicate(), assertCondition);
        asserts.add(assertWithPred);
        return "";
    }

    @Override
    public String visitAssumeStmt(SimpleCParser.AssumeStmtContext ctx) {
        String assumeCondition = visitLogicalExpr(ctx.condition);

        String assumeWithPred = String.format("(=> %s %s)", buildPredicate(), assumeCondition);
        assumptions.add(assumeWithPred);
        return "";
    }

    private String buildInvariants() {
        String invariantStr = "";

        int idx = 0;
        int numInvariants = invariants.size() - 1;

        for (String inv : invariants) {
            if (numInvariants > idx) {
                invariantStr += "(and " + inv + " ";
                ++idx;
            } else {
                invariantStr += inv;
            }
        }
        for (int i = 0; i < numInvariants; i++) {
            invariantStr += ")";
        }

        return invariantStr;
    }

    private String buildAssumptions() {
        String assumptionStr = "";

        int idx = 0;
        int numAssumpts = assumptions.size() - 1;

        for (String ass : assumptions) {
            if (numAssumpts > idx) {
                assumptionStr += "(and " + ass + " ";
                ++idx;
            } else {
                assumptionStr += ass;
            }
        }
        for (int i = 0; i < numAssumpts; i++) {
            assumptionStr += ")";
        }

        return assumptionStr;
    }

    private String buildPredicate() {
        String predicateStr = "";

        int idx = 0;
        int numPreds = predicates.size() - 1;

        for (String pred : predicates) {
            if (numPreds > idx) {
                predicateStr += "(and " + pred + " ";
                ++idx;
            } else {
                predicateStr += pred;
            }
        }
        for (int i = 0; i < numPreds; ++i) {
            predicateStr += ")";
        }

        return predicateStr;
    }

    @Override
    public String visitHavocStmt(SimpleCParser.HavocStmtContext ctx) {
        return havocVar(ctx.var.ident.getText());
    }

    private String havocVar(String var) {
        String varName = scopes.getVariable(var);
        int varId = fresh(varName);
        mapping.put(varName, varId);
        return "(declare-fun " + varName + varId + " () (_ BitVec 32))\n";
    }

    @Override
    //cry inside
    //cry some more
    public String visitCallStmt(CallStmtContext ctx) {

        String expr = "";
        ProcDetail details = procDetails.get(ctx.callee.getText());

        assert details.getArgs().size() == ctx.actuals.size();

        for(int i = 0; i < ctx.actuals.size(); i++) {
            String actual = visitIntegerExpr(ctx.actuals.get(i));
            functionArgumentMap.put(details.getArgs().get(i), actual);
        }

        inCallSummarisation = true;
        //visit precondition with arguments replaced with actuals
        if(details.getPreCond() != null) {
            visit(details.getPreCond());
        }

        //havoc the modset
        for(String varName : details.getModset()) {
            int varId = fresh(varName);
            mapping.put(varName, varId);
            expr += "(declare-fun " + varName + varId + " () (_ BitVec 32))\n";
        }

        //Havoc the temporary return variable
        functionReturnTemp = ctx.callee.getText() + "_ret";
        int ret_id = fresh(functionReturnTemp);
        mapping.put(functionReturnTemp, ret_id);
        expr += "(declare-fun " + functionReturnTemp + ret_id + " () (_ BitVec 32))\n";

        //visit the postcondition with \result replaced with bar_ret
        if(details.getPostCond() != null) {
            visit(details.getPostCond());
        }

        // Assign the lhs to the temporary return variable
        String varName = scopes.getVariable(ctx.lhs.ident.getText());
        int newId = fresh(varName);
        mapping.put(varName, newId);

        String varDecl = "(declare-fun " + varName + newId + " () (_ BitVec 32))\n";
        expr += varDecl + "(assert (= " + varName + newId + " " + functionReturnTemp + mapping.get(functionReturnTemp) + "))";

        functionArgumentMap.clear();
        inCallSummarisation = false;

        return expr;
    }

    @Override
    public String visitIfStmt(SimpleCParser.IfStmtContext ctx) {
        String expr = "";

        String newPred = visitLogicalExpr(ctx.condition);

        Map<String, Integer> originalMap = copyMap(mapping);
        Map<String, Integer> ifMap = copyMap(mapping);
        Map<String, Integer> elseMap = copyMap(mapping);

        mapping = ifMap;

        predicates.push(newPred);
        expr += visit(ctx.thenBlock);
        predicates.pop();
        ifMap = mapping;

        if (ctx.elseBlock != null) {
            mapping = elseMap;

            predicates.push(String.format("(not %s)", newPred));
            expr += visit(ctx.elseBlock);
            predicates.pop();
            elseMap = mapping;
        }

        mapping = originalMap;

        // apply the modset difference
        for (String var : calculateModset(originalMap, ifMap, elseMap)) {
            expr += getScopeFreeVar(var);
            expr += "\n";
            String ifVar = ifMap.get(var) != null ? var + ifMap.get(var) : "(_ bv0 32)";
            String elseVar = elseMap.get(var) != null ? var + elseMap.get(var) : "(_ bv0 32)";
            expr += String.format("(assert (= %s (ite %s %s %s)))\n", var + mapping.get(var), newPred,
                    ifVar, elseVar);
        }

        return expr;
    }

    @Override
    public String visitWhileStmt(WhileStmtContext ctx) {
        String expr = "";

        // visit and generate loop condition
        String loopCond = visitLogicalExpr(ctx.condition);

        // visit and build invariants
        ctx.invariantAnnotations.forEach(this::visit);
        String invBefore = buildInvariants();

        // build loop body and collect info for building modset
        Map<String, Integer> originalMap = copyMap(mapping);
        Map<String, Integer> ifMap = copyMap(mapping);
        mapping = ifMap;
        predicates.push(loopCond);

        // visit the main loop body
        String loopBody = visit(ctx.body);

        ifMap = mapping;
        mapping = originalMap;

        // generate additional assert invarient / assume false before the pop the condition pred off the stack
        // TODO: problem is here!! inv still contains info for old values of variables, needs reapplying for newly updated vars???
        invariants.clear();
        ctx.invariantAnnotations.forEach(this::visit);
        String invAfter = buildInvariants();
        asserts.add(invAfter);
        String assumeWithPred = String.format("(=> %s %s)", buildPredicate(), "false");
        assumptions.add(assumeWithPred);

        predicates.pop();
        Set<String> modset = calculateLoopModset(originalMap, ifMap);

        // pretend we visit arbitrary loop iteration
        // assert the invariant
        asserts.add(invBefore);
        // havoc the modset
        for (String var : modset) {
            expr += getScopeFreeVar(var);
        }
        // assuming invatiant
        assumptions.add(invBefore);

        // add loop body
        expr += loopBody;
        for (String var : modset) {
            expr += getScopeFreeVar(var);
            expr += "\n";
            String ifVar = ifMap.get(var) != null ? var + ifMap.get(var) : "(_ bv0 32)";
            String elseVar = originalMap.get(var) != null ? var + originalMap.get(var) : "(_ bv0 32)";
            expr += String.format("(assert (= %s (ite %s %s %s)))\n", var + mapping.get(var), loopCond,
                    ifVar, elseVar);
        }

        return expr;
    }

    private Set<String> calculateLoopModset(Map<String, Integer> originalMap, Map<String, Integer> ifMap) {
        Set<String> modset = new HashSet<>();

        MapDifference<String, Integer> ifMapDiff = Maps.difference(originalMap, ifMap);

        modset.addAll(ifMapDiff.entriesDiffering().keySet());

        return modset;
    }

    @Override
    public String visitBlockStmt(SimpleCParser.BlockStmtContext ctx) {
        scopes.openScope();
        StringBuilder builder = new StringBuilder();
        ctx.stmts.stream().forEach(stmt -> {
            builder.append(visit(stmt));
            builder.append("\n");
        });
        scopes.closeScope();
        return builder.toString();
    }

    @Override
    public String visitInvariant(InvariantContext ctx) {
        String inv = visitLogicalExpr(ctx.condition);
        invariants.add(inv);
        return "";
    }

    private Map<String, Integer> copyMap(Map<String, Integer> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private Set<String> calculateModset(Map<String, Integer> originalMap, Map<String, Integer> ifMap, Map<String, Integer> elseMap) {
        Set<String> modset = new HashSet<>();

        MapDifference<String, Integer> ifMapDiff = Maps.difference(originalMap, ifMap);
        MapDifference<String, Integer> elseMapDiff = Maps.difference(originalMap, elseMap);

        modset.addAll(ifMapDiff.entriesDiffering().keySet());
        modset.addAll(elseMapDiff.entriesDiffering().keySet());

        return modset;
    }

    private String generateIte(List<LorExprContext> args) {
        String expr = "";

        String iteCond = visitLogicalExpr(args.get(0));

        expr += "(ite " + iteCond + " " + visit(args.get(1)) + " ";
        if (args.size() > 3) {
            expr += generateIte(args.subList(2, args.size()));
        } else {
            expr += visit(args.get(2));
        }
        expr += ")";
        return expr;
    }

    @Override
    public String visitTernExpr(SimpleCParser.TernExprContext ctx) {
        String expr = "";
        if (ctx.single != null) {
            expr = visit(ctx.single);
        } else {
            checkIntegerReturnType();
            expr += generateIte(ctx.args);
        }
        return expr;
    }

    @Override
    public String visitLorExpr(SimpleCParser.LorExprContext ctx) {
        String expr;
        if (ctx.single != null) {
            expr = visit(ctx.single);
        } else {
            checkLogicalReturnType();
            expr = generateLogicalExpr(ctx.args, ctx.ops);
        }
        return expr;
    }

    @Override
    public String visitLandExpr(SimpleCParser.LandExprContext ctx) {
        String expr;
        if (ctx.single != null) {
            expr = visit(ctx.single);
        } else {
            checkLogicalReturnType();
            expr = generateLogicalExpr(ctx.args, ctx.ops);
        }
        return expr;
    }

    @Override
    public String visitBorExpr(SimpleCParser.BorExprContext ctx) {
        String expr;
        if (ctx.single != null) {
            expr = visit(ctx.single);
        } else {
            checkIntegerReturnType();
            expr = generateExpr(ctx.args, ctx.ops);
        }
        return expr;
    }

    @Override
    public String visitBxorExpr(SimpleCParser.BxorExprContext ctx) {
        String expr;
        if (ctx.single != null) {
            expr = visit(ctx.single);
        } else {
            checkIntegerReturnType();
            expr = generateExpr(ctx.args, ctx.ops);
        }
        return expr;
    }

    @Override
    public String visitBandExpr(SimpleCParser.BandExprContext ctx) {
        String expr;
        if (ctx.single != null) {
            expr = visit(ctx.single);
        } else {
            checkIntegerReturnType();
            expr = generateExpr(ctx.args, ctx.ops);
        }
        return expr;
    }

    @Override
    public String visitEqualityExpr(SimpleCParser.EqualityExprContext ctx) {
        String expr;
        if (ctx.single != null) {
            expr = visit(ctx.single);
        } else {
            checkLogicalReturnType();
            expr = generateExpr(ctx.args, ctx.ops);
        }
        return expr;
    }

    @Override
    public String visitRelExpr(SimpleCParser.RelExprContext ctx) {
        String expr;
        if (ctx.single != null) {
            expr = visit(ctx.single);
        } else {
            checkLogicalReturnType();
            expr = generateRelExpr(ctx.args, ctx.ops);
        }
        return expr;
    }

    @Override
    public String visitShiftExpr(SimpleCParser.ShiftExprContext ctx) {
        String expr;
        if (ctx.single != null) {
            expr = visit(ctx.single);
        } else {
            checkIntegerReturnType();
            expr = generateMulShiftExpr(ctx.args, ctx.ops);
        }
        return expr;
    }

    @Override
    public String visitAddExpr(SimpleCParser.AddExprContext ctx) {
        String expr;
        if (ctx.single != null) {
            expr = visit(ctx.single);
        } else {
            checkIntegerReturnType();
            expr = generateExpr(ctx.args, ctx.ops);
        }
        return expr;
    }

    @Override
    public String visitMulExpr(SimpleCParser.MulExprContext ctx) {
        String expr;
        if (ctx.single != null) {
            expr = visit(ctx.single);
        } else {
            checkIntegerReturnType();
            expr = generateMulShiftExpr(ctx.args, ctx.ops);
        }
        return expr;
    }

    private String generateMulShiftExpr(List<? extends ParserRuleContext> ctxs, List<Token> ops) {
        List<String> opstrs = ops.stream().map(Token::getText).collect(Collectors.toList());
        String expr = "";

        String lhs;
        String rhs;

        int opIdx = ops.size() - 1;

        if(ctxs.size() > 2) {
            lhs = generateMulShiftExpr(ctxs.subList(0, ctxs.size()-1), ops.subList(0, opIdx));
            rhs = visitIntegerExpr(ctxs.get(ctxs.size() - 1));
        } else {
            lhs = visitIntegerExpr(ctxs.get(0));
            rhs = visitIntegerExpr(ctxs.get(1));
        }

        if (opstrs.get(opIdx).equals(">>") || opstrs.get(opIdx).equals("<<")) {
            expr += "(" + String.format(smtBinFuncs.get(opstrs.get(opIdx)), rhs, "(_ bv0 32)", lhs, rhs) + ")";
        } else if(opstrs.get(opIdx).equals("/") || opstrs.get(opIdx).equals("%")){
            expr += "(" + String.format(smtBinFuncs.get(opstrs.get(opIdx)), rhs, lhs, lhs, rhs) + ")";
        } else if(opstrs.get(opIdx).equals("*")){
            expr += "(" + String.format(smtBinFuncs.get(opstrs.get(opIdx)), lhs) + " " + rhs + ")";
        }

        return expr;
    }

    @Override
    public String visitUnaryExpr(SimpleCParser.UnaryExprContext ctx) {
        String expr = "";
        if (ctx.single != null) {
            expr = visit(ctx.single);
        } else {
            String closingBrackets = "";
            if(ctx.ops.get(0).getText().equals("!")) {
                checkLogicalReturnType();
            } else {
                checkIntegerReturnType();
            }
            for (int i = 0; i < ctx.ops.size(); ++i) {
                String op = ctx.ops.get(i).getText();
                Boolean lastOp = false;
                String nextOp = "";
                try {
                    nextOp = ctx.ops.get(i + 1).getText();
                } catch (IndexOutOfBoundsException e) {
                    lastOp = true;
                }
                //rise4fun
                switch (op) {
                    case "+":
                        if (!lastOp) {
                            if (nextOp.equals("!")) {
                                expr += "(tobv32 ";
                                closingBrackets += ")";
                            }
                        } else {
                            expr += visitIntegerExpr(ctx.arg);
                        }
                        break;
                    case "-":
                        expr += "(bvmul #xffffffff ";
                        closingBrackets += ")";
                        if (!lastOp) {
                            if (nextOp.equals("!")) {
                                expr += "(tobv32 ";
                                closingBrackets += ")";
                            }
                        } else {
                            expr += visitIntegerExpr(ctx.arg);
                        }
                        break;
                    case "!":
                        expr += "(not ";
                        closingBrackets += ")";
                        if (!lastOp) {
                            if (!nextOp.equals("!")) {
                                expr += "(tobool ";
                                closingBrackets += ")";
                            }
                        } else {
                            expr += visitLogicalExpr(ctx.arg);
                        }
                        break;
                    case "~":
                        expr += "(bvnot ";
                        closingBrackets += ")";
                        if (!lastOp) {
                            if (nextOp.equals("!")) {
                                expr += "(tobv32 ";
                                closingBrackets += ")";
                            }
                        } else {
                            expr += visitIntegerExpr(ctx.arg);
                        }
                        break;
                }
            }
            expr += closingBrackets;
        }
        return expr;
    }

    @Override
    public String visitNumberExpr(SimpleCParser.NumberExprContext ctx) {
        checkIntegerReturnType();
        return "(_ bv" + ctx.number.getText() + " 32)";
    }

    @Override
    public String visitVarrefExpr(SimpleCParser.VarrefExprContext ctx) {
        checkIntegerReturnType();

        String varName;

        if(inCallSummarisation) {
            if (!mapping.containsKey(ctx.var.ident.getText())) {
                varName = functionArgumentMap.get(ctx.var.ident.getText());
                return varName;
            } else {
                varName = scopes.getVariable(ctx.var.ident.getText());
                return varName + mapping.get(varName);
            }
        } else {
            varName = scopes.getVariable(ctx.var.ident.getText());
            return varName + mapping.get(varName);
        }
    }

    @Override
    public String visitParenExpr(SimpleCParser.ParenExprContext ctx) {
        return visit(ctx.arg);
    }

    @Override
    public String visitResultExpr(SimpleCParser.ResultExprContext ctx) {
        if(resultIsLogicExpr) {
            checkLogicalReturnType();
        } else {
            checkIntegerReturnType();
        }

        if(inCallSummarisation) {
            return functionReturnTemp + mapping.get(functionReturnTemp);
        } else {
            return result;
        }
    }

    @Override
    public String visitOldExpr(SimpleCParser.OldExprContext ctx) {
        checkIntegerReturnType();
        String varName = ctx.arg.ident.getText();
        return varName + oldGlobals.get(varName);
    }

}

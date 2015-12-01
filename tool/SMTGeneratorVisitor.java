package tool;

import candidate.Candidate;
import candidate.CandidateInvariant;
import candidate.CandidatePrePostCond;
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

    private String procName;

    private boolean checkingIfLogicExpr;
    private final Stack<Boolean> isLogicExpr;

    private boolean checkingIfIntegerExpr;
    private final Stack<Boolean> isIntegerExpr;

    private boolean inCallSummarisation;
    private Map<String, String> functionArgumentMap = new HashMap<>();
    private String functionReturnTemp;

    private String result = "";
    private boolean resultIsLogicExpr;

    private int currentAssertID = 0;

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
    private final Set<Assertion> asserts;
    private final Set<String> assumptions;
    private final Scopes scopes;
    private final Map<String, ProcDetail> procDetails;
    private final Set<String> invariants;
    private final Set<CandidateInvariant> enabledCandidateInvariants;

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
        enabledCandidateInvariants = new HashSet<>();
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
            lhs = generateExpr(ctxs.subList(0, ctxs.size() - 1), ops.subList(0, opIdx));
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

            for (Assertion assertStmt : asserts) {
                String assertName;
                if (assertStmt.getName() != null) {
                    assertName = assertStmt.getName();
                } else {
                    assertName = getNextAssertPred();
                }
                assertCheckDefs += String.format("(declare-fun %s () Bool) \n", assertName);
                assertCheckGets += String.format("(get-value ( %s ))\n", assertName);

                assertChecks += String.format("(assert (= %s %s))\n", assertName, assertStmt.getAssertion());

                if (numAnds > idx) {
                    expr += "(and " + assertName + " ";
                    ++idx;
                } else {
                    expr += assertName;
                }
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

        procName = ctx.name.getText();

        // register global variables and proc args
        for (String var : globals) {
            builder.append(declareVar(var));
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
            if (preCond.candidateRequires() != null) {
                addCandidateRequires(preCond.candidateRequires());
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
            if(postCond.candidateEnsures() != null) {
                addCandidateEnsures(postCond.candidateEnsures());
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
            String assertWithPred = String.format("(=> (and %s %s) %s)", buildAssumptions(), buildPredicate(), requiredCondition);
            asserts.add(new Assertion(assertWithPred));
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
            String assertWithPred = String.format("(=> (and %s %s) %s)", buildAssumptions(), buildPredicate(), ensuredCondition);
            asserts.add(new Assertion(assertWithPred));
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
        asserts.add(new Assertion(assertWithPred));
        return "";
    }

    @Override
    public String visitAssumeStmt(SimpleCParser.AssumeStmtContext ctx) {
        String assumeCondition = visitLogicalExpr(ctx.condition);

        String assumeWithPred = String.format("(=> %s %s)", buildPredicate(), assumeCondition);
        assumptions.add(assumeWithPred);
        return "";
    }

    private void assertInvariants() {
        String predicates = buildPredicate();
        String assumptions = buildAssumptions();

        // Normal invariants
        for (String inv : invariants) {
            String invariant = String.format("(=> (and %s %s) %s)", assumptions, predicates, inv);
            asserts.add(new Assertion(invariant));
        }

        // Candidate invariants
        for (CandidateInvariant candidate : enabledCandidateInvariants) {
            String invariant = String.format("(=> (and %s %s) %s)", assumptions, predicates, candidate.getExpr());
            String predName = getNextAssertPred();
            candidate.addPred(predName);
            asserts.add(new Assertion(invariant, predName));
        }
    }

    private String getNextAssertPred() {
        String nextPred = "assertCheck" + currentAssertID;
        currentAssertID++;
        return nextPred;
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

        Map<String, Integer> savedOldGlobals = copyMap(oldGlobals);

        assert details.getArgs().size() == ctx.actuals.size();

        for(int i = 0; i < ctx.actuals.size(); i++) {
            String actual = visitIntegerExpr(ctx.actuals.get(i));
            functionArgumentMap.put(details.getArgs().get(i), actual);
        }

        inCallSummarisation = true;

        //Bring old global map up to date
        for(String global : globals) {
            oldGlobals.put(global, mapping.get(global));
        }

        //visit precondition with arguments replaced with actuals
        details.getPreConds().forEach(this::visit);
        details.getCandidateRequires().keySet().forEach(req -> addCandidateRequires(req, ctx.callee.getText()));

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
        details.getPostConds().forEach(this::visit);
        details.getCandidateEnsures().keySet().forEach(ens -> addCandidateEnsures(ens, ctx.callee.getText()));

        // Assign the lhs to the temporary return variable
        String varName = scopes.getVariable(ctx.lhs.ident.getText());
        int newId = fresh(varName);
        mapping.put(varName, newId);

        String varDecl = "(declare-fun " + varName + newId + " () (_ BitVec 32))\n";
        expr += varDecl + "(assert (= " + varName + newId + " " + functionReturnTemp + mapping.get(functionReturnTemp) + "))";

        functionArgumentMap.clear();
        inCallSummarisation = false;

        //Reset old values
        oldGlobals = copyMap(savedOldGlobals);
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

        //calculate modsets
        ModsetCalculatorVisitor modsetCalculator = new ModsetCalculatorVisitor(scopes, procDetails);
        modsetCalculator.visit(ctx.thenBlock);
        if(ctx.elseBlock != null) {
            modsetCalculator.visit(ctx.elseBlock);
        }
        Set<String> modset = modsetCalculator.getModset();

        // apply the modset difference
        for (String var : modset) {
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

        //good luck knowing what the fuck is happening here
        if (procDetails.get(procName).shouldCheckWithBMC(ctx)) {
            BMCLoopDetail detail = procDetails.get(procName).getBMCLoopDetail(ctx);

            // initialMap used to pull off values for varIds before havocing the modset
            Stack<Map<String, Integer>> initialMapStack = new Stack<>();
            Stack<String> condStack = new Stack<>();

            //calculate modset
            ModsetCalculatorVisitor modsetCalculator = new ModsetCalculatorVisitor(scopes, procDetails);
            modsetCalculator.visit(ctx);
            Set<String> modset = modsetCalculator.getModset();

            // generating cascading unwinding
            for (int i = 0; i < detail.getUnwindingDepth(); ++i){
                initialMapStack.push(copyMap(mapping));

                // generate cond
                String cond = visitLogicalExpr(ctx.condition);
                condStack.push(cond);

                predicates.push(cond);
                expr += visit(ctx.body);
            }
            // unwinding assertion generation
            String cond = visitLogicalExpr(ctx.condition);

            predicates.push(cond);

            if (!detail.isUnsound()) {
                // assert false indicating the end of unwinding depth
                String assertFalsePred = String.format("(=> (and %s %s) %s)",
                        buildAssumptions(), buildPredicate(), "false");
                String nextAssertId = getNextAssertPred();
                asserts.add(new Assertion(assertFalsePred, nextAssertId));
                detail.addOwnedPred(nextAssertId);
            }

            // add assume false and block further execution
            String assumeFalse = String.format("(=> %s %s)", buildPredicate(), "false");
            assumptions.add(assumeFalse);
            predicates.pop();

            // close down the cascade
            for (int i = 0; i < detail.getUnwindingDepth(); ++i) {
                predicates.pop();

                cond = condStack.pop();

                Map<String, Integer> ifMap = mapping;
                Map<String, Integer> initialMap = initialMapStack.pop();
                mapping = copyMap(initialMap);

                //Apply the modset difference
                for (String var : modset) {
                    expr += getScopeFreeVar(var);
                    expr += "\n";
                    String ifVar = ifMap.get(var) != null ? var + ifMap.get(var) : "(_ bv0 32)";
                    String elseVar = initialMap.get(var) != null ? var + initialMap.get(var) : var + mapping.get(var);
                    expr += String.format("(assert (= %s (ite %s %s %s)))\n", var + mapping.get(var), cond,
                            ifVar, elseVar);
                }
            }
        } else {
            //Assert invariant
            invariants.clear();
            enabledCandidateInvariants.clear();
            ctx.invariantAnnotations.forEach(this::visit);
            assertInvariants();

            //calculate modset
            ModsetCalculatorVisitor modsetCalculator = new ModsetCalculatorVisitor(scopes, procDetails);
            modsetCalculator.visit(ctx);
            Set<String> modset = modsetCalculator.getModset();

            //havoc the modset
            for (String var : modset) {
                expr += getScopeFreeVar(var);
            }

            //Assume Invariant
            invariants.clear();
            enabledCandidateInvariants.clear();
            ctx.invariantAnnotations.forEach(this::visit);
            assumeInvariants();

            // initialMap used to pull off values for varIds before havocing the modset
            Map<String, Integer> initialMap = copyMap(mapping);
            Map<String, Integer> ifMap = copyMap(mapping);

            //Approximate the while loop with an if stmt
            String loopCond = visitLogicalExpr(ctx.condition);

            predicates.push(loopCond);

            //visit loop body
            expr += visit(ctx.body);

            //Assert Invariant
            invariants.clear();
            enabledCandidateInvariants.clear();
            ctx.invariantAnnotations.forEach(this::visit);
            assertInvariants();

            //Assume false
            String assumeFalse = String.format("(=> %s %s)", buildPredicate(), "false");
            assumptions.add(assumeFalse);

            predicates.pop();

            ifMap = mapping;
            mapping = copyMap(initialMap);

            //Apply the modset difference
            for (String var : modset) {
                expr += getScopeFreeVar(var);
                expr += "\n";
                String ifVar = ifMap.get(var) != null ? var + ifMap.get(var) : "(_ bv0 32)";
                String elseVar = initialMap.get(var) != null ? var + initialMap.get(var) : var + mapping.get(var);
                expr += String.format("(assert (= %s (ite %s %s %s)))\n", var + mapping.get(var), loopCond,
                        ifVar, elseVar);
            }

        }
        return expr;
    }

    private void assumeInvariants() {
        String predicate = buildPredicate();

        for (String inv : invariants) {
            assumptions.add(String.format("(=> %s %s)", predicate, inv));
        }

        for(String inv : enabledCandidateInvariants.stream().map(Candidate::getExpr).collect(Collectors.toList())) {
            assumptions.add(String.format("(=> %s %s)", predicate, inv));
        }
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
        return null;
    }

    @Override
    public String visitCandidateInvariant(CandidateInvariantContext ctx) {
        if (procDetails.get(procName).candidateInvariantEnabled(ctx)) {
            String expr = visitLogicalExpr(ctx.expr());
            CandidateInvariant candidate = procDetails.get(procName).getCandidateInvariant(ctx);
            candidate.setExpr(expr);
            enabledCandidateInvariants.add(candidate);
        }
        return null;
    }

    public void addCandidateRequires(CandidateRequiresContext ctx) {
        if(procDetails.get(procName).candidatePrecondEnabled(ctx)) {
            String expr = visitLogicalExpr(ctx.expr());
            CandidatePrePostCond candidate = procDetails.get(procName).getCandidatePrecond(ctx);
            candidate.setExpr(expr);

            if(inCallSummarisation) {
                String assertWithPred = String.format("(=> (and %s %s) %s)", buildAssumptions(), buildPredicate(), expr);
                String predName = getNextAssertPred();
                candidate.addPred(predName, procName);
                asserts.add(new Assertion(assertWithPred, predName));
            } else {
                assumptions.add(expr);
            }
        }
    }

    public void addCandidateRequires(CandidateRequiresContext ctx, String calleeName) {
        if(procDetails.get(calleeName).candidatePrecondEnabled(ctx)) {
            String expr = visitLogicalExpr(ctx.expr());
            CandidatePrePostCond candidate = procDetails.get(calleeName).getCandidatePrecond(ctx);
            candidate.setExpr(expr);

            if(inCallSummarisation) {
                String assertWithPred = String.format("(=> (and %s %s) %s)", buildAssumptions(), buildPredicate(), expr);
                String predName = getNextAssertPred();
                candidate.addPred(predName, procName);
                asserts.add(new Assertion(assertWithPred, predName));
            } else {
                assumptions.add(expr);
            }
        }
    }

    public void addCandidateEnsures(CandidateEnsuresContext ctx) {
        if(procDetails.get(procName).candidatePostcondEnabled(ctx)) {
            String expr = visitLogicalExpr(ctx.expr());
            CandidatePrePostCond candidate = procDetails.get(procName).getCandidatePostcond(ctx);
            candidate.setExpr(expr);

            if(inCallSummarisation) {
                assumptions.add(expr);
            } else {
                String assertWithPred = String.format("(=> (and %s %s) %s)", buildAssumptions(), buildPredicate(), expr);
                String predName = getNextAssertPred();
                candidate.addPred(predName, procName);
                asserts.add(new Assertion(assertWithPred, predName));
            }
        }
    }

    public void addCandidateEnsures(CandidateEnsuresContext ctx, String calleeName) {
        if(procDetails.get(calleeName).candidatePostcondEnabled(ctx)) {
            String expr = visitLogicalExpr(ctx.expr());
            CandidatePrePostCond candidate = procDetails.get(calleeName).getCandidatePostcond(ctx);
            candidate.setExpr(expr);

            if(inCallSummarisation) {
                assumptions.add(expr);
            } else {
                String assertWithPred = String.format("(=> (and %s %s) %s)", buildAssumptions(), buildPredicate(), expr);
                String predName = getNextAssertPred();
                candidate.addPred(predName, procName);
                asserts.add(new Assertion(assertWithPred, predName));
            }
        }
    }

    private Map<String, Integer> copyMap(Map<String, Integer> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private String generateIte(List<LorExprContext> args) {
        String expr = "";

        String iteCond = visitLogicalExpr(args.get(0));

        expr += "(ite " + iteCond + " " + visitIntegerExpr(args.get(1)) + " ";
        if (args.size() > 3) {
            expr += generateIte(args.subList(2, args.size()));
        } else {
            expr += visitIntegerExpr(args.get(2));
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

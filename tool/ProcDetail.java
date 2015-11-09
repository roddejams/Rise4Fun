package tool;

import parser.SimpleCParser;
import parser.SimpleCParser.EnsuresContext;
import parser.SimpleCParser.RequiresContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProcDetail {

    private SimpleCParser.ProcedureDeclContext ctx;
    private Boolean verified;
    private RequiresContext preCond;
    private EnsuresContext postCond;
    private final Set<String> modset;
    private final List<String> args;
    private final Set<String> calledProcs;


    public ProcDetail(SimpleCParser.ProcedureDeclContext ctx) {
        this.ctx = ctx;
        verified = false;
        modset = new HashSet<>();
        args = new ArrayList<>();
        calledProcs = new HashSet<>();
    }

    public void addPreCond(RequiresContext cond) {
        preCond = cond;
    }

    public void addPostCond(EnsuresContext cond) {
        postCond = cond;
    }

    public void addToModset(String var) {
        modset.add(var);
    }

    public void addArgument(String arg) {
        args.add(arg);
    }

    public void addCalledProc(String proc) {
        calledProcs.add(proc);
    }

    public Boolean procCalled(String proc) {
        return calledProcs.contains(proc);
    }

    public RequiresContext getPreCond() {
        return preCond;
    }

    public EnsuresContext getPostCond() {
        return postCond;
    }

    public Set<String> getModset() {
        return modset;
    }

    public List<String> getArgs() {
        return args;
    }

    public SimpleCParser.ProcedureDeclContext getCtx() {
        return ctx;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setUnverified() {
        verified = false;
    }

    public void setVerified() {
        verified = true;
    }
}

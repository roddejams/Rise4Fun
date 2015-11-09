package tool;

import parser.SimpleCParser.EnsuresContext;
import parser.SimpleCParser.RequiresContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProcDetail {

    private List<RequiresContext> preConds;
    private List<EnsuresContext> postConds;
    private final Set<String> modset;
    private final List<String> args;
    private final Set<String> calledProcs;


    public ProcDetail() {
        modset = new HashSet<>();
        args = new ArrayList<>();
        calledProcs = new HashSet<>();
        preConds = new ArrayList<>();
        postConds = new ArrayList<>();
    }

    public void addPreCond(RequiresContext cond) {
        preConds.add(cond);
    }

    public void addPostCond(EnsuresContext cond) {
        postConds.add(cond);
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

    public List<RequiresContext> getPreConds() {
        return preConds;
    }

    public List<EnsuresContext> getPostConds() {
        return postConds;
    }

    public Set<String> getModset() {
        return modset;
    }

    public List<String> getArgs() {
        return args;
    }
}

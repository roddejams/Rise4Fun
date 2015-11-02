package tool;

import parser.SimpleCParser.EnsuresContext;
import parser.SimpleCParser.RequiresContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProcDetail {

    private RequiresContext preCond;
    private EnsuresContext postCond;
    private final Set<String> modset;
    private final List<String> args;


    public ProcDetail() {
        modset = new HashSet<>();
        args = new ArrayList<>();
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

    public RequiresContext getPreCond() {
        return preCond;
    }

    public EnsuresContext getPostCond() {
        return postCond;
    }

    public Set<String> getModset() {
        return modset;
    }
}

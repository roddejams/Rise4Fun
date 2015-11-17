package candidate;

import java.util.HashSet;
import java.util.Set;

public class Candidate {


    protected Boolean enabled;
    protected Set<String> ownedPredicates;
    protected String expr;

    protected Candidate () {
        enabled = true;
        ownedPredicates = new HashSet<>();
    }

    public Boolean ownsPredicate(String pred) {
        return ownedPredicates.contains(pred);
    }

    public void disable() {
        System.err.println("Disabling candidate that owns predicates " + ownedPredicates.toString());
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }

    public String getExpr() {
        return expr;
    }

    public void addPred(String predName) {
        ownedPredicates.add(predName);
    }

    public void clearPreds() {
        ownedPredicates.clear();
    }
}

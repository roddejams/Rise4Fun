package candidate;

import java.util.HashSet;
import java.util.Set;

public abstract class Candidate {
    protected Boolean enabled;
    protected String expr;

    public Candidate () {
        enabled = true;
    }

    public void disable() {
        //System.err.println("Disabling candidate that owns predicates " + ownedPredicates.toString());
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
}

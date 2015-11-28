package candidate;

import java.util.HashSet;
import java.util.Set;

// TODO: Is this really needed or is Candidate enough? Yes.
public class CandidateInvariant extends Candidate {

    protected Set<String> ownedPredicates;
    public CandidateInvariant() {
        super();
        ownedPredicates = new HashSet<>();
    }

    public Boolean ownsPredicate(String pred) {
        return ownedPredicates.contains(pred);
    }

    public void addPred(String predName) {
        ownedPredicates.add(predName);
    }

    public void clearPreds() {
        ownedPredicates.clear();
    }
}

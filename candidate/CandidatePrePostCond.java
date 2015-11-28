package candidate;

import parser.SimpleCParser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CandidatePrePostCond extends Candidate {

    private Map<String, Set<String>> ownedPreds;

    public CandidatePrePostCond() {
        super();
        ownedPreds = new HashMap<>();
    }

    public Boolean ownsPredicate(String pred, String procName) {
        if(!ownedPreds.containsKey(procName)) {
            return false;
        }
        return ownedPreds.get(procName).contains(pred);
    }

    public void addPred(String pred, String procName) {
        if(!ownedPreds.containsKey(procName)) {
            ownedPreds.put(procName, new HashSet<>());
        }
        ownedPreds.get(procName).add(pred);
    }

    public void clearPreds(String procName) {
        if(!ownedPreds.containsKey(procName)) {
            return;
        }
        ownedPreds.get(procName).clear();
    }
}

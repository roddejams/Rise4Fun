package tool;

import java.util.HashSet;
import java.util.Set;

public class BMCLoopDetail {

    private static final int MAX_UNWINDING_DEPTH = 100;
    private Set<String> ownedPred;
    private int unwindingDepth;

    public BMCLoopDetail(int unwindingDepth) {
        this.unwindingDepth = unwindingDepth;
        ownedPred = new HashSet<>();
    }

    public void addOwnedPred(String ownedPred) {
        this.ownedPred.add(ownedPred);
    }

    public void incUnwindingDepth(int inc) {
        unwindingDepth += inc;
    }

    public Set<String> getOwnedPred() {
        return ownedPred;
    }

    public int getUnwindingDepth() {
        return unwindingDepth;
    }

    public void clearOwnedPred() {
        ownedPred.clear();
    }

    public boolean maxUnwindingDepthReached() {
        return unwindingDepth >= MAX_UNWINDING_DEPTH;
    }
}

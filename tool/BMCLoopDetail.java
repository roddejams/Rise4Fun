package tool;

import java.util.HashSet;
import java.util.Set;

public class BMCLoopDetail {

    private static final int MAX_UNWINDING_DEPTH = 100;
    private Set<String> ownedPred;
    private int unwindingDepth;
    private boolean unsound;

    public BMCLoopDetail(int unwindingDepth) {
        this.unwindingDepth = unwindingDepth;
        ownedPred = new HashSet<>();
        unsound = true;
    }

    public void addOwnedPred(String ownedPred) {
        this.ownedPred.add(ownedPred);
    }

    public void incUnwindingDepth(int inc) {
        unwindingDepth += inc;
        // Give up with unsound, has a punt at sound if we reach the threshold
        unsound = unwindingDepth < MAX_UNWINDING_DEPTH;
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

    public boolean isUnsound() {
        return unsound;
    }
}

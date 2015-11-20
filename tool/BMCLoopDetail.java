package tool;

public class BMCLoopDetail {

    private String ownedPred;
    private int unwindingDepth;

    public BMCLoopDetail(int unwindingDepth) {
        this.unwindingDepth = unwindingDepth;
    }

    public void setOwnedPred(String ownedPred) {
        this.ownedPred = ownedPred;
    }

    public void incUnwindingDepth(int inc) {
        unwindingDepth += inc;
    }

    public String getOwnedPred() {
        return ownedPred;
    }

    public int getUnwindingDepth() {
        return unwindingDepth;
    }
}

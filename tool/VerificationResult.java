package tool;

public class VerificationResult {

    private String procName;
    private String result;

    //TODO: Assertion class? More than 1. Wat
    private Boolean actuallyIncorrect;
    private String removedCandidates;

    public VerificationResult(String procName, String result) {
        this.procName = procName;
        this.result = result;

        //TODO: This is broken yo
        actuallyIncorrect = true;
    }

    public String getProcName() {
        return procName;
    }

    public String getResult() {
        return result;
    }

    public Boolean getActuallyIncorrect() {
        return actuallyIncorrect;
    }


}

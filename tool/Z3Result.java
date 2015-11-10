package tool;


import util.ProcessExec;
import util.ProcessTimeoutException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Z3Result {

    private static final int TIMEOUT = 30;

    private String result;
    private Set<String> failedPreds;

    public Z3Result() {
        failedPreds = new HashSet<>();
    }

    public void process(String in) throws InterruptedException, IOException, ProcessTimeoutException {
        ProcessExec process = new ProcessExec("z3", "-smt2", "-in");
        String z3Result = process.execute(in, TIMEOUT);

        System.err.println("result: " + z3Result);
        if (z3Result.startsWith("sat")) {
            result = "sat";
        } else if (z3Result.startsWith("unsat")) {
            result = "unsat";
        } else {
            result = "unknown";
        }
    }

    public String getResult() {
        return result;
    }

    public Set<String> getFailedPreds() {
        return failedPreds;
    }
}

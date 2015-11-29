package tool;

import util.ProcessExec;
import util.ProcessTimeoutException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class VerificationResult {

    private static final int TIMEOUT = 30;

    private String procName;
    private String result;

    private Set<String> failedPreds;


    public VerificationResult(String procName) {
        this.procName = procName;

        failedPreds = new HashSet<>();
    }

    public String getProcName() {
        return procName;
    }

    public String getResult() {
        return result;
    }

    public Set<String> getFailedPreds() {
        return failedPreds;
    }

    public void process(String in) {
        ProcessExec process = new ProcessExec("z3", "-smt2", "-in");
        String z3Result;
        try {
            z3Result = process.execute(in, TIMEOUT);
        } catch (ProcessTimeoutException | IOException e) {
            result = "UNKNOWN";
            return;
        } catch (InterruptedException e) {
            result = "INTERRUPTED";
            return;
        }
        System.err.println("result: " + z3Result);
        if (z3Result.startsWith("sat")) {

            // Java regex sucks lol
            String[] preds = z3Result.split("\n");
            for (String s : preds) {
                if (s.startsWith("((")) {
                    String[] res = s.split(" ");
                    if (res[1].equals("false))")) {
                        failedPreds.add(res[0].substring(2));
                    }
                }
            }
            result = "INCORRECT";
        } else if (z3Result.startsWith("unsat")) {
            result = "CORRECT";
        } else {
            result = "UNKNOWN";
        }

        // Debug output yay!
        if (!failedPreds.isEmpty()) {
            System.err.println("Failed Predicates");
            for (String pred : failedPreds) {
                System.err.println(pred);
            }
        }
    }
}


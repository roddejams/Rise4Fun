package tool;


import util.ProcessExec;
import util.ProcessTimeoutException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            String [] preds = z3Result.split("\n");
            for (String s : preds) {
                if (s.startsWith("((")) {
                    String[] res = s.split(" ");
                    if (res[1].equals("false))")) {
                        failedPreds.add(res[0].substring(2));
                    }
                }
            }
            result = "sat";
        } else if (z3Result.startsWith("unsat")) {
            result = "unsat";
        } else {
            result = "unknown";
        }

        for (String d : failedPreds) {
            System.out.println("fds " + d);
        }
    }

    public String getResult() {
        return result;
    }

    public Set<String> getFailedPreds() {
        return failedPreds;
    }
}

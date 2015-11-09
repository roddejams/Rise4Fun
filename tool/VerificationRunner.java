package tool;

import util.ProcessExec;

import java.util.Queue;
import java.util.concurrent.Callable;

public class VerificationRunner implements Runnable {

    private static final int TIMEOUT = 30;
    private String procName;
    private VCGenerator generator;
    private Queue<VerificationResult> results;

    public VerificationRunner(String procName, VCGenerator generator, Queue<VerificationResult> results) {
        this.procName = procName;
        this.generator = generator;
        this.results = results;
    }

    @Override
    public void run() {
        String smtCode = generator.generateVC().toString();

        // prints for testing
        System.err.println(smtCode);

        ProcessExec process = new ProcessExec("z3", "-smt2", "-in");
        String queryResult = "";
        try {
            queryResult = process.execute(smtCode, TIMEOUT);
            System.err.println(queryResult);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("UNKNOWN");
            System.exit(1);
        }
        if (queryResult.startsWith("sat")) {
            // TODO: Parse reason for failure, update procdetail if needed
            results.add(new VerificationResult(procName, "INCORRECT"));
            return;
        }

        if (!queryResult.startsWith("unsat")) {
            System.out.println("UNKNOWN");
            System.out.println(queryResult);
            System.exit(1);
        }
        results.add(new VerificationResult(procName, "CORRECT"));
        return;
    }
}

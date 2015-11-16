package tool;

import java.util.Queue;

public class VerificationRunner implements Runnable {

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
        VerificationResult result = new VerificationResult(procName);
        result.process(smtCode);

        results.add(result);
    }
}

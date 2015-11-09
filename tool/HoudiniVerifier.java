package tool;

import java.util.*;
import java.util.concurrent.*;

public class HoudiniVerifier {

    private ExecutorService executor;
    private Queue<VerificationResult> results;
    private Set<String> globals;
    private Map<String, ProcDetail> procDetails;

    public HoudiniVerifier(int poolSize, Set<String> globals, Map<String, ProcDetail> procDetails) {
        executor = Executors.newFixedThreadPool(poolSize);
        results = new ConcurrentLinkedDeque<>();
        this.globals = globals;
        this.procDetails = procDetails;
    }

    public String verify() throws InterruptedException {
        // Verify all procedures
        procDetails.keySet().forEach(this::verifyProc);

        while(true) {
            if(procDetails.values().stream().map(ProcDetail::getVerified).reduce((a, b) -> a && b).get()) {
                //All procedures are correctly verified, we are done
                return "CORRECT";
            }
            VerificationResult result = null;
            if (!results.isEmpty()) {
                VerificationResult res = results.remove();
                if(res.getResult().equals("CORRECT")) {
                    procDetails.get(res.getProcName()).setVerified();
                    //TODO: Tell other shit to verify itself
                } else {
                    if (res.getActuallyIncorrect()) {
                        // Actual bug, just return incorrect
                        //TODO: Based on other things with candidates enabled?
                        return "INCORRECT";
                    }
                }
            } else {
                    Thread.sleep(100);
            }
            //TODO
            //Get finished job results
            //Decide
            //Queue new jobs as required
        }
    }

    public void verifyProc(String proc) {
        procDetails.get(proc).setUnverified();
        VCGenerator vc = new VCGenerator(procDetails.get(proc).getCtx(), globals, procDetails);
        VerificationRunner runner = new VerificationRunner(proc, vc, results);
        executor.submit(runner);
    }
}

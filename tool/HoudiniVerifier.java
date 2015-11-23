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
            if (!results.isEmpty()) {
                VerificationResult res = results.remove();
                String z3Result = res.getResult();
                ProcDetail procDetail = procDetails.get(res.getProcName());
                switch (z3Result) {
                    case "CORRECT":
                        procDetail.setVerified();
                        //TODO: Tell other shit to verify itself
                        break;
                    case "UNKNOWN":
                        return "UNKNOWN";
                    case "INCORRECT":
                        //Incorrect cases
                        // Failed due to candidates - disable and reverify
                        Set<String> failedPreds = new HashSet<>(res.getFailedPreds());
                        Set<FailureType> failures = procDetail.getFailureType(res.getFailedPreds());
                        if (failures.contains(FailureType.ASSERTION)) {
                            return "INCORRECT";
                        } else if (failures.contains(FailureType.CANDIDATE)) {
                            procDetail.disableCandidates(failedPreds);
                            procDetail.clearAllPreds();
                        } else {
                            procDetail.updateBMCLoopDetails(failedPreds);
                        }
                        // Failed due to candidates or BMC, submit for re-verification
                        verifyProc(res.getProcName());
                        break;
                    default:
                        System.err.println("Invalid status returned, probably Z3 error, pretending it's UNKNOWN");
                        return "UNKNOWN";
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

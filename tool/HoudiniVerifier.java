package tool;

import candidate.CandidatePrePostCond;

import java.util.*;
import java.util.concurrent.*;

public class HoudiniVerifier implements Callable<String> {

    private ExecutorService executor;
    private Queue<VerificationResult> results;
    private Set<String> globals;
    private Map<String, ProcDetail> procDetails;
    private Map<String, Future<?>> runningVerifications;

    public HoudiniVerifier(int poolSize, Set<String> globals, Map<String, ProcDetail> procDetails) {
        executor = Executors.newFixedThreadPool(poolSize);
        results = new ConcurrentLinkedDeque<>();
        runningVerifications = new HashMap<>();
        this.globals = globals;
        this.procDetails = procDetails;
    }

    @Override
    public String call() throws InterruptedException {
        // Verify all procedures
        procDetails.keySet().forEach(this::verifyProc);

        while(true) {
            if(procDetails.values().stream().map(ProcDetail::getVerified).reduce((a, b) -> a && b).get()) {
                //All procedures are correctly verified, we are done
                printRemainingCandidates();
                return "CORRECT";
            }
            if (!results.isEmpty()) {
                VerificationResult res = results.remove();
                String z3Result = res.getResult();
                String procName = res.getProcName();
                ProcDetail procDetail = procDetails.get(procName);
                switch (z3Result) {
                    case "INTERRUPTED":
                        procDetail.clearAllPreds(procName);
                        break;
                    case "CORRECT":
                        if(procDetail.hasUnsoundBMC()) {
                            procDetail.clearAllPreds(procName);
                            clearCompletedVerifications(procName);
                            // Pass an empty hash set because it needs a set of failed preds (ew)
                            procDetail.updateBMCLoopDetails();
                            // Try more unwinding / sound BMC if the threshold is reached
                            verifyProc(procName);
                        } else {
                            procDetail.setVerified();
                            runningVerifications.remove(procName);
                        }
                        break;
                    case "UNKNOWN":
                        return "UNKNOWN";
                    case "INCORRECT":
                        //Incorrect cases
                        // Failed due to candidates - disable and reverify
                        Set<String> failedPreds = new HashSet<>(res.getFailedPreds());
                        Set<FailureType> failures = procDetail.getFailureType(res.getFailedPreds(), procName);
                        for(String calledProc : procDetail.getCalledProcs()) {
                            failures.addAll(procDetails.get(calledProc).getFailureType(res.getFailedPreds(), procName));
                        }
                        if(!res.getFailedPreds().isEmpty()) {
                            return "INCORRECT";
                        }
                        if (failures.contains(FailureType.CANDIDATE_INVARIANT)) {
                            procDetail.disableCandidates(failedPreds, procName);
                            procDetail.clearAllPreds(procName);
                        }
                        if(failures.contains(FailureType.CANDIDATE_POST)) {
                            procDetail.disableCandidates(failedPreds, procName);
                            procDetail.clearAllPreds(procName);

                            for(String proc : procDetails.keySet()) {
                                if(procDetails.get(proc).getCalledProcs().contains(procName)) {
                                    clearCompletedVerifications(proc);
                                    verifyProc(proc);
                                }
                            }
                        }
                        if (failures.contains(FailureType.CANDIDATE_PRE)) {
                            procDetail.disableCandidates(failedPreds, procName);
                            procDetail.clearAllPreds(procName);

                            for(String calledProc : procDetail.getCalledProcs()) {
                                procDetails.get(calledProc).disableCandidates(failedPreds, procName);
                                procDetails.get(calledProc).clearAllPreds(procName);
                                clearCompletedVerifications(calledProc);
                                verifyProc(calledProc);
                            }
                        }
                        if(failures.contains(FailureType.SOUND_BMC)) {
                            return "UNKNOWN";
                        }
                        // Failed due to candidates or BMC, submit for re-verification
                        clearCompletedVerifications(procName);
                        verifyProc(procName);
                        runningVerifications.remove(procName);
                        break;

                    default:
                        System.err.println("Invalid status returned, probably Z3 error, pretending it's UNKNOWN");
                        return "UNKNOWN";
                }
            } else {
                Thread.sleep(100);
            }
        }
    }

    private void printRemainingCandidates() {
        for(String procName : procDetails.keySet()) {
            ProcDetail detail = procDetails.get(procName);
            System.err.println("Remaining Enabled Candidate Requires for " + procName);
            for(CandidatePrePostCond candidate : detail.getCandidateRequires().values()) {
                if(candidate.isEnabled()) {
                    System.err.println(candidate.getExpr());
                }
            }
            System.err.println("Remaining Enabled Candidate Ensures for " + procName);
            for(CandidatePrePostCond candidate : detail.getCandidateEnsures().values()) {
                if(candidate.isEnabled()) {
                    System.err.println(candidate.getExpr());
                }
            }
        }
    }

    private void clearCompletedVerifications(String proc) {
        for(Iterator<VerificationResult> it = results.iterator(); it.hasNext();) {
            VerificationResult result = it.next();
            if(result.getProcName().equals(proc)) {
                it.remove();
            }
        }
    }

    public void verifyProc(String proc) {
        if(runningVerifications.containsKey(proc)) {
            runningVerifications.get(proc).cancel(true);
        }
        procDetails.get(proc).setUnverified();
        VCGenerator vc = new VCGenerator(procDetails.get(proc).getCtx(), globals, procDetails);
        VerificationRunner runner = new VerificationRunner(proc, vc, results);
        runningVerifications.put(proc, executor.submit(runner));
    }
}

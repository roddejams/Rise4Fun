package tool;

import candidate.Candidate;
import candidate.CandidatePreCond;
import org.omg.PortableInterceptor.NON_EXISTENT;
import parser.SimpleCParser;
import parser.SimpleCParser.*;
import candidate.CandidateInvariant;

import java.util.*;

//God object v2
public class ProcDetail {

    private static final int UNWINDING_INCREMENT = 1;
    private SimpleCParser.ProcedureDeclContext ctx;
    private Boolean verified;
    private List<RequiresContext> preConds;
    private List<EnsuresContext> postConds;
    private final Set<String> modset;
    private final List<String> args;
    private final Set<String> calledProcs;
    private Map<CandidateInvariantContext, CandidateInvariant> candidateInvariants;

    private Map<CandidateEnsuresContext, Candidate> candidateEnsures;
    private Map<CandidateRequiresContext, Candidate> candidateRequires;

    private Map<String, Map<CandidateRequiresContext, CandidatePreCond>> othersPreconditions;
    private Map<WhileStmtContext, BMCLoopDetail> bmcLoops;

    public ProcDetail(SimpleCParser.ProcedureDeclContext ctx) {
        this.ctx = ctx;
        verified = false;
        modset = new HashSet<>();
        args = new ArrayList<>();
        calledProcs = new HashSet<>();
        preConds = new ArrayList<>();
        postConds = new ArrayList<>();
        candidateInvariants = new HashMap<>();
        candidateRequires = new HashMap<>();
        candidateEnsures = new HashMap<>();
        othersPreconditions = new HashMap<>();
        bmcLoops = new HashMap<>();
    }

    public void addPreCond(RequiresContext cond) {
        preConds.add(cond);
    }

    public void addPostCond(EnsuresContext cond) {
        postConds.add(cond);
    }

    public void addToModset(String var) {
        modset.add(var);
    }

    public void addArgument(String arg) {
        args.add(arg);
    }

    public void addCalledProc(String proc) {
        calledProcs.add(proc);
    }

    public Boolean procCalled(String proc) {
        return calledProcs.contains(proc);
    }

    public List<RequiresContext> getPreConds() {
        return preConds;
    }

    public List<EnsuresContext> getPostConds() {
        return postConds;
    }

    public Set<String> getModset() {
        return modset;
    }

    public List<String> getArgs() {
        return args;
    }

    public SimpleCParser.ProcedureDeclContext getCtx() {
        return ctx;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setUnverified() {
        verified = false;
    }

    public void setVerified() {
        verified = true;
    }

    public void addCandidateInvariant(CandidateInvariantContext ctx) {
        candidateInvariants.put(ctx, new CandidateInvariant());
    }

    public boolean candidateInvariantEnabled(CandidateInvariantContext ctx) {
        // No null pointer here as summarisation visitor ensures all ctx's are added
        return candidateInvariants.get(ctx).isEnabled();
    }

    public void addCandidatePrecond(CandidateRequiresContext ctx) {
        candidateRequires.put(ctx, new Candidate());
    }

    public boolean candidatePrecondEnabled(CandidateRequiresContext ctx) {
        return candidateRequires.get(ctx).isEnabled();
    }

    public void addCandidatePostcond(CandidateEnsuresContext ctx) {
        candidateEnsures.put(ctx, new Candidate());
    }

    public boolean candidatePostcondEnabled(CandidateEnsuresContext ctx) {
        return candidateEnsures.get(ctx).isEnabled();
    }

    // Returns true if we have disabled a candidate - i.e. need to re-verify
    public void disableCandidates(Set<String> failedPreds) {
        for (String pred : failedPreds) {
            for (CandidateInvariant cand : candidateInvariants.values()) {
                if (cand.ownsPredicate(pred)) {
                    cand.disable();
                }
            }
            for (Candidate cand : candidateRequires.values()) {
                if (cand.ownsPredicate(pred)) {
                    cand.disable();
                }
            }
            for (Candidate cand : candidateEnsures.values()) {
                if (cand.ownsPredicate(pred)) {
                    cand.disable();
                }
            }
        }
    }

    public CandidateInvariant getCandidateInvariant(CandidateInvariantContext ctx) {
        return candidateInvariants.get(ctx);
    }

    public Candidate getCandidatePrecond(CandidateRequiresContext ctx) {
        return candidateRequires.get(ctx);
    }

    public Candidate getCandidatePostcond(CandidateEnsuresContext ctx) {
        return candidateEnsures.get(ctx);
    }

    public void clearAllPreds() {
        candidateInvariants.values().forEach(Candidate::clearPreds);
    }

    public void checkWithBMC(WhileStmtContext ctx, int unwindingDepth) {
        bmcLoops.put(ctx, new BMCLoopDetail(unwindingDepth));
    }

    public boolean shouldCheckWithBMC(WhileStmtContext ctx) {
        return bmcLoops.containsKey(ctx);
    }

    public BMCLoopDetail getBMCLoopDetail(WhileStmtContext ctx) {
        return bmcLoops.get(ctx);
    }

    public Set<FailureType> getFailureType(Set<String> failedPreds) {

        Set<FailureType> failures = new HashSet<>();

        for (Iterator<String> it = failedPreds.iterator(); it.hasNext();) {
            String failedPred = it.next();
            for (CandidateInvariant inv : candidateInvariants.values()) {
                if (inv.ownsPredicate(failedPred)) {
                    failures.add(FailureType.CANDIDATE);
                    it.remove();
                }
            }
            for (Candidate inv : candidateEnsures.values()) {
                if (inv.ownsPredicate(failedPred)) {
                    failures.add(FailureType.CANDIDATE);
                    it.remove();
                }
            }
            for (Candidate inv : candidateRequires.values()) {
                if (inv.ownsPredicate(failedPred)) {
                    failures.add(FailureType.CANDIDATE);
                    it.remove();
                }
            }
            for (BMCLoopDetail bmcLoopDet : bmcLoops.values()) {
                if (bmcLoopDet.getOwnedPred().equals(failedPred)) {
                    failures.add(FailureType.BMC);
                    it.remove();
                }
            }
        }
        if (!failedPreds.isEmpty()) {
            failures.add(FailureType.ASSERTION);
        }

        return failures;
    }

    public void updateBMCLoopDetails(Set<String> failedPreds) {
        for (BMCLoopDetail bmcLoopDet : bmcLoops.values()) {
            if (failedPreds.contains(bmcLoopDet.getOwnedPred())) {
                bmcLoopDet.incUnwindingDepth(UNWINDING_INCREMENT);
            }
        }
    }

    @Override
    //rise4fun
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProcDetail that = (ProcDetail) o;

        if (ctx != null ? !ctx.equals(that.ctx) : that.ctx != null) return false;
        if (verified != null ? !verified.equals(that.verified) : that.verified != null) return false;
        if (preConds != null ? !preConds.equals(that.preConds) : that.preConds != null) return false;
        if (postConds != null ? !postConds.equals(that.postConds) : that.postConds != null) return false;
        if (modset != null ? !modset.equals(that.modset) : that.modset != null) return false;
        if (args != null ? !args.equals(that.args) : that.args != null) return false;
        if (calledProcs != null ? !calledProcs.equals(that.calledProcs) : that.calledProcs != null) return false;
        if (candidateInvariants != null ? !candidateInvariants.equals(that.candidateInvariants) : that.candidateInvariants != null)
            return false;
        if (othersPreconditions != null ? !othersPreconditions.equals(that.othersPreconditions) : that.othersPreconditions != null)
            return false;
        return !(bmcLoops != null ? !bmcLoops.equals(that.bmcLoops) : that.bmcLoops != null);

    }

    @Override
    public int hashCode() {
        int result = ctx != null ? ctx.hashCode() : 0;
        result = 31 * result + (verified != null ? verified.hashCode() : 0);
        result = 31 * result + (preConds != null ? preConds.hashCode() : 0);
        result = 31 * result + (postConds != null ? postConds.hashCode() : 0);
        result = 31 * result + (modset != null ? modset.hashCode() : 0);
        result = 31 * result + (args != null ? args.hashCode() : 0);
        result = 31 * result + (calledProcs != null ? calledProcs.hashCode() : 0);
        result = 31 * result + (candidateInvariants != null ? candidateInvariants.hashCode() : 0);
        result = 31 * result + (othersPreconditions != null ? othersPreconditions.hashCode() : 0);
        result = 31 * result + (bmcLoops != null ? bmcLoops.hashCode() : 0);
        return result;
    }
}

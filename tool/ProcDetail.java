package tool;

import candidate.Candidate;
import candidate.CandidatePreCond;
import parser.SimpleCParser;
import parser.SimpleCParser.CandidateInvariantContext;
import parser.SimpleCParser.CandidateRequiresContext;
import parser.SimpleCParser.EnsuresContext;
import parser.SimpleCParser.RequiresContext;
import candidate.CandidateInvariant;

import java.util.*;

public class ProcDetail {

    private SimpleCParser.ProcedureDeclContext ctx;
    private Boolean verified;
    private List<RequiresContext> preConds;
    private List<EnsuresContext> postConds;
    private final Set<String> modset;
    private final List<String> args;
    private final Set<String> calledProcs;
    private Map<CandidateInvariantContext, CandidateInvariant> candidateInvariants;

    private Map<String, Map<CandidateRequiresContext, CandidatePreCond>> othersPreconditions;

    public ProcDetail(SimpleCParser.ProcedureDeclContext ctx) {
        this.ctx = ctx;
        verified = false;
        modset = new HashSet<>();
        args = new ArrayList<>();
        calledProcs = new HashSet<>();
        preConds = new ArrayList<>();
        postConds = new ArrayList<>();
        candidateInvariants = new HashMap<>();
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

    // Returns true if we have disabled a candidate - i.e. need to re-verify
    public boolean disableCandidates(Set<String> failedPreds) {
        boolean candidatesDisabled = false;
        for (String pred : failedPreds) {
            for (CandidateInvariant cand : candidateInvariants.values()) {
                if (cand.ownsPredicate(pred)) {
                    cand.disable();
                    candidatesDisabled = true;
                }
            }
        }
        return candidatesDisabled;
    }

    public CandidateInvariant getCandidate(CandidateInvariantContext ctx) {
        return candidateInvariants.get(ctx);
    }

    public void clearAllPreds() {
        candidateInvariants.values().forEach(Candidate::clearPreds);
    }
}

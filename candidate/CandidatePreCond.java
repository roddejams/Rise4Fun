package candidate;

import parser.SimpleCParser.CandidateRequiresContext;

public class CandidatePreCond extends Candidate {

    private CandidateRequiresContext ctx;

    public CandidatePreCond(CandidateRequiresContext ctx) {
        super();
        this.ctx = ctx;
    }


}

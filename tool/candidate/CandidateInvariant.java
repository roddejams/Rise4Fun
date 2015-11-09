package tool.candidate;

import parser.SimpleCParser.CandidateInvariantContext;

public class CandidateInvariant extends Candidate {

    private CandidateInvariantContext ctx;

    public CandidateInvariant(CandidateInvariantContext ctx) {
        super();
        this.ctx = ctx;
    }
}

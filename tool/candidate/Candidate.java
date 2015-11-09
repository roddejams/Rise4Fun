package tool.candidate;

import java.util.ArrayList;
import java.util.List;

public class Candidate {


    protected Boolean enabled;
    protected List<String> ownedPredicates;

    protected Candidate () {
        enabled = true;
        ownedPredicates = new ArrayList<>();
    }
}

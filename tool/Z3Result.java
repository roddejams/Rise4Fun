package tool;


import java.util.HashSet;
import java.util.Set;

public class Z3Result {

    private String result;
    private Set<String> failedPreds;

    public Z3Result() {
        failedPreds = new HashSet<>();
    }

    public void process(String in) {

    }

    public String getResult() {
        return result;
    }

    public Set<String> getFailedPreds() {
        return failedPreds;
    }
}

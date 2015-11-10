package tool;


public class Assertion {

    private String assertion;
    private String name;

    public Assertion(String assertion, String name) {
        this.name = name;
        this.assertion = assertion;
    }

    public Assertion(String assertion) {
        this.assertion = assertion;
    }

    public String getAssertion() {
        return assertion;
    }

    public String getName() {
        return name;
    }
}

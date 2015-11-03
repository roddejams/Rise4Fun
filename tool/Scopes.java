package tool;

import java.util.*;


public class Scopes {
    private List<Set<String>> scopes;

    public Scopes() {
        scopes = new LinkedList<Set<String>>();
    }

    public void openScope() {
        scopes.add(new HashSet<>());
    }

    public void closeScope() {
        scopes.remove(scopes.size() - 1);
    }

    public String getVariable(String varName) {
        String var = varName;

        int scopeCount = scopes.size() - 1;
        for (int i = scopes.size() - 1; i >= 0; --i) {
            if (scopes.get(i).contains(varName)) {
                for (int j = 0; j < scopeCount; ++j) {
                    var += "_";
                }
                return var;
            }
            --scopeCount;
        }
        return null;
    }

    public String add(String varName) {
        scopes.get(scopes.size() - 1).add(varName);
        return getVariable(varName);
    }
}

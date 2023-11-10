package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.NameDef;
import edu.ufl.cise.cop4020fa23.exceptions.TypeCheckException;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;



public class SymbolTable {


    public HashMap<String, NameDef> global;
    //public static int numScopesPopped = 0;
    public Stack<HashMap<String, NameDef>> scopes;

    // CONSTRUCTOR
    public SymbolTable() {
        scopes = new Stack<HashMap<String, NameDef>>();
        global = new HashMap<String, NameDef>();
        enterScope();
    }

    void enterScope() {
        scopes.push(new HashMap<>());

    }

    void leaveScope() {
        scopes.pop();

    }

    public boolean insert(String name, NameDef n) throws TypeCheckException {
        if (scopes.get(scopes.size()-1).containsKey(name)) {
            return false;
        }
        scopes.get(scopes.size()-1).put(name, n);
        return true;
    }

    public NameDef lookup(String name) {

        for (int i = scopes.size() - 1; i > - 1; i--) {
            if (scopes.get(i).containsKey(name)) {
                return scopes.get(i).get(name);
            }
        }

        return global.get(name);
    }

    public void print() {

        for (int i = scopes.size() - 1; i > -1; i--) {
            HashMap<String, NameDef> curr = scopes.get(i);
            for (HashMap.Entry<String, NameDef> mpEL : curr.entrySet()) {

                String key = mpEL.getKey();
                NameDef value = mpEL.getValue();
                //System.out.println(key);
            }
        }
    }

    public int getScope(String name) {
        for (int i = scopes.size() - 1; i > - 1; i--) {
            HashMap<String, NameDef> scopeChecked;
            scopeChecked = scopes.get(i);
            if (scopeChecked.containsKey(name)) {
                return (i+1);
            }
        }
        return -1;
    }
}
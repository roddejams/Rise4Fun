package tool;

import parser.SimpleCParser.ProcedureDeclContext;

import java.util.Map;
import java.util.Set;

public class VCGenerator {

	private ProcedureDeclContext proc;
	private SMTGeneratorVisitor visitor;

	public VCGenerator(ProcedureDeclContext proc, Set<String> globals, Map<String, ProcDetail> procDetails) {
		this.proc = proc;
		visitor = new SMTGeneratorVisitor(globals, procDetails);
	}
	
	public StringBuilder generateVC() {
		StringBuilder result = new StringBuilder("(set-logic QF_BV)\n");
		result.append("(set-option :produce-models true)\n");
		result.append("(define-fun tobv32 ((p Bool)) (_ BitVec 32) (ite p (_ bv1 32) (_ bv0 32)))\n");
		result.append("(define-fun tobool ((p (_ BitVec 32))) Bool (ite (= p (_ bv0 32)) false true))\n");

        result.append(visitor.visitProcedureDecl(proc));
		
		//result.append("\n(check-sat)\n");
		return result;
	}

}

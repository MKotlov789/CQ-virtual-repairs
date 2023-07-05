package virtualrepair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementTriplesBlock;
import org.apache.jena.sparql.syntax.ElementVisitorBase;
import org.apache.jena.sparql.syntax.ElementWalker;

import com.google.common.collect.Sets;

import de.tu_dresden.inf.lat.abox_repairs.seed_function.SeedFunction;
import openllet.query.sparqldl.jena.SparqlDLExecutionFactory;

public class VirtualRepairQueryHandler {
	public Query query;
	public Query execQuery;
	public Set<Var> answerVariables;
	public Set<Var> variables;
	public Set<Triple> atoms;
	public ResultSet resultSet;
	public OntModel ontModel;
	public CFunction cFunction;
	public SeedFunction seedFunction;
	public DFunction dFunction;
	public Set<Triple> triplePatterns;
	public VirtualRepairAnswerFilter repairer;
	private Map<List<String>,QuerySolution> resMap;

	public VirtualRepairQueryHandler() {

	}

	public void setEnvironment(OntModel ontModel, SeedFunction seedFunction, DFunction dFunction, CFunction cFunction) {
		this.ontModel = ontModel;
		this.seedFunction = seedFunction;
		this.dFunction = dFunction;
		this.cFunction = cFunction;
	}

	public void setQuery(Query query) {
		this.query = query;
		this.answerVariables = new HashSet<>(query.getProjectVars());
		this.variables = getVariables(query);
		this.execQuery = getExecQuery(query);
		this.triplePatterns = this.getTriplePatterns(query);
		System.out.println("a Query Handler is created, the set of variables is:"
				+variables+"\nthe set of answer variables is:"+answerVariables);
	}

	private Query getExecQuery(Query query) {
		Set<Var> answerVariables = new HashSet<>(query.getProjectVars());
		Set<Var> variables = getVariables(query);
		variables.removeAll(answerVariables);
		Query queryCopy = query.cloneQuery();
		queryCopy.addProjectVars(variables);
		return queryCopy;
	}

	private Set<Var> getVariables(Query query) {
		Set<Var> vars = new HashSet<>();

		Set<Triple> triplePatterns = getTriplePatterns(query);

		for (Triple tp : triplePatterns) {
			if (tp.getSubject().isVariable()) {
				vars.add(Var.alloc(tp.getSubject()));
			} else if (tp.getObject().isVariable()) {
				vars.add(Var.alloc(tp.getObject()));
			} else if (tp.getPredicate().isVariable()) {
				vars.add(Var.alloc(tp.getPredicate()));
			}
		}

		return vars;
	}

	private static Set<Triple> getTriplePatterns(Query query) {
		final Set<Triple> triplePatterns = Sets.newHashSet();

		ElementWalker.walk(query.getQueryPattern(), new ElementVisitorBase() {
			@Override
			public void visit(ElementTriplesBlock el) {
				Iterator<Triple> triples = el.patternElts();
				while (triples.hasNext()) {
					Triple triple = triples.next();
					triplePatterns.add(triple);
				}
			}

			@Override
			public void visit(ElementPathBlock el) {
				Iterator<TriplePath> triplePaths = el.patternElts();
				while (triplePaths.hasNext()) {
					TriplePath tp = triplePaths.next();
					if (tp.isTriple()) {
						Triple triple = tp.asTriple();
						triplePatterns.add(triple);
					}
				}
			}
		});
		return triplePatterns;
	}

	public void executeQuery() {
		this.doQuery();
		this.pruneResultSet();
		this.processQueryRes();

	}

	private void pruneResultSet() {
		this.resMap = new HashMap<>();

		while (this.resultSet.hasNext()) {

			QuerySolution qs = this.resultSet.next();
			List<String> answerTuple = new LinkedList<>();
			for (Var v : this.answerVariables) {
				answerTuple.add(qs.get(v.toString()).toString());
			this.resMap.put(answerTuple, qs);
			}
		}
	}
	
	private void processQueryRes () {
		Set<List<String>> toDisplay = new HashSet<>();
		this.repairer = new VirtualRepairAnswerFilter( answerVariables, triplePatterns, seedFunction, dFunction, cFunction);
		for (Entry<List<String>, QuerySolution> e :this.resMap.entrySet()) {
			this.repairer.setQuerySolution(e.getValue());
			if (!this.repairer.ifHide()) {
				toDisplay.add(e.getKey());
			}
		}
		
		System.out.println("-----------ANSWER TUPLES-----------");
		for (List<String> l :toDisplay) {
			System.out.println(l);
		}
		System.out.println("-----------------------------------");
	}

	private void doQuery() {
//		System.out.println("we execute the following query"+this.execQuery);
		QueryExecution qe = SparqlDLExecutionFactory.create(this.execQuery, this.ontModel);

		this.resultSet = qe.execSelect();

		System.out.println("in order to obtain homomorphisms, the following number of answers is used"+resultSet);

	}

}

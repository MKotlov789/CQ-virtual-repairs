package virtualrepair.tools;

import com.google.common.collect.Sets;
import org.semanticweb.owlapi.model.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CQtoSPARQLconverter {
    private Set<OWLAxiom> CQ;
    private String SPARQLquery;
    private Set<OWLNamedIndividual> answerVariables;
    private Set<String> whereStatement;

    public String getSPARQLquery(Set<OWLAxiom> CQ,Set<OWLNamedIndividual> answerVariables) {
        this.CQ = CQ;
        this.answerVariables = answerVariables;
        this.whereStatement = new HashSet<>();
        SPARQLquery = "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX owl:  <http://www.w3.org/2002/07/owl#>\n";

        String WHEREstatement = getWHEREstatement(CQ);
        String SELECTstatement = getSELECTstatement();
        SPARQLquery = SPARQLquery + SELECTstatement + WHEREstatement;

        return SPARQLquery;
    }

    private String getSELECTstatement () {
        String SELECTstatement = "SELECT ";
        for (OWLNamedIndividual var:answerVariables) {
            SELECTstatement = SELECTstatement + "?"+ getVarName(var) + " ";
        }
        return  SELECTstatement+"\n";
    }

    private String getWHEREstatement (Set<OWLAxiom> CQ) {
//        answerVariables = new HashSet<>();
        String WHEREstatement = "\n WHERE {\n";
        answerVariables.stream().filter(i->!containsClassAssertion(CQ,i))
                .forEach(i-> whereStatement.add("?"+getVarName(i)+" rdf:type "+"owl:Thing . \n"));

        for (OWLAxiom ax:CQ) {
            if(ax.isOfType(AxiomType.CLASS_ASSERTION)) {
                OWLClassAssertionAxiom a = (OWLClassAssertionAxiom) ax;
                OWLClass cl = (OWLClass) a.getClassExpression();
                whereStatement.add("?"+getVarName(a.getIndividual())
                        +" rdf:type <"+  cl.getIRI().toString()+"> . \n");
//                answerVariables.add(a.getIndividual().toStringID());
            } else {
                OWLObjectPropertyAssertionAxiom a = (OWLObjectPropertyAssertionAxiom) ax;
                whereStatement.addAll(getAdditionalAtoms(a));
                whereStatement.add(
                        "?" + getVarName(a.getSubject()) + " <" + a.getProperty().getNamedProperty().getIRI()
                        .toString()+"> "+
                        "?" + getVarName(a.getObject()) + " . \n");
//                answerVariables.add(a.getSubject().toStringID());
//                answerVariables.add(a.getObject().toStringID());
            }

        }
        for(String at: whereStatement) {
            WHEREstatement = WHEREstatement + at;
        }
        WHEREstatement = WHEREstatement + "\n}\n";
        return WHEREstatement;
    }

    private String getVarName(OWLIndividual ind) {
        if(ind.isOWLNamedIndividual()) {
            OWLNamedIndividual nInd = (OWLNamedIndividual) ind;
            return nInd.getIRI().getShortForm().toString().replaceAll("[^a-zA-Z0-9]", "");
        } else {
            return ind.toStringID().replaceAll("[^a-zA-Z0-9]", "");
        }
    }
    private Set<String> getAdditionalAtoms(OWLObjectPropertyAssertionAxiom a) {

        Set<String> additionalAtoms = Sets.newHashSet(a.getSubject(),a.getObject())
                .stream()
                .filter(i->!containsClassAssertion(CQ,i))
                .map(i-> "?"+getVarName(i)+" rdf:type "+"owl:Thing . \n")
                .collect(Collectors.toSet());

        return additionalAtoms;
    }
    private Boolean containsClassAssertion(Set<OWLAxiom> CQ,OWLIndividual ind) {
        return CQ.stream().filter(x -> x.isOfType(AxiomType.CLASS_ASSERTION))
                .map(x -> (OWLClassAssertionAxiom) x)
                .anyMatch(x -> x.getIndividual().equals(ind));
    }
}

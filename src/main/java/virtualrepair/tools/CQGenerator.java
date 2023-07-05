package virtualrepair.tools;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.util.*;
import java.util.stream.Collectors;

public class CQGenerator {
    private final OWLOntology ontology;
    private Random random;
    private final OWLDataFactory dataFactory;
    private Set<OWLClassAssertionAxiom> classAssertions;
    private Set<OWLObjectPropertyAssertionAxiom> objectPropertyAssertions;
    private Set<OWLNamedIndividual> answerVariables;
    private int minDepth = 0;
    private int atNumber = 0;
    private int maxDepth;
    private int maxAtomNumber;


     public CQGenerator(OWLOntology ontology) {
        this.ontology=ontology;
        this.dataFactory=ontology.getOWLOntologyManager().getOWLDataFactory();
    }

    public String generateSPARQLCQString (boolean random,
                                          int maxDepth,
                                          int maxAtomNum,
                                    OWLNamedIndividual rootIndividual) throws CQGenerationException {

        Set<OWLAxiom> CQ = generateCQ(random, maxDepth, maxAtomNum, rootIndividual);
         CQtoSPARQLconverter converter = new CQtoSPARQLconverter();
        String query = converter.getSPARQLquery(CQ,getAnswerVariables());
        return query;
     }
    public Query generateSPARQLCQ (boolean random,
                                   int maxDepth,
                                   int maxAtomNum,
                                   OWLNamedIndividual rootIndividual) throws CQGenerationException {
         String strQuery = generateSPARQLCQString(random, maxDepth, maxAtomNum, rootIndividual);
        return QueryFactory.create( strQuery );
    }


    public Set<OWLAxiom> generateCQ(boolean random,
                                    int maxDepth,
                                    int maxAtomNum,
                                    OWLNamedIndividual rootIndividual) throws CQGenerationException {
        List<OWLNamedIndividual> inds = ontology.individualsInSignature(Imports.INCLUDED)
                .collect(Collectors.toList());
        if(inds.isEmpty())
            throw new CQGenerationException("Ontology contains no individuals!");
        classAssertions = new HashSet<>();
        objectPropertyAssertions = new HashSet<>();
        answerVariables = new HashSet<>();

        if (random) {
            this.random= new Random();
        } else {
            this.random= new PseudoRandom();
        }
        this.maxAtomNumber = maxAtomNum;
        this.maxDepth = maxDepth;
        OWLNamedIndividual rootInd = null;
        if (rootIndividual==null) {
            rootInd = takeRandom(inds);
        } else {
            rootInd = rootIndividual;
        }

        int currentDepth = 1;
        Map<OWLIndividual,List<OWLClass>> rootIndClasses = new HashMap<>();
        rootIndClasses.put(rootInd,findClasses(rootInd));
        this.classAssertions.addAll(generateClassAssertionAxioms(rootIndClasses));
        this.answerVariables.add(rootInd);
        generateTree(findRandomSuccessors(rootInd),currentDepth);
        Set<OWLAxiom> CQ = new HashSet<>();
        CQ.addAll(objectPropertyAssertions);
        CQ.addAll(classAssertions);
        return CQ;
    }
    public Set<OWLNamedIndividual> getAnswerVariables() {
        return answerVariables;
    }
    private void generateTree(List<OWLObjectPropertyAssertionAxiom> ObjectProperties, int currentDepth) {
         int increasedDepth = currentDepth +1;
        List<OWLIndividual> currentBranch =  generateBranch(ObjectProperties);
        int atNum = classAssertions.size()+ objectPropertyAssertions.size();
         if(increasedDepth<maxDepth && !currentBranch.isEmpty() && atNum < maxAtomNumber) {
             for(OWLIndividual ind:currentBranch) {
                 generateTree(findRandomSuccessors(ind),increasedDepth);
             }
         }
    }
    private List<OWLObjectPropertyAssertionAxiom> findSuccessors(OWLIndividual ind) {
        return ontology.axioms(AxiomType.OBJECT_PROPERTY_ASSERTION)
                .filter(ax -> ax.getSubject().equals(ind))
                .collect(Collectors.toList());
    }
    private List<OWLObjectPropertyAssertionAxiom> findRandomSuccessors(OWLIndividual ind) {
        List<OWLObjectPropertyAssertionAxiom> successors = findSuccessors(ind);
//        System.out.println(successors);
        if (!successors.isEmpty()) {
            int successorsNum  = random.nextInt(successors.size());
            return successors.subList(0,successorsNum);
        } else {
            return successors;
        }
    }
    private List<OWLIndividual> getSuccessorIndividuals(List<OWLObjectPropertyAssertionAxiom> descendants) {
        return descendants.stream()
                .map(ax -> ax.getObject()).collect(Collectors.toList());
    }
    private List<OWLClass> findClasses(OWLIndividual ind) {
        return ontology.axioms(AxiomType.CLASS_ASSERTION)
                .filter(ax -> ax.getIndividual().equals(ind))
                .map(ax -> ax.getClassExpression())
                .map(ax -> (OWLClass) ax)
                .collect(Collectors.toList());
    }
    private List<OWLIndividual> generateBranch(List<OWLObjectPropertyAssertionAxiom> ObjectProperties) {
        List<OWLIndividual> successors = getSuccessorIndividuals(ObjectProperties);
        if (!successors.isEmpty()) {
            Map<OWLIndividual, List<OWLClass>> successorsClassMap = getIndClassMap(successors);
            List<OWLNamedIndividual> answerVariables = defineAnswerVariables(successors);
            this.answerVariables.addAll(answerVariables);
            this.classAssertions.addAll(generateClassAssertionAxioms(successorsClassMap));
            this.objectPropertyAssertions.addAll(ObjectProperties);
            return successors;
        } else {
            return successors;
        }
    }
    private Set<OWLClassAssertionAxiom> generateClassAssertionAxioms(Map<OWLIndividual,List<OWLClass>> indClassMap) {
         Set<OWLClassAssertionAxiom> axioms = new HashSet<>();
         for(Map.Entry<OWLIndividual,List<OWLClass>> e:indClassMap.entrySet()) {
             for(OWLClass cl: e.getValue()) {
                 axioms.add(dataFactory.getOWLClassAssertionAxiom(cl,e.getKey()));
             }
         }
         return axioms;
    }
    private List<OWLNamedIndividual> defineAnswerVariables (List<OWLIndividual> inds) {
        Collections.shuffle(inds);
        int actualVarNum = getVarNum(inds);
        int desiredVarNum = random.nextInt(inds.size());
        int toConvert = desiredVarNum - actualVarNum;

        if (toConvert> 0) {
            List<OWLNamedIndividual> answerVars = inds.stream()
                    .filter(ind -> ind.isOWLNamedIndividual())
                    .map(ind -> (OWLNamedIndividual)ind )
                    .collect(Collectors.toList());
            return answerVars.subList(0,answerVars.size()-toConvert);
        } else {
            return inds.stream()
                    .filter(ind -> ind.isOWLNamedIndividual())
                    .map(ind -> (OWLNamedIndividual)ind )
                    .collect(Collectors.toList());
        }
    }

    private int getVarNum(List<OWLIndividual> inds) {
        return (int) inds.stream().filter(ind -> ind.isAnonymous()).count();
    }
    private Map<OWLIndividual,List<OWLClass>> getIndClassMap(List<OWLIndividual> inds) {
        Map<OWLIndividual,List<OWLClass>> successorOWLClassMap = new HashMap<>();
        for (OWLIndividual ind:inds) {
            List<OWLClass> classes = findClasses(ind);
            if (!classes.isEmpty()) {
                Collections.shuffle(classes);
                int numClasses = random.nextInt(classes.size());
                List<OWLClass> randomClasses = classes.subList(0,numClasses);
                successorOWLClassMap.put(ind,randomClasses);
            } else {
                successorOWLClassMap.put(ind,classes);
            }

        }
        return successorOWLClassMap;
    }
    private <T> T takeRandom(List<T> list) {

        Random rand= new Random();
        return list.remove(
                rand.nextInt(
                        list.size()));
    }
    private class PseudoRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return bound;
        }
    }
}

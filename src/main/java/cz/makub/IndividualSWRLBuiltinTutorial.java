package cz.makub;

import static cz.makub.SWRLBuiltInsTutorial.listAllDataPropertyValues;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;
import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import cz.makub.swrl.CustomSWRLBuiltin;
import openllet.core.boxes.abox.ABox;
import openllet.core.boxes.abox.Node;
import openllet.core.rules.builtins.BuiltInRegistry;
import openllet.core.utils.ATermUtils;
import openllet.owlapi.OpenlletReasonerFactory;

/**
 * Example of a Pellet SWRL built-in that works with both Individuals and data literals.
 * <p>
 * Run in Maven with <code>mvn exec:java -Dexec.mainClass=cz.makub.IndividualSWRLBuiltinTutorial</code>
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class IndividualSWRLBuiltinTutorial {

    /**
     * The built-in implementation.
     */
    private static class IRIparts implements CustomSWRLBuiltin.CustomSWRLFunction {

        @Override
        public boolean isApplicable(boolean[] boundPositions) {
            //applicable only to 4 arguments, two bound and two unbound
            return boundPositions.length == 4 && boundPositions[0] && boundPositions[1] && !boundPositions[2] && !boundPositions[3];
        }

        @Override
        public boolean apply(ABox abox, Node[] args) {
            //accepts IRIparts(individual,separator string,unbound variable,unbound variable)
            if (!args[0].isIndividual() || !args[1].isLiteral() || args[2] != null || args[3] != null) return false;
            //get the IRI of the individual in the first argument
            String iri = args[0].getNameStr();
            //get the string value of the second argument
            String separator = ATermUtils.getLiteralValue(args[1].getTerm());
            //split the IRI at the separator
            int idx = iri.indexOf(separator);
            if (idx == -1) return false;
            String prefix = iri.substring(0, idx);
            String id = iri.substring(idx + separator.length());
            //bind the third and fourth arguments to the IRI parts
            args[2] = abox.addLiteral(ATermUtils.makeStringLiteral(prefix));
            args[3] = abox.addLiteral(ATermUtils.makeStringLiteral(id));
            return true;
        }
    }

    //a simple example ontology
    private static final String DOC_URL = "http://acrab.ics.muni.cz/ontologies/swrl_tutorial_ind.owl";

    public static void main(String[] args) throws OWLOntologyCreationException {
        //register my built-in implementation
        BuiltInRegistry.instance.registerBuiltIn("urn:makub:builtIn#IRIparts", new CustomSWRLBuiltin(new IRIparts()));
        //initialize ontology and reasoner
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(IRI.create(DOC_URL));
        OWLReasonerFactory reasonerFactory = OpenlletReasonerFactory.getInstance();
        OWLReasoner reasoner = reasonerFactory.createReasoner(ontology, new SimpleConfiguration());
        OWLDataFactory factory = manager.getOWLDataFactory();
        PrefixDocumentFormat pm = manager.getOntologyFormat(ontology).asPrefixOWLDocumentFormat();
        //print the SWRL rule
        listSWRLRules(ontology);
        //use the rule with the built-in to infer property values
        OWLNamedIndividual martin = factory.getOWLNamedIndividual(":Martin", pm);
        listAllDataPropertyValues(martin, ontology, reasoner);
        System.out.println("done.");
    }

    private static void listSWRLRules(OWLOntology ontology) {
        OWLObjectRenderer renderer = new DLSyntaxObjectRenderer();
        for (SWRLRule rule : ontology.getAxioms(AxiomType.SWRL_RULE)) {
            System.out.println(renderer.render(rule));
        }
    }



}
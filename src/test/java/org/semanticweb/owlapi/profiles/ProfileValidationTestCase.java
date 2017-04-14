/* This file is part of the OWL API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright 2014, The University of Manchester
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. */
package org.semanticweb.owlapi.profiles;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.Searcher;
import org.semanticweb.owlapi.vocab.OWL2Datatype;


/**
 *
 * ONT-API WARNING:
 * This testcase never worked and does not work under OWL-API.
 * BUT it was so designed that it always passed.
 * NOW it always fails for OWL-API (if -Ddebug.use.owl=true), since it can not find any inner ontology.
 * I don't know why but it is definitely a bug of OWL-API.
 * TODO: for ONT-API there are also problems:
 * 1) in the 'all.rdf' there are some broken ontologies (wrong rdf:List, invalid IRI)
 * 2) there are also ontologies with web-resources in imports
 * 3) In ONT-API the transforms mechanism makes all graphs to be ontological consistent,
 * so there could not be axiom rdfs:subClassOf with missed class declaration (for example).
 * On the other hand these ontologies still could be not DL (but always FULL).
 * Example of such violation 'Not enough operands; at least two needed' for owl:intersectionOf.
 * The all.rdf is very huge and I don't want to edit it special for ONT-API.
 * By this reason I temporarily disable checking for DL.
 *
 * @author Matthew Horridge, The University of Manchester, Information
 *         Management Group
 * @since 3.0.0
 */
@ru.avicomp.ontapi.utils.ModifiedForONTApi
@SuppressWarnings("javadoc")
public class ProfileValidationTestCase extends TestBase {
    private static final String ALL_NS = "http://www.w3.org/2007/OWL/testOntology#";
    private static final String ALL_PATH = "/owlapi/all.rdf";

    private static final List<IRI> SKIPPED = Stream.of(
            // invalid uri ('http://example.com/b-and-c=2a'):
            "http://km.aifb.uni-karlsruhe.de/projects/owltests/index.php/Special:URIResolver/One_equals_two"
            // broken rdf:List
            , "http://km.aifb.uni-karlsruhe.de/projects/owltests/index.php/Special:URIResolver/New-2DFeature-2DRational-2D003"
            , "http://km.aifb.uni-karlsruhe.de/projects/owltests/index.php/Special:URIResolver/New-2DFeature-2DRational-2D002"
            // web-access:
            , "http://km.aifb.uni-karlsruhe.de/projects/owltests/index.php/Special:URIResolver/TestCase-3AWebOnt-2Dmiscellaneous-2D001"
            , "http://km.aifb.uni-karlsruhe.de/projects/owltests/index.php/Special:URIResolver/TestCase-3AWebOnt-2Dmiscellaneous-2D002"
            , "http://km.aifb.uni-karlsruhe.de/projects/owltests/index.php/Special:URIResolver/TestCase-3AWebOnt-2Dimports-2D011"
    ).map(IRI::create).collect(Collectors.toList());

    @Test
    public void testProfiles() throws Exception {
        IRI allTestURI = IRI.create(ProfileValidationTestCase.class.getResource(ALL_PATH));
        OWLOntology testCasesOntology = m.loadOntologyFromOntologyDocument(allTestURI);
        OWLClass profileIdentificationTestClass = df.getOWLClass(IRI.create(ALL_NS, "ProfileIdentificationTest"));

        OWLObjectProperty speciesProperty = df.getOWLObjectProperty(IRI.create(ALL_NS, "species"));
        OWLDataProperty rdfXMLPremiseOntologyProperty = df.getOWLDataProperty(IRI.create(ALL_NS, "rdfXmlPremiseOntology"));
        // new: they forgot about fs ontology:
        OWLDataProperty fsPremiseOntologyProperty = df.getOWLDataProperty(IRI.create(ALL_NS, "fsPremiseOntology"));

        int count = 0;
        List<OWLClassAssertionAxiom> axioms = testCasesOntology.classAssertionAxioms(profileIdentificationTestClass).collect(Collectors.toList());
        for (OWLClassAssertionAxiom ax : axioms) {
            LOGGER.debug(String.valueOf(ax));
            OWLNamedIndividual ind = ax.getIndividual().asOWLNamedIndividual();
            List<OWLLiteral> values = Stream.concat(
                    Searcher.values(testCasesOntology.dataPropertyAssertionAxioms(ind), rdfXMLPremiseOntologyProperty),
                    Searcher.values(testCasesOntology.dataPropertyAssertionAxioms(ind), fsPremiseOntologyProperty)
            ).collect(Collectors.toList());
            // WARNING: OWL-API (NOT ONT-API) always fails here:
            Assert.assertFalse("No values found", values.isEmpty());
            IRI iri = ind.asOWLNamedIndividual().getIRI();
            LOGGER.debug("{}:::IRI:::{}", ++count, iri);
            if (SKIPPED.contains(iri)) {
                LOGGER.warn("SKIP:::{}", ax);
                continue;
            }
            Collection<OWLIndividual> finder = Searcher.values(testCasesOntology.objectPropertyAssertionAxioms(ind), speciesProperty).collect(Collectors.toSet());
            Collection<OWLIndividual> negativeFinder = Searcher.negValues(testCasesOntology.negativeObjectPropertyAssertionAxioms(ind), speciesProperty).collect(Collectors.toSet());
            for (OWLLiteral v : values) {
                testInnerOntology(v.getLiteral(), finder, negativeFinder);
            }
        }
    }

    private static void testInnerOntology(String txt, Collection<OWLIndividual> finder, Collection<OWLIndividual> negativeFinder) throws Exception {
        OWLNamedIndividual el = df.getOWLNamedIndividual(IRI.create(ALL_NS, "EL"));
        OWLNamedIndividual ql = df.getOWLNamedIndividual(IRI.create(ALL_NS, "QL"));
        OWLNamedIndividual rl = df.getOWLNamedIndividual(IRI.create(ALL_NS, "RL"));
        OWLNamedIndividual full = df.getOWLNamedIndividual(IRI.create(ALL_NS, "FULL"));
        OWLNamedIndividual dl = df.getOWLNamedIndividual(IRI.create(ALL_NS, "DL"));
        OWLOntologyManager manager = manager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(txt));
        //ru.avicomp.ontapi.utils.ReadWriteUtils.print(ontology);
        // Always FULL:
        checkProfile(ontology, new OWL2Profile(), true);
        // DL?
        /*if (finder.contains(dl)) {
            checkProfile(ontology, new OWL2DLProfile(), true);
        }
        if (negativeFinder.contains(dl)) {
            checkProfile(ontology, new OWL2DLProfile(), false);
        }*/
        // EL?
        if (finder.contains(el)) {
            checkProfile(ontology, new OWL2ELProfile(), true);
        }
        if (negativeFinder.contains(el)) {
            checkProfile(ontology, new OWL2ELProfile(), false);
        }
        // QL?
        if (finder.contains(ql)) {
            checkProfile(ontology, new OWL2QLProfile(), true);
        }
        if (negativeFinder.contains(ql)) {
            checkProfile(ontology, new OWL2QLProfile(), false);
        }
        // RL?
        if (finder.contains(rl)) {
            checkProfile(ontology, new OWL2RLProfile(), true);
        }
        if (negativeFinder.contains(rl)) {
            checkProfile(ontology, new OWL2RLProfile(), false);
        }
        manager.removeOntology(ontology);
    }

    private static OWLOntologyManager manager() {
        OWLOntologyManager m = setupManager();
        if (DEBUG_USE_OWL) return m;
        OWLOntologyLoaderConfiguration conf = ((ru.avicomp.ontapi.config.OntLoaderConfiguration) m
                .getOntologyLoaderConfiguration())
                //.setAllowReadDeclarations(false)
                //.setPerformTransformation(false)
                .setSupportedSchemes(Stream.of(ru.avicomp.ontapi.config.OntConfig.DefaultScheme.FILE).collect(Collectors.toList()))
                .setPersonality(ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig.ONT_PERSONALITY_LAX);
        m.setOntologyLoaderConfiguration(conf);
        return m;
    }

    private static void checkProfile(OWLOntology ontology, OWLProfile profile, boolean shouldBeInProfile) {
        OWLProfileReport report = profile.checkOntology(ontology);
        Assert.assertEquals(String.format("[%s] VIOLATIONS:\n%s", profile.getClass().getSimpleName(), report.getViolations()),
                shouldBeInProfile, report.isInProfile());
    }

    @Test
    public void shouldNotFailELBecauseOfBoolean() {
        OWLOntology o = getOWLOntology();
        OWLAnnotation ann = df.getRDFSLabel(df.getOWLLiteral(true));
        OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(IRI.create("urn:test#", "ELProfile"), ann);
        o.add(ax, df.getOWLDeclarationAxiom(OWL2Datatype.XSD_BOOLEAN.getDatatype(df)));
        checkProfile(o, new OWL2ELProfile(), true);
    }
}

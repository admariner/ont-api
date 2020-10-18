/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.owlcs.owlapi.tests.api.syntax;

import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxPrefixNameShortFormProvider;
import org.semanticweb.owlapi.manchestersyntax.renderer.ParserException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.AnnotationValueShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.mansyntax.ManchesterOWLSyntaxParser;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Class;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.*;

public class ManchesterOWLSyntaxParserTestCase extends TestBase {

    public static final String NS = "http://protege.org/ontologies/Test.owl";
    protected OWLDataProperty p;
    protected OWLDatatype dateTime;

    @BeforeEach
    public void setUpPAndDateTime() {
        p = DataProperty(IRI(NS + "#", "p"));
        dateTime = df.getOWLDatatype(XSDVocabulary.DATE_TIME);
    }

    @Test
    public void testShouldRoundTripAnnotationAssertionsWithAnnotations() throws Exception {
        String input = "Prefix: o: <urn:test#>\nOntology: <urn:test>\n AnnotationProperty: o:bob\n Annotations:\n rdfs:label \"bob-label\"@en";
        OWLOntology o = loadOntologyFromString(input);
        OWLOntology o2 = roundTrip(o);
        equal(o, o2);
    }

    @Test
    public void testShouldRoundTrip() throws Exception {
        // given
        IRI iri = IRI("http://protege.org/ontologies#", "p");
        OWLOntology ontology = getOWLOntology();
        ontology.add(Declaration(DataProperty(iri)));
        // when
        ontology = roundTrip(ontology);
        // then
        Assertions.assertTrue(ontology.containsDataPropertyInSignature(iri));
    }

    @Test
    public void testShouldRenderCorrectly() throws Exception {
        // given
        OWLObjectProperty prop = ObjectProperty(IRI("urn:test#", "p"));
        OWLClass led = Class(IRI("urn:test#", "led"));
        OWLClass crt = Class(IRI("urn:test#", "crt"));
        OWLClass display = Class(IRI("urn:test#", "display"));
        OWLOntology ontology = getOWLOntology();
        OWLObjectSomeValuesFrom r = df.getOWLObjectSomeValuesFrom(prop, df.getOWLObjectUnionOf(led, crt));
        OWLSubClassOfAxiom axiom = df.getOWLSubClassOfAxiom(display, r);
        ontology.add(axiom);
        StringDocumentTarget target = saveOntology(ontology, new ManchesterSyntaxDocumentFormat());
        Assertions.assertFalse(target.toString().contains("((<urn:test#crt> or <urn:test#led>))"));
    }

    @Test
    public void testShouldRoundTripDisjointUnion() throws Exception {
        OWLOntology o = getOWLOntology();
        OWLClass a = Class(IRI("http://iri/#", "a"));
        OWLClass b = Class(IRI("http://iri/#", "b"));
        OWLClass c = Class(IRI("http://iri/#", "c"));
        OWLClass d = Class(IRI("http://iri/#", "d"));
        OWLDisjointUnionAxiom axiom = DisjointUnion(a, b, c, d);
        o.add(axiom);
        o.add(Declaration(a));
        o.add(Declaration(b));
        o.add(Declaration(c));
        o.add(Declaration(d));
        OWLOntology actual = roundTrip(o, new ManchesterSyntaxDocumentFormat());
        Assertions.assertEquals(o.axioms().collect(Collectors.toSet()), actual.axioms().collect(Collectors.toSet()));
    }

    @Test
    public void testManSyntaxEditorParser() {
        Assertions.assertThrows(ParserException.class, this::manSyntaxEditorParser);
    }

    private void manSyntaxEditorParser() throws Exception {
        String onto = "<?xml version=\"1.0\"?>" + "<!DOCTYPE rdf:RDF ["
                + "<!ENTITY vin  \"http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#\" >"
                + "<!ENTITY food \"http://www.w3.org/TR/2003/PR-owl-guide-20031209/food#\" >"
                + "<!ENTITY owl  \"http://www.w3.org/2002/07/owl#\" >"
                + "<!ENTITY xsd  \"http://www.w3.org/2001/XMLSchema#\" >" + "]>" + "<rdf:RDF "
                + "xmlns     = \"http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#\" "
                + "xmlns:vin = \"http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#\" "
                + "xml:base  = \"http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine#\" "
                + "xmlns:food= \"http://www.w3.org/TR/2003/PR-owl-guide-20031209/food#\" "
                + "xmlns:owl = \"http://www.w3.org/2002/07/owl#\" "
                + "xmlns:rdf = \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
                + "xmlns:rdfs= \"http://www.w3.org/2000/01/rdf-schema#\" "
                + "xmlns:xsd = \"http://www.w3.org/2001/XMLSchema#\">"
                + "<owl:Ontology rdf:about=\"\"><rdfs:comment>An example OWL ontology</rdfs:comment>"
                + "<rdfs:label>Wine Ontology</rdfs:label></owl:Ontology>" + "<owl:Class rdf:ID=\"VintageYear\" />"
                + "<owl:DatatypeProperty rdf:ID=\"yearValue\"><rdfs:domain rdf:resource=\"#VintageYear\" />    <rdfs:range  rdf:resource=\"&xsd;positiveInteger\" />"
                + "</owl:DatatypeProperty></rdf:RDF>";
        String expression = "yearValue some ";
        OWLOntology wine = loadOntologyFromString(onto);
        List<OWLOntology> ontologies = m.ontologies().collect(Collectors.toList());
        ShortFormProvider sfp = new ManchesterOWLSyntaxPrefixNameShortFormProvider(wine.getFormat());
        BidirectionalShortFormProvider shortFormProvider = new BidirectionalShortFormProviderAdapter(ontologies, sfp);
        ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();
        parser.setStringToParse(expression);
        parser.setDefaultOntology(wine);
        parser.setOWLEntityChecker(new ShortFormEntityChecker(shortFormProvider));
        parser.parseClassExpression();
    }

    @Test
    public void testShouldParseRuleInManSyntax() throws Exception {
        String inputManSyntax = "Prefix: owl: <http://www.w3.org/2002/07/owl#>\n"
                + "Prefix: rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "Prefix: xml: <http://www.w3.org/XML/1998/namespace>\n"
                + "Prefix: xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                + "Prefix: rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "Ontology: <http://www.owl-ontologies.com/Ontology1307394066.owl>\n"
                + "Datatype: xsd:decimal\n Datatype: xsd:int\n Datatype: xsd:dateTime\n"
                + "DataProperty: <http://www.owl-ontologies.com/Ontology1307394066.owl#hasAge>\n Characteristics: \n Functional\n Range: \n xsd:int\n"
                + "DataProperty: <http://www.owl-ontologies.com/Ontology1307394066.owl#hasDate>\n Range: \n xsd:dateTime\n"
                + "Class: <http://www.owl-ontologies.com/Ontology1307394066.owl#Person>\n"
                + "Individual: <http://www.owl-ontologies.com/Ontology1307394066.owl#p1>\n Types: \n <http://www.owl-ontologies.com/Ontology1307394066.owl#Person>\n"
                + "Rule: \n xsd:decimal(?<urn:swrl:var#x>), <http://www.owl-ontologies.com/Ontology1307394066.owl#hasAge>(?<urn:swrl:var#p>, ?<urn:swrl:var#x>) -> <http://www.owl-ontologies.com/Ontology1307394066.owl#Person>(?<urn:swrl:var#p>)";
        OWLOntology o = loadOntologyFromString(inputManSyntax);
        OWLOntology o1 = roundTrip(o, new ManchesterSyntaxDocumentFormat());
        Assertions.assertEquals(o.logicalAxioms().collect(Collectors.toSet()), o1.logicalAxioms().collect(Collectors.toSet()));
    }

    @Test
    public void testShouldParseRuleInManSimpleSyntax() throws Exception {
        String inputManSyntax = "Prefix: owl: <http://www.w3.org/2002/07/owl#>\n"
                + "Prefix: rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                + "Prefix: xml: <http://www.w3.org/XML/1998/namespace>\n"
                + "Prefix: xsd: <http://www.w3.org/2001/XMLSchema#>\n"
                + "Prefix: rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "Ontology: <http://www.owl-ontologies.com/Ontology1307394066.owl>\n" + "Datatype: xsd:decimal\n"
                + "Datatype: xsd:int\n" + "Datatype: xsd:dateTime\n"
                + "DataProperty: <http://www.owl-ontologies.com/Ontology1307394066.owl#hasAge>\n"
                + "    Characteristics: \n" + "        Functional\n" + "    Range: \n" + "        xsd:int\n"
                + "DataProperty: <http://www.owl-ontologies.com/Ontology1307394066.owl#hasDate>\n" + "    Range: \n"
                + "        xsd:dateTime\n" + "Class: <http://www.owl-ontologies.com/Ontology1307394066.owl#Person>\n"
                + "Individual: <http://www.owl-ontologies.com/Ontology1307394066.owl#p1>\n" + "    Types: \n"
                + "        <http://www.owl-ontologies.com/Ontology1307394066.owl#Person>\n" + "Rule: \n"
                + "    xsd:decimal(?x), <http://www.owl-ontologies.com/Ontology1307394066.owl#hasAge>(?p, ?x) -> <http://www.owl-ontologies.com/Ontology1307394066.owl#Person>(?p)";
        OWLOntology o = loadOntologyFromString(inputManSyntax);
        OWLOntology o1 = roundTrip(o, new ManchesterSyntaxDocumentFormat());
        Assertions.assertEquals(o.logicalAxioms().collect(Collectors.toSet()), o1.logicalAxioms().collect(Collectors.toSet()));
    }

    @Test
    public void testShouldAnnotateAndRoundTrip() throws OWLOntologyCreationException {
        String input = "Prefix: : <http://example.com/owl/families/>\n"
                + "Ontology: <http://example.com/owl/families>\n"
                + "Class: Person\n Annotations:  rdfs:comment \"Represents the set of all people.\"\n"
                + "Class: Man\n Annotations: rdfs:comment \"States that every man is a person.\"\n SubClassOf:  Person";
        OWLOntology o = loadOntologyFromString(input);
        OWLClass person = Class(IRI("http://example.com/owl/families/", "Person"));
        OWLClass man = Class(IRI("http://example.com/owl/families/", "Man"));
        Assertions.assertTrue(o.containsAxiom(Declaration(person)));
        Assertions.assertTrue(o.containsAxiom(Declaration(man)));
        Assertions.assertTrue(o.containsAxiom(SubClassOf(man, person)));
    }

    @Test
    public void testShouldParseCorrectly() {
        // given
        OWLClass a = Class(IRI(NS + "#", "A"));
        String text1 = "'GWAS study' and  has_publication_date some dateTime[< \"2009-01-01T00:00:00+00:00\"^^dateTime]";
        OWLClassExpression expected = df.getOWLObjectIntersectionOf(a, df.getOWLDataSomeValuesFrom(p, df
                .getOWLDatatypeRestriction(dateTime, OWLFacet.MAX_EXCLUSIVE, df.getOWLLiteral("2009-01-01T00:00:00+00:00",
                        dateTime))));
        // ontology creation including labels - this is the input ontology
        OWLOntology o = getOWLOntology();
        o.add(df.getOWLDeclarationAxiom(a), df.getOWLDeclarationAxiom(p), df.getOWLDeclarationAxiom(dateTime),
                annotation(a, "'GWAS study'"), annotation(p, "has_publication_date"), annotation(dateTime, "dateTime"));
        // select a short form provider that uses annotations
        ShortFormProvider sfp = new AnnotationValueShortFormProvider(Collections.singletonList(df.getRDFSLabel()), Collections
                .emptyMap(), m);
        BidirectionalShortFormProvider shortFormProvider = new BidirectionalShortFormProviderAdapter(m.ontologies().collect(Collectors.toList()), sfp);
        ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();
        parser.setStringToParse(text1);
        ShortFormEntityChecker owlEntityChecker = new ShortFormEntityChecker(shortFormProvider);
        parser.setOWLEntityChecker(owlEntityChecker);
        parser.setDefaultOntology(o);
        // when
        // finally parse
        OWLClassExpression dsvf = parser.parseClassExpression();
        // then
        Assertions.assertEquals(expected, dsvf);
    }

    public OWLAxiom annotation(OWLEntity e, String s) {
        return df.getOWLAnnotationAssertionAxiom(e.getIRI(), df.getRDFSLabel(s));
    }

    @Test
    public void testShouldDoPrecedenceWithParentheses() {
        // given
        String text1 = "(a and b) or c";
        OWLClass a = Class(IRI("urn:test#", "a"));
        OWLClass b = Class(IRI("urn:test#", "b"));
        OWLClass c = Class(IRI("urn:test#", "c"));
        OWLClassExpression expected = df.getOWLObjectUnionOf(df.getOWLObjectIntersectionOf(a, b), c);
        ManchesterOWLSyntaxParser parser = setupPArser(text1, expected);
        // when
        // finally parse
        OWLClassExpression dsvf = parser.parseClassExpression();
        // then
        Assertions.assertEquals(expected, dsvf, "Expected " + expected + " actual " + dsvf);
    }

    @Test
    public void testShouldParseCorrectlydecimal() {
        // given
        String text1 = "p some decimal[<=2.0, >= 1.0]";
        OWLDatatype decimal = df.getOWLDatatype(OWL2Datatype.XSD_DECIMAL);
        OWLFacetRestriction max = df.getOWLFacetRestriction(OWLFacet.MAX_INCLUSIVE, df.getOWLLiteral("2.0", decimal));
        OWLFacetRestriction min = df.getOWLFacetRestriction(OWLFacet.MIN_INCLUSIVE, df.getOWLLiteral("1.0", decimal));
        OWLClassExpression expected = df.getOWLDataSomeValuesFrom(p, df.getOWLDatatypeRestriction(decimal, max, min));
        // ontology creation including labels - this is the input ontology
        OWLOntology o = getOWLOntology();
        o.add(df.getOWLDeclarationAxiom(p), df.getOWLDeclarationAxiom(decimal), annotation(p, "p"));
        // select a short form provider that uses annotations
        ShortFormProvider sfp = new AnnotationValueShortFormProvider(Collections.singletonList(df.getRDFSLabel()), Collections
                .emptyMap(), m);
        BidirectionalShortFormProvider shortFormProvider = new BidirectionalShortFormProviderAdapter(m.ontologies().collect(Collectors.toList()), sfp);
        ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();
        parser.setStringToParse(text1);
        ShortFormEntityChecker owlEntityChecker = new ShortFormEntityChecker(shortFormProvider);
        parser.setOWLEntityChecker(owlEntityChecker);
        parser.setDefaultOntology(o);
        // when
        // finally parse
        OWLClassExpression dsvf = parser.parseClassExpression();
        // then
        Assertions.assertEquals(expected, dsvf);
    }

    @Test
    public void testShouldParseCorrectlydecimalNotSpecified() throws OWLOntologyCreationException {
        // given
        OWLAxiom expected = df.getOWLDataPropertyRangeAxiom(df.getOWLDataProperty("urn:test#", "a"), df.getOWLDataOneOf(
                df.getOWLLiteral("1.2", OWL2Datatype.XSD_DECIMAL)));
        String input = "Ontology:\n DataProperty: <urn:test#a>\n Range: {1.2}";
        OWLOntology o = loadOntologyFromString(input);
        o.logicalAxioms().forEach(ax -> Assertions.assertEquals(expected, ax));
    }

    @Test
    public void testShouldDoPrecedenceWithoutParentheses() {
        // given
        String text1 = "a and b or c";
        OWLClass a = Class(IRI("urn:test#", "a"));
        OWLClass b = Class(IRI("urn:test#", "b"));
        OWLClass c = Class(IRI("urn:test#", "c"));
        OWLClassExpression expected = df.getOWLObjectUnionOf(df.getOWLObjectIntersectionOf(a, b), c);
        ManchesterOWLSyntaxParser parser = setupPArser(text1, expected);
        // when
        // finally parse
        OWLClassExpression dsvf = parser.parseClassExpression();
        // then
        Assertions.assertEquals(expected, dsvf, "Expected " + expected + " actual " + dsvf);
    }

    protected ManchesterOWLSyntaxParser setupPArser(String text1, OWLClassExpression expected) {
        OWLClass a = Class(IRI("urn:test#", "a"));
        OWLClass b = Class(IRI("urn:test#", "b"));
        OWLClass c = Class(IRI("urn:test#", "c"));
        OWLClass d = Class(IRI("urn:test#", "all"));
        OWLOntology o = getOWLOntology();
        o.add(df.getOWLDeclarationAxiom(a), df.getOWLDeclarationAxiom(b), df.getOWLDeclarationAxiom(c), df
                .getOWLDeclarationAxiom(d), df.getOWLSubClassOfAxiom(expected, d));
        // select a short form provider that uses annotations
        ShortFormProvider sfp = new AnnotationValueShortFormProvider(Collections.singletonList(df.getRDFSLabel()), Collections
                .emptyMap(), m);
        BidirectionalShortFormProvider shortFormProvider = new BidirectionalShortFormProviderAdapter(m.ontologies()
                .collect(Collectors.toList()), sfp);
        ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();
        parser.setStringToParse(text1);
        ShortFormEntityChecker owlEntityChecker = new ShortFormEntityChecker(shortFormProvider);
        parser.setOWLEntityChecker(owlEntityChecker);
        parser.setDefaultOntology(o);
        return parser;
    }

    @Test
    public void testShouldNotFailOnAnnotations() throws Exception {
        String in = "Ontology(<http://x.org/>\n" + "Declaration(Class(<http://x.org/c>))\n"
                + "AnnotationAssertion(<http://x.org/p> <http://x.org/c> \"v1\")\n"
                + "AnnotationAssertion(<http://x.org/p> <http://x.org/c> \"orifice\")\n"
                + "AnnotationAssertion(Annotation(<http://x.org/p2> \"foo\") <http://x.org/p> <http://x.org/c> \"v1\"))";
        OWLOntology o = loadOntologyFromString(in);
        OWLOntology result = roundTrip(o, new ManchesterSyntaxDocumentFormat());
        o.axioms().forEach(ax -> Assertions.assertTrue(result.containsAxiom(ax)));
    }

    @Test
    public void testShouldNotFailSubclass() {
        // given
        OWLClass a = Class(IRI("urn:test#", "A"));
        OWLClass b = Class(IRI("urn:test#", "B"));
        String in = "A SubClassOf B";
        OWLOntology o = getOWLOntology();
        o.add(df.getOWLDeclarationAxiom(a), df.getOWLDeclarationAxiom(b));
        // select a short form provider that uses annotations
        ShortFormProvider sfp = new AnnotationValueShortFormProvider(Collections.singletonList(df.getRDFSLabel()),
                Collections.emptyMap(), m);
        BidirectionalShortFormProvider shortFormProvider = new BidirectionalShortFormProviderAdapter(m.ontologies()
                .collect(Collectors.toList()), sfp);
        ManchesterOWLSyntaxParser parser = OWLManager.createManchesterParser();
        parser.setStringToParse(in);
        ShortFormEntityChecker owlEntityChecker = new ShortFormEntityChecker(shortFormProvider);
        parser.setOWLEntityChecker(owlEntityChecker);
        parser.setDefaultOntology(o);
        // when
        // finally parse
        OWLAxiom axiom = parser.parseAxiom();
        // then
        Assertions.assertEquals(df.getOWLSubClassOfAxiom(a, b), axiom);
    }
}

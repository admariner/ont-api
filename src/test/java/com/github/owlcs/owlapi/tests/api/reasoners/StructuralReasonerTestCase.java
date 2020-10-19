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
package com.github.owlcs.owlapi.tests.api.reasoners;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class StructuralReasonerTestCase extends TestBase {

    @Test
    public void testClassHierarchy() {
        OWLClass clsX = OWLFunctionalSyntaxFactory.Class(iri("X"));
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLClass clsAp = OWLFunctionalSyntaxFactory.Class(iri("Ap"));
        OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("B"));
        OWLOntology ont = getOWLOntology();
        OWLOntologyManager man = ont.getOWLOntologyManager();
        man.addAxiom(ont, OWLFunctionalSyntaxFactory.EquivalentClasses(OWLFunctionalSyntaxFactory.OWLThing(), clsX));
        man.addAxiom(ont, OWLFunctionalSyntaxFactory.SubClassOf(clsB, clsA));
        man.addAxiom(ont, OWLFunctionalSyntaxFactory.EquivalentClasses(clsA, clsAp));
        StructuralReasoner reasoner = new StructuralReasoner(ont, new SimpleConfiguration(),
                BufferingMode.NON_BUFFERING);
        testClassHierarchy(reasoner);
        ont.add(OWLFunctionalSyntaxFactory.SubClassOf(clsA, OWLFunctionalSyntaxFactory.OWLThing()));
        testClassHierarchy(reasoner);
        ont.remove(OWLFunctionalSyntaxFactory.SubClassOf(clsA, OWLFunctionalSyntaxFactory.OWLThing()));
        testClassHierarchy(reasoner);
    }

    private static void testClassHierarchy(StructuralReasoner reasoner) {
        OWLClass clsX = OWLFunctionalSyntaxFactory.Class(iri("X"));
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLClass clsAp = OWLFunctionalSyntaxFactory.Class(iri("Ap"));
        OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("B"));
        NodeSet<OWLClass> subsOfA = reasoner.getSubClasses(clsA, true);
        Assertions.assertEquals(1, subsOfA.nodes().count());
        Assertions.assertTrue(subsOfA.containsEntity(clsB));
        NodeSet<OWLClass> subsOfAp = reasoner.getSubClasses(clsAp, true);
        Assertions.assertEquals(1, subsOfAp.nodes().count());
        Assertions.assertTrue(subsOfAp.containsEntity(clsB));
        Node<OWLClass> topNode = reasoner.getTopClassNode();
        NodeSet<OWLClass> subsOfTop = reasoner.getSubClasses(topNode.getRepresentativeElement(), true);
        Assertions.assertEquals(1, subsOfTop.nodes().count());
        Assertions.assertTrue(subsOfTop.containsEntity(clsA));
        NodeSet<OWLClass> descOfTop = reasoner.getSubClasses(topNode.getRepresentativeElement(), false);
        Assertions.assertEquals(3, descOfTop.nodes().count());
        Assertions.assertTrue(descOfTop.containsEntity(clsA));
        Assertions.assertTrue(descOfTop.containsEntity(clsB));
        Assertions.assertTrue(descOfTop.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> supersOfTop = reasoner.getSuperClasses(OWLFunctionalSyntaxFactory.OWLThing(), false);
        Assertions.assertTrue(supersOfTop.isEmpty());
        NodeSet<OWLClass> supersOfA = reasoner.getSuperClasses(clsA, false);
        Assertions.assertTrue(supersOfA.isTopSingleton());
        Assertions.assertEquals(1, supersOfA.nodes().count());
        Assertions.assertTrue(supersOfA.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        Node<OWLClass> equivsOfTop = reasoner.getEquivalentClasses(OWLFunctionalSyntaxFactory.OWLThing());
        Assertions.assertEquals(2, equivsOfTop.entities().count());
        Assertions.assertTrue(equivsOfTop.entities().anyMatch(x -> x.equals(clsX)));
    }
}

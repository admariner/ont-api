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
package com.github.owlcs.owlapi.tests.api.ontology;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class OntologyChangeListenerTestCase extends TestBase {

    @Test
    public void testOntologyChangeListener() {
        OWLOntology ont = getOWLOntology();
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("ClsA"));
        OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("ClsB"));
        OWLSubClassOfAxiom ax = OWLFunctionalSyntaxFactory.SubClassOf(clsA, clsB);
        final Set<OWLAxiom> impendingAdditions = new HashSet<>();
        final Set<OWLAxiom> impendingRemovals = new HashSet<>();
        final Set<OWLAxiom> additions = new HashSet<>();
        final Set<OWLAxiom> removals = new HashSet<>();
        ont.getOWLOntologyManager().addImpendingOntologyChangeListener(impendingChanges -> {
            for (OWLOntologyChange change : impendingChanges) {
                if (change.isAddAxiom()) {
                    impendingAdditions.add(change.getAxiom());
                } else if (change.isRemoveAxiom()) {
                    impendingRemovals.add(change.getAxiom());
                }
            }
        });
        ont.getOWLOntologyManager().addOntologyChangeListener(changes -> {
            for (OWLOntologyChange change : changes) {
                if (change.isAddAxiom()) {
                    additions.add(change.getAxiom());
                } else if (change.isRemoveAxiom()) {
                    removals.add(change.getAxiom());
                }
            }
        });
        ont.getOWLOntologyManager().addAxiom(ont, ax);
        Assert.assertTrue(additions.contains(ax));
        Assert.assertTrue(impendingAdditions.contains(ax));
        ont.remove(ax);
        Assert.assertTrue(removals.contains(ax));
        Assert.assertTrue(impendingRemovals.contains(ax));
        // test that no op changes are not broadcasted
        removals.clear();
        ont.remove(ax);
        Assert.assertFalse(removals.contains(ax));
        Assert.assertTrue(impendingRemovals.contains(ax));
    }
}

/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
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

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyDocumentAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import java.util.Optional;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class OWLOntologyManagerTestCase extends TestBase {

    @Test
    public void testCreateAnonymousOntology() {
        OWLOntology ontology = getAnonymousOWLOntology();
        Assertions.assertNotNull(ontology);
        Assertions.assertNotNull(ontology.getOntologyID());
        Assertions.assertFalse(ontology.getOntologyID().getDefaultDocumentIRI().isPresent());
        Assertions.assertFalse(ontology.getOntologyID().getOntologyIRI().isPresent());
        Assertions.assertFalse(ontology.getOntologyID().getVersionIRI().isPresent());
        Assertions.assertNotNull(m.getOntologyDocumentIRI(ontology));
    }

    @Test
    public void testCreateOntologyWithIRI() throws OWLOntologyCreationException {
        IRI ontologyIRI = IRI.getNextDocumentIRI(
                "http://www.semanticweb.org/ontologies/ontology");
        OWLOntology ontology = getOWLOntology(ontologyIRI);
        Assertions.assertNotNull(ontology);
        Assertions.assertNotNull(ontology.getOntologyID());
        Assertions.assertEquals(ontologyIRI, ontology.getOntologyID().getDefaultDocumentIRI().orElse(null));
        Assertions.assertEquals(ontologyIRI, ontology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertFalse(ontology.getOntologyID().getVersionIRI().isPresent());
        Assertions.assertEquals(ontologyIRI, m.getOntologyDocumentIRI(ontology));
    }

    @Test
    public void testCreateOntologyWithIRIAndVersionIRI() throws OWLOntologyCreationException {
        IRI ontologyIRI = IRI.getNextDocumentIRI("http://www.semanticweb.org/ontologies/ontology");
        IRI versionIRI = IRI.getNextDocumentIRI("http://www.semanticweb.org/ontologies/ontology/version");
        OWLOntology ontology = getOWLOntology(new OWLOntologyID(Optional.of(
                ontologyIRI), Optional.of(versionIRI)));
        Assertions.assertNotNull(ontology);
        Assertions.assertNotNull(ontology.getOntologyID());
        Assertions.assertEquals(versionIRI, ontology.getOntologyID().getDefaultDocumentIRI().orElse(null));
        Assertions.assertEquals(ontologyIRI, ontology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(versionIRI, ontology.getOntologyID().getVersionIRI().orElse(null));
        Assertions.assertEquals(versionIRI, m.getOntologyDocumentIRI(ontology));
    }

    @Test
    public void testCreateOntologyWithIRIWithMapper() throws OWLOntologyCreationException {
        IRI ontologyIRI = IRI.getNextDocumentIRI("http://www.semanticweb.org/ontologies/ontology");
        IRI documentIRI = IRI.getNextDocumentIRI("file:documentIRI");
        SimpleIRIMapper mapper = new SimpleIRIMapper(ontologyIRI, documentIRI);
        m.getIRIMappers().add(mapper);
        OWLOntology ontology = getOWLOntology(ontologyIRI);
        Assertions.assertNotNull(ontology);
        Assertions.assertNotNull(ontology.getOntologyID());
        Assertions.assertEquals(ontologyIRI, ontology.getOntologyID().getDefaultDocumentIRI().orElse(null));
        Assertions.assertEquals(ontologyIRI, ontology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertFalse(ontology.getOntologyID().getVersionIRI().isPresent());
        Assertions.assertEquals(documentIRI, m.getOntologyDocumentIRI(ontology));
    }

    @Test
    public void testCreateOntologyWithIRIAndVersionIRIWithMapper() throws OWLOntologyCreationException {
        IRI ontologyIRI = IRI.getNextDocumentIRI("http://www.semanticweb.org/ontologies/ontology");
        IRI versionIRI = IRI.getNextDocumentIRI("http://www.semanticweb.org/ontologies/ontology/version");
        IRI documentIRI = IRI.getNextDocumentIRI("file:documentIRI");
        SimpleIRIMapper mapper = new SimpleIRIMapper(versionIRI, documentIRI);
        m.getIRIMappers().add(mapper);
        OWLOntology ontology = getOWLOntology(new OWLOntologyID(Optional.of(ontologyIRI), Optional.of(versionIRI)));
        Assertions.assertNotNull(ontology, "ontology should not be null");
        Assertions.assertNotNull(ontology.getOntologyID(), "ontology id should not be null");
        Assertions.assertEquals(versionIRI, ontology.getOntologyID().getDefaultDocumentIRI().orElse(null));
        Assertions.assertEquals(ontologyIRI, ontology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(versionIRI, ontology.getOntologyID().getVersionIRI().orElse(null));
        Assertions.assertEquals(documentIRI, m.getOntologyDocumentIRI(ontology));
    }

    @Test
    public void testCreateDuplicateOntologyWithIRI() {
        Assertions.assertThrows(OWLOntologyAlreadyExistsException.class, () -> {
            IRI ontologyIRI = IRI.getNextDocumentIRI("http://www.semanticweb.org/ontologies/ontology");
            getOWLOntology(ontologyIRI);
            try {
                getOWLOntology(ontologyIRI);
            } catch (OntApiException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    public void testCreateDuplicateOntologyWithIRIAndVersionIRI() {
        Assertions.assertThrows(OWLOntologyAlreadyExistsException.class, () -> {
            IRI ontologyIRI = IRI.getNextDocumentIRI("http://www.semanticweb.org/ontologies/ontology");
            IRI versionIRI = IRI.getNextDocumentIRI("http://www.semanticweb.org/ontologies/ontology");
            getOWLOntology(new OWLOntologyID(Optional.of(ontologyIRI), Optional.of(versionIRI)));
            try {
                getOWLOntology(new OWLOntologyID(Optional.of(ontologyIRI), Optional.of(versionIRI)));
            } catch (OntApiException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    public void testCreateDuplicatedDocumentIRI() {
        Assertions.assertThrows(OWLOntologyDocumentAlreadyExistsException.class, () -> {
            IRI ontologyIRI = IRI.getNextDocumentIRI("http://www.semanticweb.org/ontologies/ontology");
            IRI ontologyIRI2 = IRI.getNextDocumentIRI("http://www.semanticweb.org/ontologies/ontology2");
            IRI documentIRI = IRI.getNextDocumentIRI("file:documentIRI");
            m.getIRIMappers().add(new SimpleIRIMapper(ontologyIRI, documentIRI));
            m.getIRIMappers().add(new SimpleIRIMapper(ontologyIRI2, documentIRI));
            getOWLOntology(new OWLOntologyID(Optional.of(ontologyIRI), Optional.empty()));
            try {
                getOWLOntology(new OWLOntologyID(Optional.of(ontologyIRI2), Optional.empty()));
            } catch (OntApiException e) {
                throw e.getCause();
            }
        });
    }
}

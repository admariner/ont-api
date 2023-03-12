/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.owlcs.owlapi.tests.api.fileroundtrip;

import com.github.owlcs.ontapi.OWLAdapter;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.AbstractRoundTrippingTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.RDFTriple;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;

import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class FileRoundTripSubClassOfUntypedOWLClassStrictTestCase extends AbstractRoundTrippingTestCase {

    private static final String FILE = "SubClassOfUntypedOWLClass.rdf";

    @Override
    protected OWLOntology createOntology() {
        return AbstractFileRoundTrippingTestCase.createOntology(FILE);
    }

    /**
     * ONT-API comment:
     * to make test passed the original (OWL-API) loading methods are used.
     * TODO: {@link OWLDocumentFormat#getOntologyLoaderMetaData()} is not supported in ONT-API
     * Currently I tend to thinking that we should not mindlessly repeat OWL-API behaviour.
     * Even if we are going to support it there would be problem with transformers:
     * Yhe only way to produce skipped declarations and incorrect graph is disabling transform-mechanism,
     * in this case there would not be any statistics to pass it inside the format.
     * Or what? Should we implement some checker for this special situation
     * when no graph-tuning are needed but anyway statistic is required?
     * Just note - statement rdfs:subClassOf uniquely identifies the class itself.
     */
    @Test
    public void testAxioms() {
        OWLOntologyLoaderConfiguration config = this.config.setStrict(true);
        if (!OWLManager.DEBUG_USE_OWL) {
            config = OWLAdapter.get().asONT(config).setUseOWLParsersToLoad(true);
        }
        OWLOntology ont = AbstractFileRoundTrippingTestCase.createOntology(FILE, OWLManager.createOWLOntologyManager(), config);
        OWLIOUtils.print(ont);
        Assertions.assertEquals(0, ont.axioms(AxiomType.SUBCLASS_OF).count());
        OWLDocumentFormat format = ont.getFormat();
        Assertions.assertTrue(format instanceof RDFXMLDocumentFormat);

        RDFXMLDocumentFormat rdfXmlFormat = (RDFXMLDocumentFormat) format;
        Assertions.assertTrue(rdfXmlFormat.getOntologyLoaderMetaData().isPresent());
        Stream<RDFTriple> triples = rdfXmlFormat.getOntologyLoaderMetaData()
                .orElseThrow(() -> new AssertionError("No loader meta data")).getUnparsedTriples();
        Assertions.assertEquals(1, triples.count());
    }

}

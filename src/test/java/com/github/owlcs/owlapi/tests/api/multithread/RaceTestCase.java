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
package com.github.owlcs.owlapi.tests.api.multithread;

import com.github.owlcs.owlapi.OWLManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RaceTestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaceTestCase.class);

    @Test
    public void testSubClassLHS() throws Exception {
        final int totalRepetitions = 200;
        int repetitions = 0;
        RaceTestCaseRunner r;
        do {
            repetitions++;
            r = new RaceTestCaseRunner();
            r.racing();
        } while (!r.callback.failed() && repetitions < totalRepetitions);
        if (r.callback.failed()) {
            r.callback.diagnose();
            Assertions.fail("Failed after " + repetitions + " repetition(s).");
        }
    }

    interface RaceCallback {

        void add();

        boolean failed();

        void diagnose();

        void race();
    }

    static class RaceTestCaseRunner {

        public static final String NS = "http://www.race.org#";
        protected RaceCallback callback;
        final AtomicBoolean done = new AtomicBoolean(false);
        ExecutorService exec = Executors.newFixedThreadPool(5);
        private final Runnable writer = () -> {
            while (!done.get()) {
                callback.add();
            }
            callback.add();
        };

        RaceTestCaseRunner() throws OWLOntologyCreationException {
            callback = new SubClassLHSCallback();
        }

        public void racing() throws InterruptedException {
            exec.submit(writer);
            callback.race();
            done.set(true);
            exec.shutdown();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }

        public static class SubClassLHSCallback implements RaceCallback {

            private final AtomicInteger counter = new AtomicInteger();
            OWLDataFactory factory;
            OWLOntologyManager manager;
            OWLOntology ontology;
            OWLClass x;
            OWLClass y;

            public SubClassLHSCallback() throws OWLOntologyCreationException {
                manager = OWLManager.createConcurrentOWLOntologyManager();
                factory = manager.getOWLDataFactory();
                ontology = manager.createOntology();
                x = factory.getOWLClass(IRI.create(NS, "X"));
                y = factory.getOWLClass(IRI.create(NS, "Y"));
            }

            @Override
            public void add() {
                OWLClass middle = createMiddleClass(counter.getAndIncrement());
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("add {}", middle);
                Set<OWLAxiom> axioms = computeChanges(middle);
                ontology.add(axioms);
            }

            @Override
            public boolean failed() {
                long size = computeSize();
                return size < counter.get();
            }

            public long computeSize() {
                return ontology.subClassAxiomsForSubClass(x).count();
            }

            public Set<OWLAxiom> computeChanges(OWLClass middle) {
                OWLAxiom axiom1 = factory.getOWLSubClassOfAxiom(x, middle);
                OWLAxiom axiom2 = factory.getOWLSubClassOfAxiom(middle, y);
                Set<OWLAxiom> axioms = new HashSet<>();
                axioms.add(axiom1);
                axioms.add(axiom2);
                return axioms;
            }

            @Override
            public void diagnose() {
                List<OWLSubClassOfAxiom> axiomsFound = ontology.subClassAxiomsForSubClass(x).collect(Collectors.toList());
                LOGGER.debug("Expected getSubClassAxiomsForSubClass to return {} axioms but it only found {}",
                        counter, axiomsFound.size());
                for (int i = 0; i < counter.get(); i++) {
                    OWLAxiom checkMe = factory.getOWLSubClassOfAxiom(x, createMiddleClass(i));
                    if (ontology.subClassAxiomsForSubClass(x).noneMatch(checkMe::equals)
                            && ontology.containsAxiom(checkMe)) {
                        LOGGER.debug("{} is an axiom in the ontology that is not found by getSubClassAxiomsForSubClass", checkMe);
                        return;
                    }
                }
            }

            @Override
            public void race() {
                List<OWLSubClassOfAxiom> list1 = ontology
                        .subClassAxiomsForSubClass(factory.getOWLClass(IRI.create(NS, "testclass")))
                        .collect(Collectors.toList());
                List<OWLSubClassOfAxiom> list2 = ontology
                        .subClassAxiomsForSubClass(x)
                        .collect(Collectors.toList());
                List<OWLSubClassOfAxiom> list3 = ontology
                        .subClassAxiomsForSubClass(y)
                        .collect(Collectors.toList());
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("race: {}, {}, {}", list1, list2, list3);
            }

            public OWLClass createMiddleClass(int i) {
                return factory.getOWLClass(IRI.create(NS, "P" + i));
            }
        }
    }
}

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

package com.github.owlcs.ontapi.tests.managers;

import com.github.owlcs.ontapi.ID;
import com.github.owlcs.ontapi.NoOpReadWriteLock;
import com.github.owlcs.ontapi.OntologyCollection;
import com.github.owlcs.ontapi.OntologyCollectionImpl;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import org.apache.jena.graph.Node;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.HasOntologyID;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * To test some internal managers components.
 * Created by @ssz on 09.12.2018.
 */
public class InternalManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalManagerTest.class);
    private static final PrintStream OUT = OWLIOUtils.NULL_OUT;

    private static void performSomeModifying(OntologyCollection<IDHolder> list) {
        Random r = ThreadLocalRandom.current();
        addRandom(list, r, 5000);
        OUT.println(list);

        list.clear();
        addRandom(list, r, 1500);
        changeVersionIRIs(list);
        OUT.println(list);

        changeOntologyIRIs(list);
        list.keys().skip(r.nextInt((int) list.size() + 1)).collect(Collectors.toList()).forEach(list::remove);
        OUT.println(list);

        addRandom(list, r, 3000);
        changeVersionIRIs(list);
        changeOntologyIRIs(list);
        OUT.println(list);

        list.values().limit(r.nextInt((int) list.size() + 1)).collect(Collectors.toSet()).forEach(list::delete);
        OUT.println(list);
        list.clear();
    }

    private static void addRandom(OntologyCollection<IDHolder> list, Random r, int count) {
        IntStream.rangeClosed(0, r.nextInt(count) + 1)
                .mapToObj(i -> IDHolder.of(String.valueOf(i)))
                .forEach(list::add);
    }

    private static void changeOntologyIRIs(OntologyCollection<IDHolder> list) {
        list.values().forEach(o -> {
            String iri = o.getOntologyIRI();
            Assertions.assertNotNull(iri);
            o.setOntologyIRI(iri + "_x");
        });
    }

    private static void changeVersionIRIs(OntologyCollection<IDHolder> list) {
        list.values().forEach(o -> {
            String ver = o.getVersionIRI();
            if (ver == null) {
                ver = "y";
            } else {
                ver += "_y";
            }
            o.setVersionIRI(ver);
        });
    }

    @Test
    public void testCommonOntologyCollection() {
        OntologyCollection<IDHolder> list1 = new OntologyCollectionImpl<>();
        Assertions.assertTrue(list1.isEmpty());
        Assertions.assertEquals(0, list1.size());

        IDHolder a = IDHolder.of("a");
        IDHolder b = IDHolder.of("b");
        list1.add(a).add(b);
        LOGGER.debug("1) List: {}", list1);
        Assertions.assertEquals(2, list1.size());
        Assertions.assertFalse(list1.isEmpty());
        Assertions.assertTrue(list1.get(ID.create("a", null)).isPresent());
        Assertions.assertFalse(list1.get(ID.create("a", "v")).isPresent());
        Assertions.assertTrue(list1.contains(b.getOntologyID()));
        Assertions.assertFalse(list1.contains(ID.create("a", "v")));

        // change id externally for 'b':
        b.setOntologyID(IDHolder.of("x").getOntologyID());
        LOGGER.debug("2) List: {}", list1);
        list1.delete(b).remove(a.getOntologyID());
        Assertions.assertEquals(0, list1.size());
        Assertions.assertTrue(list1.isEmpty());

        OntologyCollection<IDHolder> list2 = new OntologyCollectionImpl<>(NoOpReadWriteLock.NO_OP_RW_LOCK,
                Arrays.asList(a, b));
        LOGGER.debug("3) List: {}", list2);
        Assertions.assertEquals(2, list2.size());
        Assertions.assertEquals(2, list2.values().peek(x -> LOGGER.debug("{}", x)).count());
        Assertions.assertFalse(list2.isEmpty());
        Assertions.assertFalse(list2.get(ID.create("b", null)).isPresent());
        Assertions.assertTrue(list2.get(ID.create("x", null)).isPresent());
        Assertions.assertEquals(Arrays.asList("a", "x"), list2.keys()
                .map(ID::asONT).map(ID::asNode)
                .map(Node::getURI).sorted().collect(Collectors.toList()));
        list2.clear();
        Assertions.assertTrue(list2.isEmpty());
        Assertions.assertEquals(0, list2.size());
        Assertions.assertEquals(0, list2.values().count());
        list2.add(b).add(a);
        LOGGER.debug("4) List: {}", list2);

        // change id externally for 'a':
        a.setOntologyID(ID.create("x", null));
        LOGGER.debug("5) List: {}", list2);
        Assertions.assertEquals(2, list2.size());
        Assertions.assertEquals(Arrays.asList("x", "x"), list2.keys()
                .map(ID::asONT).map(ID::asNode)
                .map(Node::getURI).sorted().collect(Collectors.toList()));
        Assertions.assertSame(b, list2.get(ID.create("x", null)).orElseThrow(AssertionError::new));
        Assertions.assertNotSame(a, list2.get(ID.create("x", null)).orElseThrow(AssertionError::new));

        // change id externally for 'b':
        b.setOntologyID(ID.create("x", "v"));
        LOGGER.debug("6) List: {}", list2);
        Set<OWLOntologyID> keys = list2.keys().collect(Collectors.toSet());
        LOGGER.debug("Keys: {}", keys);
        Assertions.assertEquals(2, keys.size());
        Assertions.assertSame(a, list2.get(ID.create("x", null)).orElseThrow(AssertionError::new));
        Assertions.assertNotSame(b, list2.get(ID.create("x", null)).orElseThrow(AssertionError::new));

        // change id externally for 'a' and 'b':
        a.setOntologyID(ID.create("y", null));
        b.setOntologyID(ID.create("x", null));
        LOGGER.debug("7) List: {}", list2);
        Assertions.assertEquals(2, list2.values().peek(x -> LOGGER.debug("{}", x)).count());
        Assertions.assertSame(b, list2.get(ID.create("x", null)).orElseThrow(AssertionError::new));
        Assertions.assertSame(a, list2.get(ID.create("y", null)).orElseThrow(AssertionError::new));
    }

    @Test
    public void testConcurrentModificationOfNonSynchronizedList() {
        Assertions.assertThrows(Exception.class,
                () -> testConcurrentModification(new OntologyCollectionImpl<>(NoOpReadWriteLock.NO_OP_RW_LOCK)));
    }

    @Test
    public void testConcurrentModificationOfRWList() throws Exception {
        testConcurrentModification(new OntologyCollectionImpl<>(new ReentrantReadWriteLock()));
    }

    private void testConcurrentModification(OntologyCollection<IDHolder> list) throws ExecutionException, InterruptedException {
        int num = 15;
        ExecutorService service = Executors.newFixedThreadPool(8);
        List<Future<?>> res = new ArrayList<>();
        LOGGER.debug("Start. The collection: {}", list);
        for (int i = 0; i < num; i++)
            res.add(service.submit(() -> performSomeModifying(list)));
        service.shutdown();
        for (Future<?> f : res) {
            f.get();
            LOGGER.debug("Run. The collection ({}): {}", list.size(), list);
        }
        LOGGER.debug("Fin. The collection: {}", list);
        Assertions.assertTrue(list.isEmpty());
    }

    @SuppressWarnings("WeakerAccess")
    public static class IDHolder implements HasOntologyID {
        private ID id;

        private IDHolder(ID id) {
            this.id = Objects.requireNonNull(id);
        }

        public static IDHolder of(String iri, String ver) {
            return new IDHolder(ID.create(iri, ver));
        }

        public static IDHolder of(String iri) {
            return of(iri, null);
        }

        @Override
        public ID getOntologyID() {
            return id;
        }

        public void setOntologyID(OWLOntologyID id) {
            this.id = ID.asONT(id);
        }

        public String getOntologyIRI() {
            return id.getOntologyIRI().map(IRI::getIRIString).orElse(null);
        }

        public void setOntologyIRI(String x) {
            setOntologyID(ID.create(x, getVersionIRI()));
        }

        public String getVersionIRI() {
            return id.getVersionIRI().map(IRI::getIRIString).orElse(null);
        }

        public void setVersionIRI(String x) {
            setOntologyID(ID.create(getOntologyIRI(), x));
        }

        @Override
        public String toString() {
            return String.format("%s[%s & %s]@%s",
                    getClass().getSimpleName(),
                    getOntologyIRI(),
                    getVersionIRI(),
                    Integer.toHexString(hashCode()));
        }
    }
}

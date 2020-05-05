/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.tests.ModelData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.*;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by @ssz on 05.05.2020.
 */
@RunWith(Parameterized.class)
public class SearchByObjectTest {
    private final TestData data;

    public SearchByObjectTest(TestData data) {
        this.data = data;
    }

    @Parameterized.Parameters(name = "{0}")
    public static TestData[] getData() {
        return TestData.values();
    }

    private static OWLOntologyManager newManager() {
        return OntManagers.createONT();
    }

    @Test
    public void testDeclarations() {
        data.doTest(T.DECLARATIONS, HasSignature::signature);
    }

    @Test
    public void testSubClassAxiomsForSubClass() {
        data.doTest(T.SUB_CLASS_OF_BY_SUBJECT, HasClassesInSignature::classesInSignature);
    }

    @Test
    public void testEquivalentClassesAxioms() {
        data.doTest(T.EQUIVALENT_CLASS_BY_OPERAND, HasClassesInSignature::classesInSignature);
    }

    enum TestData {
        PIZZA(ModelData.PIZZA,
                T.DECLARATIONS.of(-5190508530L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(23994790843L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(743207879L)
        ),
        FAMILY(ModelData.FAMILY,
                T.DECLARATIONS.of(34226271096L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-405443220L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(1149278276L)
        ),
        PEOPLE(ModelData.PEOPLE,
                T.DECLARATIONS.of(-31040926516L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-6044474129L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(229986282L)
        ),
        CAMERA(ModelData.CAMERA,
                T.DECLARATIONS.of(2967944221L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(3537056616L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(2619197590L)
        ),
        KOALA(ModelData.KOALA,
                T.DECLARATIONS.of(6488467972L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-4740693142L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(1433290824L)
        ),
        TRAVEL(ModelData.TRAVEL,
                T.DECLARATIONS.of(-25825023334L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(3792566851L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(-1797460855L)
        ),
        WINE(ModelData.WINE,
                T.DECLARATIONS.of(20065711780L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(23989074593L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(15637987080L)
        ),
        FOOD(ModelData.FOOD,
                T.DECLARATIONS.of(6794851452L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-2766054837L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(16744408703L)
        ),
        NCBITAXON_CUT(ModelData.NCBITAXON_CUT,
                T.DECLARATIONS.of(244310200631L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-1220817325L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of()
        ),
        HP_CUT(ModelData.HP_CUT,
                T.DECLARATIONS.of(-14640456193L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-2245851740L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(-1475922263L)
        ),
        FAMILY_PEOPLE_UNION(ModelData.FAMILY_PEOPLE_UNION,
                T.DECLARATIONS.of(-637777500L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-730374961L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(1108552553L)
        ),
        ;
        private final ModelData resource;
        private final ByPrimitiveTester[] expectations;

        TestData(ModelData data, ByPrimitiveTester... expectations) {
            this.resource = data;
            this.expectations = expectations;
        }

        public ByPrimitiveTester getTester(T type) {
            return Arrays.stream(expectations)
                    .filter(x -> x.type.equals(type.name()))
                    .findFirst().orElseThrow(IllegalArgumentException::new);
        }

        void doTest(T type, Function<OWLOntology, Stream<? extends OWLPrimitive>> getPrimitives) {
            OWLOntology ont = load(newManager());
            getTester(type).testAxiomsCounts(ont, getPrimitives);
        }

        public OWLOntology load(OWLOntologyManager manager) {
            return resource.fetch(manager);
        }
    }

    enum T {
        DECLARATIONS {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.declarationAxioms((OWLEntity) param);
            }
        },
        SUB_CLASS_OF_BY_SUBJECT {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.subClassAxiomsForSubClass((OWLClass) param);
            }
        },
        EQUIVALENT_CLASS_BY_OPERAND {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.equivalentClassesAxioms((OWLClass) param);
            }
        },
        ;

        private ByPrimitiveTester of() {
            return of(0);
        }

        private ByPrimitiveTester of(long count) {
            return new ByPrimitiveTester(name(), count, this::listAxioms);
        }

        abstract Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param);
    }


}

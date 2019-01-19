/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena.impl.conf;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.utils.Iter;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A {@link ru.avicomp.ontapi.jena.model.OntGraphModel Ontology RDF Model} configuration object,
 * that serves for the following purposes:
 * <ul>
 * <li>Defines a set of permitted mappings from [interface] Class objects
 * to {@link ObjectFactory OWL Ontology Object factory}
 * that can generate instances of the facet represented by the Class.</li>
 * <li>Defines a set of builtin {@link OntEntity OWL entities}</li>
 * <li>Defines a set of OWL punnings</li>
 * <li>Defines a set of reserved {@link Resource}s and {@link Property}s, that cannot be used as OWL Entities</li>
 * </ul>
 * <b>NOTE</b>: the instance of this interface must also extend {@link org.apache.jena.enhanced.Personality Jena Personality}
 * with a generic type {@link org.apache.jena.rdf.model.RDFNode RDFNode}.
 * Unfortunately, the Jena {@code Personality} is a concrete and mutable class,
 * this fact makes it unacceptable to use it directly within the ONT-API.
 * Also, there is another related restriction:
 * the main {@link ru.avicomp.ontapi.jena.model.OntGraphModel OntGraphModel} interface must also implement
 * {@link ru.avicomp.ontapi.jena.impl.PersonalityModel PersonalityModel}.
 * <p>
 * Instances of this class must be unmodifiable.
 * <p>
 * To create an instance use {@link PersonalityBuilder}.
 * <p>
 * Created by @szz on 15.01.2019.
 */
public interface OntPersonality {

    /**
     * Lists all object-types, which are supported by this personality configuration.
     * Each of the class-types are associated with either {@link ObjectFactory} (if it is {@link OntObject} type)
     * or with {@link org.apache.jena.enhanced.Implementation} (if it is a standard jena resource object).
     *
     * @return Stream of {@code Class}es, sub-classes of {@link RDFNode}.
     */
    Stream<Class<? extends RDFNode>> types();

    /**
     * Gets the implementation-factory for the specified object type,
     * returning {@code null} if there isn't one available.
     *
     * @param type a class-type of {@link OntObject}
     * @return {@link ObjectFactory} a factory to create an instance of the given type
     */
    ObjectFactory getObjectFactory(Class<? extends OntObject> type);

    /**
     * Makes a full copy of this configuration.
     *
     * @return {@link OntPersonality} a new instance identical to this
     */
    OntPersonality copy();

    /**
     * Returns a punnings vocabulary.
     *
     * @return {@link Punnings}
     */
    Punnings getPunnings();

    /**
     * Returns a builtins vocabulary.
     *
     * @return {@link Builtins}
     */
    Builtins getBuiltins();

    /**
     * Returns a reserved vocabulary.
     *
     * @return {@link Reserved}
     */
    Reserved getReserved();

    /**
     * A vocabulary of built-in {@link OntEntity OWL Entities}.
     * A {@link ru.avicomp.ontapi.jena.model.OntGraphModel model}, that holds this configuration,
     * can contain entities without explicit declarations, it they IRIs are determined by this vocabulary.
     * <p>
     * For example, the OWL standard vocabulary determines
     * {@link ru.avicomp.ontapi.jena.vocabulary.OWL#Thing owl:Thing} as builtin OWL class.
     * In this case the expression {@code voc.get(OntClass.class)}, where {@code voc} is an instance of this class,
     * will returns a {@code Set} containing {@code owl:Thing} in the form of {@link Node}.
     *
     * @see OntEntity#types()
     */
    interface Builtins extends Vocabulary.Entities {

        /**
         * Returns a {@code Set} of all OWL builtin properties
         * (annotation, datatype and object named property expressions)
         *
         * @return Set of IRI-{@link Node node}s
         */
        default Set<Node> getProperties() {
            return Stream.of(getObjectProperties(), getAnnotationProperties(), getDatatypeProperties())
                    .flatMap(Collection::stream).collect(Iter.toUnmodifiableSet());
        }

    }

    /**
     * A punnings vocabulary.
     * For a given {@link OntEntity} type it returns a {@code Set} of forbidden types.
     * A {@link ru.avicomp.ontapi.jena.model.OntGraphModel model}, that holds this configuration,
     * cannot contain entities which have intersection in {@link ru.avicomp.ontapi.jena.vocabulary.RDF#type rdf:type}
     * that are determined by this vocabulary.
     * <p>
     * For example, for the {@link OntModelConfig.StdMode#MEDIUM} and {@link OntModelConfig.StdMode#STRICT}
     * configurations, the expression {@code voc.get(OntClass.class)}, where {@code voc} is an instance of this class,
     * will returns a {@code Set}
     * containing {@link org.apache.jena.vocabulary.RDFS#Datatype rdfs:Datatype} in the form of {@link Node}.
     *
     * @see <a href='https://www.w3.org/TR/owl2-new-features/#F12:_Punning'>Punnings</a>
     * @see OntEntity#types()
     */
    interface Punnings extends Vocabulary.Entities {
    }

    /**
     * A vocabulary of reserved IRIs.
     * A {@link ru.avicomp.ontapi.jena.model.OntGraphModel model}, that holds this configuration,
     * cannot contain entities with the IRIs from this vocabulary.
     * <p>
     * Example of such a forbidden {@link Property} returned by this is
     * {@link ru.avicomp.ontapi.jena.vocabulary.OWL#sameAs owl:sameAs}.
     * An {@link ru.avicomp.ontapi.jena.model.OntGraphModel ontology model} cannot contain an OWL entity with this IRI.
     * But, of course, all these things are customizable, and vocabularies may contain more or less restrictions.
     */
    interface Reserved extends Vocabulary<Resource> {

        /**
         * Gets a Set of reserved nodes by a key, using a {@code loader} to calculate the result
         * if it absences in the vocabulary.
         * <p>
         * Important: the given {@code loader} must not use external (e.g. the model) resources to infer the result.
         * It can use only {@link OntPersonality} resources
         * (from this or {@link Punnings} and {@link Builtins} vocabularies).
         * The {@link Reserved} vocabulary, as anything in {@link OntPersonality configuration},
         * must be unmodifiable, and this method is just a back door,
         * a possibility to lazily assembly a constant, which will be used by various models,
         * and should <b>never</b> be changed.
         *
         * @param key    String, not {@code null}
         * @param loader {@code Supplier} to construct a Set of {@link Node}s for a given {@code key}
         * @return Set of IRI-{@link Node node}s
         */
        Set<Node> get(String key, Supplier<Set<Node>> loader);

        /**
         * Gets all reserved IRIs, which are most likely used as an object in SPO of some schema graph triple.
         *
         * @return Set of IRI-{@link Node node}s
         */
        default Set<Node> getResources() {
            return get(Resource.class);
        }

        /**
         * Gets all reserved IRIs, which are most likely used as a predicate in SPO of some schema graph triple.
         *
         * @return Set of IRI-{@link Node node}s
         */
        default Set<Node> getProperties() {
            return get(Property.class);
        }
    }

    /**
     * Represents the given {@code OntPersonality} configuration as
     * a {@link Personality Jena Personality} with a generic type {@link RDFNode}.
     *
     * @param p {@link OntPersonality}
     * @return {@link Personality}
     * @throws OntJenaException in case the conversion is not possible
     * @see ObjectFactory#asJenaImplementation(ObjectFactory)
     * @see ru.avicomp.ontapi.jena.impl.PersonalityModel#asPersonalityModel(EnhGraph)
     */
    @SuppressWarnings("unchecked")
    static Personality<RDFNode> asJenaPersonality(OntPersonality p) throws OntJenaException {
        if (p instanceof Personality) {
            return (Personality<RDFNode>) p;
        }
        throw new OntJenaException.IllegalArgument("The given OntPersonality is not an instance of Jena Personality.");
    }

}

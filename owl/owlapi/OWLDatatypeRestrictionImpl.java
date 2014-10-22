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
package uk.ac.manchester.cs.owl.owlapi;

import static org.semanticweb.owlapi.util.OWLAPIPreconditions.checkNotNull;

import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.model.DataRangeType;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.util.CollectionFactory;
import org.semanticweb.owlapi.util.OWLObjectTypeIndexProvider;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health
 *         Informatics Group
 * @since 2.0.0
 */
public class OWLDatatypeRestrictionImpl extends
        OWLObjectImplWithoutEntityAndAnonCaching implements
        OWLDatatypeRestriction {

    private static final long serialVersionUID = 40000L;
    @Nonnull
    private final OWLDatatype datatype;
    @Nonnull
    private final Set<OWLFacetRestriction> facetRestrictions;

    @Override
    protected int index() {
        return OWLObjectTypeIndexProvider.DATA_TYPE_INDEX_BASE + 6;
    }

    /**
     * @param datatype
     *        datatype
     * @param facetRestrictions
     *        facet restriction
     */
    public OWLDatatypeRestrictionImpl(@Nonnull OWLDatatype datatype,
            @Nonnull Set<OWLFacetRestriction> facetRestrictions) {
        this.datatype = checkNotNull(datatype, "datatype cannot be null");
        this.facetRestrictions = new TreeSet<>(checkNotNull(facetRestrictions,
                "facetRestrictions cannot be null"));
    }

    @Override
    public void addSignatureEntitiesToSet(Set<OWLEntity> entities) {
        entities.add(datatype);
        facetRestrictions.forEach(f -> addSignatureEntitiesToSetForValue(
                entities, f));
    }

    @Override
    public void addAnonymousIndividualsToSet(Set<OWLAnonymousIndividual> anons) {
        facetRestrictions.forEach(r -> addAnonymousIndividualsToSetForValue(
                anons, r));
    }

    @Override
    public DataRangeType getDataRangeType() {
        return DataRangeType.DATATYPE_RESTRICTION;
    }

    @Override
    public OWLDatatype getDatatype() {
        return datatype;
    }

    @Override
    public Set<OWLFacetRestriction> getFacetRestrictions() {
        return CollectionFactory.copy(facetRestrictions);
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            if (!(obj instanceof OWLDatatypeRestriction)) {
                return false;
            }
            OWLDatatypeRestriction other = (OWLDatatypeRestriction) obj;
            return other.getDatatype().equals(datatype)
                    && other.getFacetRestrictions().equals(facetRestrictions);
        }
        return false;
    }

    @Override
    protected int compareObjectOfSameType(OWLObject object) {
        OWLDatatypeRestriction other = (OWLDatatypeRestriction) object;
        int diff = datatype.compareTo(other.getDatatype());
        if (diff != 0) {
            return diff;
        }
        return compareSets(facetRestrictions, other.getFacetRestrictions());
    }
}

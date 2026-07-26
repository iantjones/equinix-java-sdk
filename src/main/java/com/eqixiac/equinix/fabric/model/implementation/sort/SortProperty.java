package com.eqixiac.equinix.fabric.model.implementation.sort;

import com.eqixiac.equinix.core.enums.SortOrder;
import com.eqixiac.equinix.fabric.model.serializers.FilterPropertySerializer;
import com.eqixiac.equinix.fabric.model.serializers.SortPropertySerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

@JsonSerialize(using = SortPropertySerializer.class)
@Getter
public class SortProperty {

    private final SortOrder sortOrder;

    private final String property;

    public SortProperty(SortOrder sortOrder, String property) {
        this.sortOrder = sortOrder;
        this.property = property;
    }
}
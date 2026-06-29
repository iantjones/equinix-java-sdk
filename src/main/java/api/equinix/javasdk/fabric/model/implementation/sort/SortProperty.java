package api.equinix.javasdk.fabric.model.implementation.sort;

import api.equinix.javasdk.core.enums.SortOrder;
import api.equinix.javasdk.fabric.model.serializers.FilterPropertySerializer;
import api.equinix.javasdk.fabric.model.serializers.SortPropertySerializer;
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
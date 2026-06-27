package api.equinix.javasdk.fabric.model.implementation.pagination;

import api.equinix.javasdk.fabric.model.serializers.FilterPropertySerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

//@JsonSerialize(using = FilterPropertySerializer.class)
@Getter
public class PaginationProperty {

    private final Integer limit;

    private final Integer offset;

    public PaginationProperty(Integer limit, Integer offset) {
        this.limit = limit;
        this.offset = offset;
    }
}
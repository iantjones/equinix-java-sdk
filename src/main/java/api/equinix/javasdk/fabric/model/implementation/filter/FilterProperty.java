package api.equinix.javasdk.fabric.model.implementation.filter;

import api.equinix.javasdk.fabric.model.serializers.FilterPropertySerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

import java.util.List;

@JsonSerialize(using = FilterPropertySerializer.class)
@Getter
public class FilterProperty {

    private final FilterOperator operator;

    private final String property;

    private final List<Object> values;

    public FilterProperty(FilterOperator operator, String property, List<Object> values) {
        this.operator = operator;
        this.property = property;
        this.values = values;
    }
}
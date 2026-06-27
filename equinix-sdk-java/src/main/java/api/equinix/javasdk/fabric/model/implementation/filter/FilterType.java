package api.equinix.javasdk.fabric.model.implementation.filter;

public enum FilterType {

    AND("and"),
    OR("or");

    private final String literal;

    FilterType(String literal) {
        this.literal = literal;
    }

    public String literal() {
        return literal;
    }
}
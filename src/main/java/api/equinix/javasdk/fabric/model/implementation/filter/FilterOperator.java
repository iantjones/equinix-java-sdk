package api.equinix.javasdk.fabric.model.implementation.filter;

public enum FilterOperator {

    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    GREATER_THAN_EQUALS(">="),
    LESS_THAN("<"),
    LESS_THAN_EQUALS("<="),
    BETWEEN("BETWEEN"),
    NOT_BETWEEN("NOT BETWEEN"),
    LIKE("LIKE"),
    NOT_LIKE("NOT LIKE"),
    IN("IN"),
    NOT_IN("NOT_IN"),
    IS_NOT_NULL("IS NOT NULL"),
    IS_NULL("IS NULL");

    private final String literal;

    FilterOperator(String literal) {
        this.literal = literal;
    }

    public String literal() {
        return literal;
    }
}
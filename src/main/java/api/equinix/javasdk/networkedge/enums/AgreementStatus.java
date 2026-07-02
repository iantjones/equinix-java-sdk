package api.equinix.javasdk.networkedge.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Status of an agreement-acceptance request (spec {@code AgreementAcceptResponse.status}: {@code SUCCESS} /
 * {@code FAILED}). {@link #UNKNOWN} is a read-side fallback for values added after this SDK release — never
 * send it.
 *
 * @author ianjones
 */
public enum AgreementStatus {
    SUCCESS,
    FAILED,
    /** Legacy value retained for compatibility; not cited by the current spec. */
    PROCESSED,
    UNKNOWN;

    @JsonCreator
    public static AgreementStatus fromString(String value) {
        try { return AgreementStatus.valueOf(value); }
        catch (Exception e) { return UNKNOWN; }
    }
}

package api.equinix.javasdk.customerportal.model.implementation;

import api.equinix.javasdk.customerportal.enums.InvoiceDocumentFileType;
import api.equinix.javasdk.customerportal.enums.InvoiceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * A downloadable document attached to an invoice (Billing v1 finance accounts API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingInvoiceDocument {

    @JsonProperty("documentId")
    private String documentId;

    @JsonProperty("fileType")
    private InvoiceDocumentFileType fileType;

    @JsonProperty("type")
    private InvoiceType type;
}

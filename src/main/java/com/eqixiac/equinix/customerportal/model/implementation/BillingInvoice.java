package com.eqixiac.equinix.customerportal.model.implementation;

import com.eqixiac.equinix.core.model.deserializers.LocalDateDeserializer;
import com.eqixiac.equinix.customerportal.enums.InvoiceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * An invoice available on a billing account (Billing v1 finance accounts API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingInvoice {

    @JsonProperty("invoiceId")
    private String invoiceId;

    @JsonProperty("transactionNumber")
    private String transactionNumber;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("date")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate date;

    @JsonProperty("type")
    private InvoiceType type;

    @JsonProperty("documents")
    private List<BillingInvoiceDocument> documents;
}

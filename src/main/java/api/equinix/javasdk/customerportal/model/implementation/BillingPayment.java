package api.equinix.javasdk.customerportal.model.implementation;

import api.equinix.javasdk.core.model.deserializers.LocalDateDeserializer;
import api.equinix.javasdk.customerportal.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.time.LocalDate;

/**
 * A payment recorded against a billing account (Billing v1 finance accounts API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingPayment {

    @JsonProperty("paymentId")
    private String paymentId;

    @JsonProperty("date")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate date;

    @JsonProperty("status")
    private PaymentStatus status;

    @JsonProperty("amount")
    private Double amount;
}

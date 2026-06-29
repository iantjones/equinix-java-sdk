package api.equinix.javasdk.customerportal.model.implementation;

import api.equinix.javasdk.core.model.deserializers.LocalDateDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reference to a prior invoice or credit memo adjusted on an invoice summary (Billing v2 API).
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriorAdjustmentInfo {

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

    @JsonProperty("transactionDate")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate transactionDate;
}

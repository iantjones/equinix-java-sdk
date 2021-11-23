package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.enums.AccountType;
import api.equinix.javasdk.customerportal.model.InvoiceSummary;
import api.equinix.javasdk.customerportal.model.Reseller;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class ResellerJson {

    @Getter static TypeReference<Page<Reseller, ResellerJson>> pagedTypeRef = new TypeReference<>() {};

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("accountName")
    private String accountName;

    @JsonProperty("accountType")
    private AccountType accountType;

    @JsonProperty("ibxs")
    private List<String> ibxs;
}

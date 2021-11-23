package api.equinix.javasdk.customerportal.model.json;

import api.equinix.javasdk.core.http.response.Page;
import api.equinix.javasdk.customerportal.enums.AccountStatus;
import api.equinix.javasdk.customerportal.enums.Service;
import api.equinix.javasdk.customerportal.model.ResellerCustomer;
import api.equinix.javasdk.customerportal.model.implementation.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;

import java.util.List;

@Getter
public class ResellerCustomerJson {

    @Getter static TypeReference<Page<ResellerCustomer, ResellerCustomerJson>> pagedTypeRef = new TypeReference<>() {};
    @Getter static TypeReference<ResellerCustomerJson> singleTypeRef = new TypeReference<>() {};

    @JsonProperty("customerAccountNumber")
    private String customerAccountNumber;

    @JsonProperty("customerAccountName")
    private String customerAccountName;

    @JsonProperty("status")
    private AccountStatus status;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("resellerNotificationContact")
    private String resellerNotificationContact;

    @JsonProperty("permittedServices")
    private List<Service> permittedServices;

    @JsonProperty("address")
    private Address address;
}
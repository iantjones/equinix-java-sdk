package com.eqixiac.equinix.design.value;

import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.design.value.ratecard.ConnectionModel;
import com.eqixiac.equinix.design.value.ratecard.CustomRateCard;
import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.PriceLookup;
import com.eqixiac.equinix.design.value.ratecard.PriceQuote;
import com.eqixiac.equinix.design.value.ratecard.RateCard;
import com.eqixiac.equinix.design.value.ratecard.ReferenceRateCard;
import com.eqixiac.equinix.design.value.ratecard.Term;
import com.eqixiac.equinix.fabric.enums.ConnectionType;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provider interconnect fees from the 2026-11 reference bundle (each figure from the provider's public page
 * or price-list API on 2026-11-12, converted at 730 h/month) and the explainable lookups.
 */
class ProviderInterconnectAndLookupTest {

    private static final ReferenceRateCard CARD = ReferenceRateCard.standard();

    private static BigDecimal fee(CloudProviderType p, ConnectionModel m, int mbps) {
        return CARD.providerConnection(p, m, mbps).value().getMonthlyRecurring();
    }

    @Test
    @DisplayName("exact tiers: hourly x 730 h, or the published monthly figure")
    void exactTiers() {
        assertEquals(new BigDecimal("219.00"), fee(CloudProviderType.AWS, ConnectionModel.DEDICATED_PORT, 1000)); // 0.30 x 730
        assertEquals(new BigDecimal("1642.50"), fee(CloudProviderType.AWS, ConnectionModel.DEDICATED_PORT, 10_000)); // 2.25
        assertEquals(new BigDecimal("240.90"), fee(CloudProviderType.AWS, ConnectionModel.HOSTED, 1000)); // 0.33
        assertEquals(new BigDecimal("43.80"), fee(CloudProviderType.AWS, ConnectionModel.HOSTED, 100)); // 0.06
        assertEquals(0, new BigDecimal("436").compareTo(fee(CloudProviderType.AZURE, ConnectionModel.HOSTED, 1000)));
        assertEquals(0, new BigDecimal("6000").compareTo(fee(CloudProviderType.AZURE, ConnectionModel.DEDICATED_PORT, 10_000)));
        // Google: the old row used 720 h and only 10G; now Partner VLAN attachments by capacity at 730 h.
        assertEquals(new BigDecimal("202.79"), fee(CloudProviderType.GOOGLE_CLOUD, ConnectionModel.HOSTED, 1000)); // 0.2778
        assertEquals(new BigDecimal("1772.44"), fee(CloudProviderType.GOOGLE_CLOUD, ConnectionModel.DEDICATED_PORT, 10_000)); // 2.328+0.10
        assertEquals(new BigDecimal("155.13"), fee(CloudProviderType.ORACLE_CLOUD, ConnectionModel.HOSTED, 1000)); // 0.2125
        PriceQuote q = CARD.providerConnection(CloudProviderType.AWS, ConnectionModel.HOSTED, 1000).value();
        assertTrue(q.getNote().contains("0.33 USD/h x 730 h = 240.90 USD/month; source "
                + "https://aws.amazon.com/directconnect/pricing/, retrieved 2026-11-12"), q.getNote());
        assertFalse(q.getNote().contains("rounded up"));
    }

    @Test
    @DisplayName("between tiers: the next tier up, with a 'rounded up from X to Y' note; never a 10x dedicated port")
    void roundedUp() {
        PriceQuote q = CARD.providerConnection(CloudProviderType.GOOGLE_CLOUD, ConnectionModel.HOSTED, 25_000).value();
        assertEquals(new BigDecimal("6584.60"), q.getMonthlyRecurring(), "50 Gbps attachment 9.02 x 730");
        assertTrue(q.getNote().startsWith("rounded up from 25000 to 50000 Mbps"), q.getNote());
        PriceQuote oracle = CARD.providerConnection(CloudProviderType.ORACLE_CLOUD, ConnectionModel.HOSTED, 500).value();
        assertEquals(new BigDecimal("155.13"), oracle.getMonthlyRecurring(), "Oracle partners sell 1, 10, 100 Gbps");
        assertTrue(oracle.getNote().startsWith("rounded up from 500 to 1000 Mbps"), oracle.getNote());
    }

    @Test
    @DisplayName("above the largest tier, or no row for the model: unpriced with the reason")
    void unpriced() {
        PriceLookup<PriceQuote> above = CARD.providerConnection(CloudProviderType.AWS, ConnectionModel.HOSTED, 50_000);
        assertFalse(above.isPriced());
        assertTrue(above.unpricedReason().contains("publishes HOSTED rates up to 25000 Mbps"), above.unpricedReason());
        PriceLookup<PriceQuote> ibm = CARD.providerConnection(CloudProviderType.IBM_CLOUD, ConnectionModel.HOSTED, 1000);
        assertTrue(ibm.unpricedReason().contains("holds no HOSTED rows for IBM_CLOUD"), ibm.unpricedReason());
        assertEquals(List.of(50, 100, 200, 300, 400, 500, 1000, 2000, 5000, 10_000, 25_000),
                CARD.providerConnectionTiers(CloudProviderType.AWS, ConnectionModel.HOSTED));
    }

    @Test
    @DisplayName("layered lookups say which layer answered and why earlier layers declined")
    void layeredProvenance() {
        CustomRateCard custom = CustomRateCard.builder()
                .connectionRate(ConnectionType.EVPL_VC, 10_000, new BigDecimal("900")).build();
        RateCard layered = RateCard.layered(custom, CARD);
        PriceLookup<PriceQuote> hit = layered.connectionLookup(ConnectionType.EVPL_VC, 10_000, MetroCode.DC, Term.MONTH_1);
        assertEquals("CustomRateCard", hit.answeredBy());
        assertEquals(List.of("layer 1 CustomRateCard: answered"), hit.trail());
        PriceLookup<PriceQuote> fallback = layered.connectionLookup(ConnectionType.EVPL_VC, 1000, MetroCode.DC, Term.MONTH_1);
        assertEquals("ReferenceRateCard", fallback.answeredBy());
        assertTrue(fallback.trail().get(0).startsWith("layer 1 CustomRateCard: declined (no custom connection rate "
                + "declared for EVPL_VC at 1000 Mbps"), fallback.trail().get(0));
        PriceLookup<?> none = layered.egressLookup(CloudProviderType.ORACLE_CLOUD, null, EgressPath.PRIVATE, null);
        assertFalse(none.isPriced());
        assertTrue(none.unpricedReason().startsWith("no layer could price it"), none.unpricedReason());
        assertTrue(none.unpricedReason().contains("no custom PRIVATE egress rate declared for ORACLE_CLOUD"));
    }

    @Test
    @DisplayName("a CustomRateCard holds one currency: changing it after declaring rates is refused")
    void customCurrencyHygiene() {
        CustomRateCard.Builder b = CustomRateCard.builder().currency("EUR")
                .connectionRate(ConnectionType.EVPL_VC, 1000, new BigDecimal("100"));
        assertThrows(IllegalStateException.class, () -> b.currency("USD"));
        assertEquals("EUR", b.currency("EUR").build().currencyCode(), "restating the same currency is fine");
        assertThrows(IllegalArgumentException.class, () -> CustomRateCard.builder().currency((String) null));
    }
}

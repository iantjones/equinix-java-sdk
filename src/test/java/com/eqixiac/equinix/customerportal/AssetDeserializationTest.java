package com.eqixiac.equinix.customerportal;

import com.eqixiac.equinix.core.internal.Constants;
import com.eqixiac.equinix.customerportal.enums.AssetStatus;
import com.eqixiac.equinix.customerportal.model.json.AssetJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class AssetDeserializationTest {

    private static ObjectMapper objectMapper;
    private static AssetJson asset;

    @BeforeAll
    static void setUp() throws Exception {
        objectMapper = Constants.mapper();
        InputStream is = AssetDeserializationTest.class.getResourceAsStream("/json/customerportal/asset_response.json");
        assertNotNull(is, "asset_response.json fixture not found on classpath");
        asset = objectMapper.readValue(is, AssetJson.class);
    }

    @Test
    void assetNumber_isDeserialized() {
        assertEquals("AST-100045", asset.getAssetNumber());
    }

    @Test
    void scalarFields_areDeserialized() {
        assertEquals("SN-998877", asset.getSerialNumber());
        assertEquals("1-204050607", asset.getOrderNumber());
        assertEquals("CROSS_CONNECT", asset.getProductName());
        assertEquals("SV5", asset.getIbx());
        assertEquals("SV5:01:000ABC", asset.getCage());
        assertEquals("Single-mode fiber cross connect", asset.getProductDescription());
        assertEquals("128745", asset.getAccountNumber());
        assertEquals("Acme Corp", asset.getAccountName());
        assertEquals("2025-09-15T10:30:00Z", asset.getInstallationDate());
        assertEquals("CRN-5521", asset.getCustomerReferenceNumber());
    }

    @Test
    void status_isDeserialized() {
        assertEquals(AssetStatus.ACTIVE, asset.getStatus());
    }

    @Test
    void productDetails_isDeserialized() {
        assertNotNull(asset.getProductDetails());
        assertEquals(2, asset.getProductDetails().size());
        assertEquals("Media Type", asset.getProductDetails().get(0).getKey());
        assertEquals("Single Mode Fiber", asset.getProductDetails().get(0).getValue());
        assertEquals("A-SIDE", asset.getProductDetails().get(0).getTag());
    }

    @Test
    void additionalDetails_isDeserialized() {
        assertNotNull(asset.getAdditionalDetails());
        assertEquals("C-14", asset.getAdditionalDetails().getCabinetNumber());
        assertEquals("CIRCUIT-7788", asset.getAdditionalDetails().getCustomerOrCarrierCircuitID());
    }
}

package api.equinix.javasdk.customerportal;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.customerportal.enums.AssetType;
import api.equinix.javasdk.customerportal.model.json.AssetJson;
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
        objectMapper = Constants.objectMapper;
        InputStream is = AssetDeserializationTest.class.getResourceAsStream("/json/customerportal/asset_response.json");
        assertNotNull(is, "asset_response.json fixture not found on classpath");
        asset = objectMapper.readValue(is, AssetJson.class);
    }

    @Test
    void uuid_isDeserialized() {
        assertEquals("b8c9d0e1-f2a3-4b4c-5d6e-7f8091021324", asset.getUuid());
    }

    @Test
    void assetType_isDeserialized() {
        assertEquals(AssetType.CAGE, asset.getAssetType());
    }

    @Test
    void name_isDeserialized() {
        assertEquals("Primary Cage SV5-01", asset.getName());
    }

    @Test
    void ibx_isDeserialized() {
        assertEquals("SV5", asset.getIbx());
    }

    @Test
    void cageId_isDeserialized() {
        assertEquals("SV5:01:000ABC", asset.getCageId());
    }

    @Test
    void cabinetId_isDeserialized() {
        assertEquals("C-14", asset.getCabinetId());
    }

    @Test
    void status_isDeserialized() {
        assertEquals("ACTIVE", asset.getStatus());
    }
}

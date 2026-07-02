package api.equinix.javasdk.fabric;
import api.equinix.javasdk.fabric.enums.IpBlockAssetType;

import api.equinix.javasdk.core.internal.Constants;
import api.equinix.javasdk.fabric.enums.IpBlockOwnership;
import api.equinix.javasdk.fabric.enums.IpBlockProductType;
import api.equinix.javasdk.fabric.enums.IpBlockState;
import api.equinix.javasdk.fabric.model.json.IpBlockJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deserialization tests for {@link IpBlockJson}, focused on the extended response fields
 * (location, order, account, regulations, assets, change, error).
 */
class IpBlockDeserializationTest {

    private static IpBlockJson ipBlock;

    @BeforeAll
    static void setUp() throws Exception {
        ObjectMapper objectMapper = Constants.objectMapper;

        InputStream is = IpBlockDeserializationTest.class.getResourceAsStream("/json/fabric/ip_block_response.json");
        assertNotNull(is, "ip_block_response.json fixture not found on classpath");

        ipBlock = objectMapper.readValue(is, IpBlockJson.class);
    }

    @Test
    void coreFields_areDeserialized() {
        assertEquals("31fbdb3f-8def-410d-868b-ef920878affb", ipBlock.getUuid());
        assertEquals(IpBlockProductType.IPV4_IP_BLOCK, ipBlock.getType());
        assertEquals(IpBlockState.ACTIVE, ipBlock.getState());
        assertEquals(IpBlockOwnership.EQUINIX, ipBlock.getOwnership());
        assertEquals(28, ipBlock.getPrefixLength());
        assertEquals("192.0.2.0/28", ipBlock.getPrefix());
    }

    @Test
    void location_isDeserialized() {
        assertNotNull(ipBlock.getLocation());
        assertEquals("SY", ipBlock.getLocation().getMetroCode());
        assertEquals("https://api.equinix.com/fabric/v4/metros/SY", ipBlock.getLocation().getMetroHref());
    }

    @Test
    void order_isDeserialized() {
        assertNotNull(ipBlock.getOrder());
        assertEquals("1-34834234", ipBlock.getOrder().getOrderNumber());
        assertEquals("PO-9981", ipBlock.getOrder().getPurchaseOrderNumber());
        assertEquals("10", ipBlock.getOrder().getOrderLine());
        assertNotNull(ipBlock.getOrder().getHref());
    }

    @Test
    void account_isDeserialized() {
        assertNotNull(ipBlock.getAccount());
        assertEquals("123456", ipBlock.getAccount().getAccountNumber());
    }

    @Test
    void regulations_isDeserialized() {
        assertNotNull(ipBlock.getRegulations());
        assertNotNull(ipBlock.getRegulations().getAddressingPlans());
        assertEquals(1, ipBlock.getRegulations().getAddressingPlans().size());
        assertEquals("Customer servers", ipBlock.getRegulations().getAddressingPlans().get(0).getPurpose());
        assertEquals(8, ipBlock.getRegulations().getAddressingPlans().get(0).getSize());
        assertNotNull(ipBlock.getRegulations().getQuestions());
        assertTrue(ipBlock.getRegulations().getQuestions().getPrivateSpaceConsidered());
        assertFalse(ipBlock.getRegulations().getQuestions().getRefusedPreviously());
        assertFalse(ipBlock.getRegulations().getQuestions().getReturningAddressSpace());
    }

    @Test
    void assets_isDeserialized() {
        assertNotNull(ipBlock.getAssets());
        assertEquals(1, ipBlock.getAssets().size());
        assertEquals(IpBlockAssetType.FABRIC, ipBlock.getAssets().get(0).getType());
        assertEquals("fd8c5042-b553-4d5e-a2ac-c73bf6d4fd92", ipBlock.getAssets().get(0).getUuid());
        assertNotNull(ipBlock.getAssets().get(0).getHref());
    }

    @Test
    void change_isDeserialized() {
        assertNotNull(ipBlock.getChange());
        assertNotNull(ipBlock.getChange().getHref());
    }

    @Test
    void error_isDeserialized() {
        assertNotNull(ipBlock.getError());
    }

    @Test
    void changeLog_isDeserialized() {
        assertNotNull(ipBlock.getChangeLog());
        assertNotNull(ipBlock.getChangeLog().getCreatedDateTime());
    }
}

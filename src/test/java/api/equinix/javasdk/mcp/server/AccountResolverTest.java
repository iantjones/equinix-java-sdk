/*
 * Copyright 2021 Ian Jones. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package api.equinix.javasdk.mcp.server;

import api.equinix.javasdk.CustomerPortal;
import api.equinix.javasdk.core.http.response.PaginatedList;
import api.equinix.javasdk.customerportal.client.BillingAccounts;
import api.equinix.javasdk.customerportal.model.BillingAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AccountResolver — billingv1 auto-resolution + multi-account elicitation")
class AccountResolverTest {

    private static BillingAccount account(String number, String name) {
        BillingAccount account = mock(BillingAccount.class);
        when(account.getAccountNumber()).thenReturn(number);
        when(account.getAccountName()).thenReturn(name);
        return account;
    }

    private static ServerContext contextWith(BillingAccount... accounts) {
        BillingAccounts billingAccounts = mock(BillingAccounts.class);
        when(billingAccounts.summaries()).thenReturn(
                new PaginatedList<>(List.of(accounts), null, null, null, null));
        CustomerPortal portal = mock(CustomerPortal.class);
        when(portal.billingAccounts()).thenReturn(billingAccounts);
        return ServerContext.builder().customerPortal(portal).environment(Map.of()).build();
    }

    private static ServerContext failingContext() {
        BillingAccounts billingAccounts = mock(BillingAccounts.class);
        when(billingAccounts.summaries()).thenThrow(new RuntimeException("HTTP 403 Forbidden"));
        CustomerPortal portal = mock(CustomerPortal.class);
        when(portal.billingAccounts()).thenReturn(billingAccounts);
        return ServerContext.builder().customerPortal(portal).environment(Map.of()).build();
    }

    @Test
    @DisplayName("a single visible account is auto-resolved without any prompt")
    void singleAccountResolved() {
        ServerContext ctx = contextWith(account("272010", "Acme Corp"));
        AccountResolver.Resolution resolution = AccountResolver.resolve(ctx, StubExchanges.unsupported());
        assertEquals(AccountResolver.Kind.RESOLVED, resolution.kind());
        assertEquals(272010L, resolution.accountNumber());
        assertEquals("272010", resolution.accountNumberText());
    }

    @Test
    @DisplayName("multiple accounts + an elicitation-capable client uses the user's pick")
    void multipleAccountsElicitPick() {
        ServerContext ctx = contextWith(account("111", "Alpha"), account("222", "Bravo"));
        AccountResolver.Resolution resolution = AccountResolver.resolve(ctx, StubExchanges.accepts("222"));
        assertEquals(AccountResolver.Kind.RESOLVED, resolution.kind());
        assertEquals(222L, resolution.accountNumber());
    }

    @Test
    @DisplayName("multiple accounts + a client that cannot elicit is CHOICE_REQUIRED naming every candidate")
    void multipleAccountsUnsupportedChoiceRequired() {
        ServerContext ctx = contextWith(account("111", "Alpha"), account("222", "Bravo"));
        AccountResolver.Resolution resolution = AccountResolver.resolve(ctx, StubExchanges.unsupported());
        assertEquals(AccountResolver.Kind.CHOICE_REQUIRED, resolution.kind());
        List<String> numbers = resolution.candidates().stream().map(AccountResolver.Account::number).toList();
        assertTrue(numbers.contains("111") && numbers.contains("222"),
                "both candidate account numbers are named: " + numbers);
    }

    @Test
    @DisplayName("multiple accounts + a declined prompt is CHOICE_REQUIRED (default is not guessed)")
    void multipleAccountsDeclinedChoiceRequired() {
        ServerContext ctx = contextWith(account("111", "Alpha"), account("222", "Bravo"));
        AccountResolver.Resolution resolution = AccountResolver.resolve(ctx, StubExchanges.declines());
        assertEquals(AccountResolver.Kind.CHOICE_REQUIRED, resolution.kind());
    }

    @Test
    @DisplayName("zero visible accounts is UNRESOLVED with an honest reason and no fabricated number")
    void zeroAccountsUnresolved() {
        ServerContext ctx = contextWith();
        AccountResolver.Resolution resolution = AccountResolver.resolve(ctx, StubExchanges.accepts("x"));
        assertEquals(AccountResolver.Kind.UNRESOLVED, resolution.kind());
        assertNull(resolution.accountNumber());
        assertTrue(resolution.message().toLowerCase().contains("no billing accounts"), resolution.message());
    }

    @Test
    @DisplayName("a failed lookup (403/timeout) is UNRESOLVED with the reason — never a fabricated number")
    void failedLookupUnresolved() {
        ServerContext ctx = failingContext();
        AccountResolver.Resolution resolution = AccountResolver.resolve(ctx, StubExchanges.accepts("x"));
        assertEquals(AccountResolver.Kind.UNRESOLVED, resolution.kind());
        assertNull(resolution.accountNumber());
        assertTrue(resolution.message().contains("403"), resolution.message());
    }

    @Test
    @DisplayName("a single non-numeric account cannot be applied and degrades to UNRESOLVED, not a fabricated value")
    void nonNumericAccountUnresolved() {
        ServerContext ctx = contextWith(account("ABC-XYZ", "Weird Co"));
        AccountResolver.Resolution resolution = AccountResolver.resolve(ctx, StubExchanges.unsupported());
        assertEquals(AccountResolver.Kind.UNRESOLVED, resolution.kind());
        assertNull(resolution.accountNumber());
        assertTrue(resolution.message().contains("ABC-XYZ"), resolution.message());
    }
}

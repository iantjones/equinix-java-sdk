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

import api.equinix.javasdk.customerportal.model.BillingAccount;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the Equinix billing <em>account number</em> a planned deployment should be stamped with,
 * from the authenticated identity — so an agent need not know, or guess, an account number.
 *
 * <p>Reads the accounts the credentials can see through the Customer Portal Billing&nbsp;v1 API
 * ({@code billingAccounts().summaries()}) and decides:</p>
 * <ul>
 *   <li><strong>exactly one</strong> visible account &rarr; {@link Kind#RESOLVED}, that account;</li>
 *   <li><strong>several</strong> &rarr; prompt the user to choose via {@link ElicitationSupport}; on a
 *       pick, {@link Kind#RESOLVED}; when the client cannot elicit, or the user declines, a
 *       {@link Kind#CHOICE_REQUIRED} naming every candidate so the agent can re-call with an explicit
 *       {@code account_number};</li>
 *   <li><strong>none</strong>, or the lookup <strong>fails</strong> (not entitled, throttled, offline)
 *       &rarr; {@link Kind#UNRESOLVED} with an honest reason — <em>never</em> a fabricated number.</li>
 * </ul>
 *
 * <p>This resolver deliberately lives in the MCP server module rather than the design module: it is the
 * only design-planning input sourced from a Customer&nbsp;Portal read, and the design engines stay free
 * of any portal dependency.</p>
 */
final class AccountResolver {

    private static final Logger logger = LoggerFactory.getLogger(AccountResolver.class);

    private AccountResolver() {
    }

    /**
     * Resolves the account number for a plan.
     *
     * @param ctx the server context (Customer Portal access + elicitation timeout)
     * @param exchange the live client exchange, for the multi-account prompt (may be {@code null})
     * @return the resolution
     */
    static Resolution resolve(ServerContext ctx, McpSyncServerExchange exchange) {
        List<Account> accounts;
        try {
            accounts = distinctAccounts(ctx);
        }
        catch (Exception | LinkageError e) {
            String reason = e.getMessage() == null || e.getMessage().isBlank()
                    ? e.getClass().getSimpleName() : e.getMessage();
            logger.warn("Billing-account lookup failed: {}", reason);
            return Resolution.unresolved("the billing-account lookup could not be completed (" + reason
                    + "); no account number was set. Pass account_number explicitly, or grant the "
                    + "credentials access to the Billing v1 accounts API.");
        }

        if (accounts.isEmpty()) {
            return Resolution.unresolved("no billing accounts are visible to the authenticated "
                    + "credentials, so no account number could be resolved. Pass account_number explicitly.");
        }
        if (accounts.size() == 1) {
            return Resolution.resolved(accounts.get(0),
                    "the single billing account visible to the authenticated credentials");
        }

        // More than one account — a genuine decision only the user can make.
        List<ElicitationSupport.Option> options = new ArrayList<>(accounts.size());
        for (Account account : accounts) {
            options.add(new ElicitationSupport.Option(account.number(), account.label(), account.detail()));
        }
        ElicitationSupport.Outcome outcome = ElicitationSupport.chooseOne(ctx, exchange,
                "This identity can bill to " + accounts.size() + " Equinix accounts. Which account should the "
                        + "planned Cloud Routers be billed to?", options);
        if (outcome.picked()) {
            Account chosen = byNumber(accounts, outcome.option().id());
            if (chosen != null) {
                return Resolution.resolved(chosen, "your selection among " + accounts.size()
                        + " visible billing accounts");
            }
        }
        return Resolution.choiceRequired(accounts, "this identity can bill to " + accounts.size()
                + " accounts and " + outcome.detail() + ", so no account number was set. Re-call with an "
                + "explicit account_number chosen from the candidates.");
    }

    private static List<Account> distinctAccounts(ServerContext ctx) {
        Map<String, Account> byNumber = new LinkedHashMap<>();
        for (BillingAccount account : ctx.customerPortal().billingAccounts().summaries().loadAll()) {
            String number = account.getAccountNumber();
            if (number == null || number.isBlank()) {
                continue;
            }
            byNumber.putIfAbsent(number.trim(), new Account(number.trim(), account.getAccountName()));
        }
        return new ArrayList<>(byNumber.values());
    }

    private static Account byNumber(List<Account> accounts, String number) {
        for (Account account : accounts) {
            if (account.number().equals(number)) {
                return account;
            }
        }
        return null;
    }

    /** How the account-number resolution turned out. */
    enum Kind {

        /** A concrete account number was resolved and applied to the plan. */
        RESOLVED,

        /** Several accounts exist and the user did not (or could not) pick — the agent must choose. */
        CHOICE_REQUIRED,

        /** No account number could be resolved — and none was fabricated. */
        UNRESOLVED
    }

    /** One visible billing account. */
    record Account(String number, String name) {

        /** The concise label: the account name when present, else the number. */
        String label() {
            return name == null || name.isBlank() ? number : name;
        }

        /** A disambiguating detail (the number) when a name is shown, else {@code null}. */
        String detail() {
            return name == null || name.isBlank() ? null : number;
        }
    }

    /** The typed outcome of {@link #resolve}. */
    static final class Resolution {

        private final Kind kind;
        private final Long accountNumber;
        private final String accountNumberText;
        private final String source;
        private final List<Account> candidates;
        private final String message;

        private Resolution(Kind kind, Long accountNumber, String accountNumberText, String source,
                           List<Account> candidates, String message) {
            this.kind = kind;
            this.accountNumber = accountNumber;
            this.accountNumberText = accountNumberText;
            this.source = source;
            this.candidates = candidates;
            this.message = message;
        }

        /**
         * Builds a RESOLVED resolution from an account, parsing its number to the {@code long} the wizard
         * requires. A non-numeric account number cannot be applied and degrades to {@link Kind#UNRESOLVED}
         * with an honest reason rather than a fabricated value.
         */
        static Resolution resolved(Account account, String source) {
            try {
                long parsed = Long.parseLong(account.number());
                return new Resolution(Kind.RESOLVED, parsed, account.number(), source, null, null);
            }
            catch (NumberFormatException e) {
                return unresolved("the resolved billing account number '" + account.number()
                        + "' is not numeric and cannot be applied to the plan. Pass account_number explicitly.");
            }
        }

        static Resolution choiceRequired(List<Account> candidates, String message) {
            return new Resolution(Kind.CHOICE_REQUIRED, null, null, null, List.copyOf(candidates), message);
        }

        static Resolution unresolved(String message) {
            return new Resolution(Kind.UNRESOLVED, null, null, null, null, message);
        }

        Kind kind() {
            return kind;
        }

        /** @return the resolved account number as a {@code long} (only when {@link Kind#RESOLVED}) */
        Long accountNumber() {
            return accountNumber;
        }

        /** @return the resolved account number as text (only when {@link Kind#RESOLVED}) */
        String accountNumberText() {
            return accountNumberText;
        }

        /** @return where the resolved number came from (only when {@link Kind#RESOLVED}) */
        String source() {
            return source;
        }

        /** @return the candidate accounts (only when {@link Kind#CHOICE_REQUIRED}) */
        List<Account> candidates() {
            return candidates;
        }

        /** @return a human/LLM-readable explanation (for {@link Kind#CHOICE_REQUIRED} / {@link Kind#UNRESOLVED}) */
        String message() {
            return message;
        }
    }
}

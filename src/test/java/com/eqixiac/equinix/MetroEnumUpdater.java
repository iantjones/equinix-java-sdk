package com.eqixiac.equinix;

import com.eqixiac.equinix.core.auth.BasicEquinixCredentials;
import com.eqixiac.equinix.core.enums.MetroCode;
import com.eqixiac.equinix.fabric.model.Metro;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * One-off runner: fetches the current metro catalogue from the live Fabric API and diffs it against
 * the {@link MetroCode} enum, so the enum can be brought up to date.
 *
 * <p>Credentials are read from the program arguments, or the {@code EQUINIX_CLIENT_ID} /
 * {@code EQUINIX_CLIENT_SECRET} environment variables — nothing is hardcoded, so this file carries no
 * secrets. Run it from your IDE ("Run 'MetroEnumUpdater.main()'") or:</p>
 *
 * <pre>{@code
 * mvn -o test-compile
 * java -cp "target/classes;target/test-classes;<deps>" \
 *      com.eqixiac.equinix.MetroEnumUpdater <clientId> <clientSecret>
 * }</pre>
 *
 * <p>Paste the "NEW" line back and the enum will be updated. This is a developer tool, not a unit
 * test, so the build never runs it.</p>
 */
public final class MetroEnumUpdater {

    public static void main(String[] args) throws Exception {
        String clientId = args.length > 0 ? args[0] : System.getenv("EQUINIX_CLIENT_ID");
        String clientSecret = args.length > 1 ? args[1] : System.getenv("EQUINIX_CLIENT_SECRET");
        if (clientId == null || clientSecret == null) {
            System.err.println("Usage: MetroEnumUpdater <clientId> <clientSecret>");
            System.err.println("   or set EQUINIX_CLIENT_ID / EQUINIX_CLIENT_SECRET");
            System.exit(1);
            return;
        }

        // code -> "name / region", sorted by code, for human-readable context.
        Map<String, String> live = new TreeMap<>();
        try (Fabric fabric = new Fabric(new BasicEquinixCredentials(clientId, clientSecret))) {
            for (Metro m : fabric.metros().list().loadAll()) {   // loadAll() pages the whole catalogue
                String code = m.metroId() != null ? m.metroId().code() : "(null)";
                live.put(code, m.getName() + " / " + m.getRegion());
            }
        }

        Set<String> liveCodes = new TreeSet<>(live.keySet());
        Set<String> enumCodes = Arrays.stream(MetroCode.values())
                .map(Enum::name)
                .filter(name -> !name.equals("UNKNOWN"))
                .collect(Collectors.toCollection(TreeSet::new));

        Set<String> newCodes = new TreeSet<>(liveCodes);
        newCodes.removeAll(enumCodes);          // in the API, missing from the enum

        Set<String> staleCodes = new TreeSet<>(enumCodes);
        staleCodes.removeAll(liveCodes);        // in the enum, no longer returned by the API

        System.out.println("=== Live metros (" + liveCodes.size() + ") ===");
        live.forEach((code, info) -> System.out.printf("  %-5s %s%n", code, info));

        System.out.println();
        System.out.println("=== ALL CODES (" + liveCodes.size() + ") ===");
        System.out.println(String.join(" ", liveCodes));

        System.out.println();
        System.out.println("=== NEW — add these to MetroCode (" + newCodes.size() + ") ===");
        System.out.println(String.join(" ", newCodes));

        System.out.println();
        System.out.println("=== STALE — in enum but not returned (" + staleCodes.size() + ") ===");
        System.out.println(String.join(" ", staleCodes));
    }
}

package com.eqixiac.equinix.design.value.savings;

import com.eqixiac.equinix.design.value.ratecard.EgressPath;
import com.eqixiac.equinix.design.value.ratecard.MulticloudLinkQuote;
import com.eqixiac.equinix.fabric.model.implementation.cloud.CloudProviderType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * The cloud-to-cloud section of a {@link SavingsEstimate}: the monthly cost of moving traffic
 * between two clouds over each of three paths, and the sustained rate at which the flat-fee
 * native path costs the same as the per-GB Equinix path. Present only when the calculator was
 * given a peer cloud ({@code SavingsCalculator.Builder.toCloud(...)}).
 *
 * <p><b>Beta</b>: the native path models products that reached general availability in 2026;
 * see {@code RateCard.multicloudLink}.</p>
 *
 * <table>
 *   <caption>The three paths ({@link EgressPath}) and what each total contains</caption>
 *   <tr><th>Path</th><th>Fixed monthly</th><th>Data transfer monthly</th></tr>
 *   <tr><td>{@code INTERNET}</td><td>none</td><td>internet egress from each cloud on its own
 *       direction's volume</td></tr>
 *   <tr><td>{@code PRIVATE}</td><td>two Fabric virtual connections, the optional Cloud Router,
 *       and the CSP interconnect-port reference figure for both clouds</td><td>private egress
 *       from each cloud on its own direction's volume</td></tr>
 *   <tr><td>{@code MULTICLOUD_INTERCONNECT}</td><td>the two providers' flat link fees</td>
 *       <td>the link's per-GB rate (zero where the provider publishes zero)</td></tr>
 * </table>
 *
 * <p>A path total is {@code null} when any of its inputs is unpriced or its inputs span more
 * than one currency; {@code getNotes()} names the missing input. A {@code null} total is not a
 * zero cost. All non-null amounts are in {@code getCurrency()}.</p>
 *
 * <h2>Break-even</h2>
 * <p>{@code getBreakEvenSustainedMbps()} is computed by
 * {@link #breakEvenSustainedMbps(BigDecimal, BigDecimal, BigDecimal, BigDecimal)} from the quotes
 * actually resolved for this estimate. It is a property of the prices, not of the declared
 * volumes. It is {@code null} when any input is unpriced, the currencies differ, or no
 * break-even exists.</p>
 */
@Value
@Builder
public class MulticloudPathComparison {

    /** Seconds in the {@value MulticloudLinkQuote#HOURS_PER_MONTH}-hour month used throughout. */
    public static final long SECONDS_PER_MONTH = MulticloudLinkQuote.HOURS_PER_MONTH * 3600L;

    /** Megabits per decimal gigabyte: 8 bits per byte x 1000 MB per GB. */
    public static final int MEGABITS_PER_GB = 8_000;

    /** The cloud the forward volume leaves. */
    CloudProviderType providerA;

    /** {@code providerA}'s region, or {@code null}. */
    String regionA;

    /** The peer cloud, which the reverse volume leaves. */
    CloudProviderType providerZ;

    /** {@code providerZ}'s region, or {@code null}. */
    String regionZ;

    /** The link size in Mbps used for every path. */
    int bandwidthMbps;

    /** The AWS connectivity-scope tier (1-5) used for the native path. */
    int pathTier;

    /** Monthly volume from {@code providerA} to {@code providerZ}, in decimal GB. */
    BigDecimal forwardEgressGb;

    /** Monthly volume from {@code providerZ} to {@code providerA}, in decimal GB. */
    BigDecimal reverseEgressGb;

    /** Whether the reverse volume was assumed equal to the forward volume (not supplied). */
    boolean reverseEgressAssumedSymmetric;

    /** Both clouds' internet egress on the declared volumes; {@code null} when unpriced. */
    BigDecimal internetMonthlyCost;

    /**
     * Fixed monthly cost of the Equinix path: two virtual connections, the optional Cloud Router
     * and both CSP interconnect ports. {@code null} when any component is unpriced.
     */
    BigDecimal equinixFixedMonthlyCost;

    /** Both clouds' private egress on the declared volumes; {@code null} when unpriced. */
    BigDecimal equinixEgressMonthlyCost;

    /** {@code equinixFixedMonthlyCost + equinixEgressMonthlyCost}; {@code null} when either is. */
    BigDecimal equinixMonthlyCost;

    /** One-time Equinix charges (two connections, router); {@code null} when unpriced. */
    BigDecimal equinixSetupCost;

    /** The two providers' flat link fees; {@code null} when a side is unpriced. */
    BigDecimal nativeFixedMonthlyCost;

    /** The native link's per-GB charge on the declared volumes; {@code null} when unknown. */
    BigDecimal nativeDataTransferMonthlyCost;

    /** {@code nativeFixedMonthlyCost + nativeDataTransferMonthlyCost}; {@code null} when either is. */
    BigDecimal nativeMonthlyCost;

    /** The two-sided native link quote as resolved, for per-side provenance; may be {@code null}. */
    MulticloudLinkQuote nativeLinkQuote;

    /**
     * The sustained rate, summed over both directions and split evenly between them, at which
     * the native path and the Equinix path cost the same per month, in Mbps. Which path is
     * cheaper on each side of it is given by {@code isNativeCheaperAboveBreakEven()}: in the
     * usual case (native fixed fee above the Equinix fixed cost, Equinix per-GB rate above the
     * native per-GB rate) the Equinix path is cheaper below it and the native path above it.
     * {@code null} when it cannot be computed. It can exceed the link's capacity
     * ({@code 2 x bandwidthMbps}), in which case the path that is cheaper below it is cheaper at
     * every achievable rate and {@code getNotes()} says so.
     */
    BigDecimal breakEvenSustainedMbps;

    /**
     * The sense of {@code getBreakEvenSustainedMbps()}: {@code true} when the native path costs
     * less above the break-even rate and the Equinix path below it (native fixed fee above the
     * Equinix fixed cost, Equinix per-GB rate above the native per-GB rate); {@code false} when
     * the two paths cross the other way (native fixed fee below the Equinix fixed cost and native
     * per-GB rate above the Equinix per-GB rate), so the Equinix path costs less above the
     * break-even rate. Meaningful only when {@code getBreakEvenSustainedMbps()} is not
     * {@code null}; {@code false} otherwise.
     */
    boolean nativeCheaperAboveBreakEven;

    /**
     * The path with the lowest monthly total at the declared volumes, among the paths that
     * priced. {@code null} when fewer than two paths priced.
     */
    EgressPath lowestCostPath;

    /** The ISO 4217 code every non-null amount is expressed in. */
    String currency;

    /** Provenance, assumptions and the reason for each {@code null} figure, in reading order. */
    List<String> notes;

    /**
     * {@code getBreakEvenSustainedMbps()} as an {@link Optional}.
     *
     * @return the break-even rate in Mbps, or empty when it could not be computed
     */
    public Optional<BigDecimal> breakEvenSustainedMbps() {
        return Optional.ofNullable(breakEvenSustainedMbps);
    }

    /**
     * The break-even sustained rate between a flat-fee native link and a per-GB path.
     *
     * <p>Model. Traffic is symmetric: {@code g} GB per month in each direction. Each cloud bills
     * egress on the traffic leaving it.</p>
     * <pre>
     * perGbPath(g) = perGbPathFixedMonthly + g x perGbSumPerGbPath
     * native(g)    = nativeFixedMonthly    + g x perGbSumNative
     *
     * g*           = (nativeFixedMonthly - perGbPathFixedMonthly) / (perGbSumPerGbPath - perGbSumNative)
     * mbpsEachWay  = g* x 8000 Mb/GB / (730 h x 3600 s/h)
     * result       = 2 x mbpsEachWay
     * </pre>
     * <p>{@code perGbSum*} is the sum of the two clouds' per-GB rates on that path (A's rate for
     * the A-to-Z direction plus Z's rate for the Z-to-A direction). The result is the total of
     * both directions; divide by two for the rate in each direction. One month is
     * {@value MulticloudLinkQuote#HOURS_PER_MONTH} hours and one GB is 10<sup>9</sup> bytes.</p>
     *
     * <p>Worked example from cited list prices (retrieved 2026-09-21): native 10 Gbps =
     * (12.33 + 19.00) USD/h x 730 h = 22,870.90 USD; per-GB path fixed = AWS Direct Connect
     * 10G port 2.25 USD/h x 730 h + Google Cross-Cloud Interconnect 10G 5.60 USD/h x 730 h =
     * 5,730.50 USD; per-GB 0.02 + 0.02 USD; native per-GB 0. Then g* = 17,140.40 / 0.04 =
     * 428,510 GB each way = 1,304.4 Mbps each way = 2,608.9 Mbps in total.</p>
     *
     * <p>The two cost lines cross when the fixed-cost difference and the per-GB difference have
     * the same sign. Both positive is the usual case: the native path is cheaper above
     * {@code g*}. Both negative (native fixed fee below the per-GB path's fixed cost, native
     * per-GB rate above the per-GB path's rate) is a crossing with the opposite sense: the
     * per-GB path is cheaper above {@code g*}. {@link #dominatesAtEveryVolume} states which case
     * applies, and the {@code nativeCheaperAboveBreakEven} field of an instance records the sense
     * for the break-even it carries. When the signs differ (or one delta is zero) one path costs
     * the same or less at every volume and there is no break-even.</p>
     *
     * <p>All four arguments must be in one currency; the caller is responsible for that.</p>
     *
     * @param nativeFixedMonthly    the native link's flat monthly fee, both sides
     * @param perGbPathFixedMonthly the per-GB path's fixed monthly cost
     * @param perGbSumPerGbPath     the two clouds' per-GB rates on the per-GB path, summed
     * @param perGbSumNative        the two clouds' per-GB rates on the native link, summed
     * @return the break-even rate in Mbps (both directions summed), scale 1; empty when an
     *         argument is {@code null} or when the two cost lines do not cross at a positive
     *         volume, that is when the native fixed fee does not exceed the per-GB path's fixed
     *         cost while the per-GB path's rate is at or above the native rate (the native path
     *         is then cheaper or equal at every volume), or when the native fixed fee is at or
     *         above the per-GB path's fixed cost while the per-GB path's rate does not exceed the
     *         native rate (the per-GB path is then cheaper or equal at every volume)
     */
    public static Optional<BigDecimal> breakEvenSustainedMbps(BigDecimal nativeFixedMonthly,
                                                              BigDecimal perGbPathFixedMonthly,
                                                              BigDecimal perGbSumPerGbPath,
                                                              BigDecimal perGbSumNative) {
        if (nativeFixedMonthly == null || perGbPathFixedMonthly == null
                || perGbSumPerGbPath == null || perGbSumNative == null) {
            return Optional.empty();
        }
        BigDecimal fixedDelta = nativeFixedMonthly.subtract(perGbPathFixedMonthly);
        BigDecimal perGbDelta = perGbSumPerGbPath.subtract(perGbSumNative);
        if (fixedDelta.signum() == 0 || perGbDelta.signum() == 0 || fixedDelta.signum() != perGbDelta.signum()) {
            return Optional.empty();
        }
        // Same sign: the quotient is positive in both cases.
        BigDecimal gbEachWay = fixedDelta.divide(perGbDelta, MathContext.DECIMAL64);
        BigDecimal mbpsEachWay = gbEachWay.multiply(BigDecimal.valueOf(MEGABITS_PER_GB))
                .divide(BigDecimal.valueOf(SECONDS_PER_MONTH), MathContext.DECIMAL64);
        return Optional.of(mbpsEachWay.multiply(BigDecimal.valueOf(2)).setScale(1, RoundingMode.HALF_UP));
    }

    /**
     * Whether the native path costs less than the per-GB path above the break-even rate that
     * {@link #breakEvenSustainedMbps(BigDecimal, BigDecimal, BigDecimal, BigDecimal)} returns for
     * the same arguments: {@code true} when the native fixed fee exceeds the per-GB path's fixed
     * cost (the per-GB path then wins at low volume and loses at high volume), {@code false}
     * when it is below it (the reverse). Meaningful only when that method returns a value.
     *
     * @param nativeFixedMonthly    the native link's flat monthly fee, both sides
     * @param perGbPathFixedMonthly the per-GB path's fixed monthly cost
     * @return {@code true} when {@code nativeFixedMonthly > perGbPathFixedMonthly}
     * @throws NullPointerException if either argument is {@code null}
     */
    public static boolean nativeCheaperAboveBreakEven(BigDecimal nativeFixedMonthly, BigDecimal perGbPathFixedMonthly) {
        return nativeFixedMonthly.compareTo(perGbPathFixedMonthly) > 0;
    }

    /**
     * Which path costs the same or less at every volume when the cost lines do not cross.
     * Defined only when {@link #breakEvenSustainedMbps(BigDecimal, BigDecimal, BigDecimal, BigDecimal)}
     * returns empty for the same non-null arguments.
     *
     * @param nativeFixedMonthly    the native link's flat monthly fee, both sides
     * @param perGbPathFixedMonthly the per-GB path's fixed monthly cost
     * @param perGbSumPerGbPath     the two clouds' per-GB rates on the per-GB path, summed
     * @param perGbSumNative        the two clouds' per-GB rates on the native link, summed
     * @return {@link EgressPath#MULTICLOUD_INTERCONNECT} when the native path costs the same or
     *         less at every volume, {@link EgressPath#PRIVATE} when the per-GB path does; empty
     *         when the lines cross (a break-even exists) or an argument is {@code null}
     */
    public static Optional<EgressPath> dominatesAtEveryVolume(BigDecimal nativeFixedMonthly,
                                                              BigDecimal perGbPathFixedMonthly,
                                                              BigDecimal perGbSumPerGbPath,
                                                              BigDecimal perGbSumNative) {
        if (nativeFixedMonthly == null || perGbPathFixedMonthly == null
                || perGbSumPerGbPath == null || perGbSumNative == null) {
            return Optional.empty();
        }
        int fixedSign = nativeFixedMonthly.subtract(perGbPathFixedMonthly).signum();
        int perGbSign = perGbSumPerGbPath.subtract(perGbSumNative).signum();
        if (fixedSign <= 0 && perGbSign >= 0) {
            return Optional.of(EgressPath.MULTICLOUD_INTERCONNECT);
        }
        if (fixedSign >= 0 && perGbSign <= 0) {
            return Optional.of(EgressPath.PRIVATE);
        }
        return Optional.empty();
    }
}

package com.eqixiac.equinix.design.value.ratecard;

/**
 * How a cloud provider sells the connection a design lands on, which decides the provider-side fee.
 *
 * <p>A connection built over Equinix Fabric to a cloud is always the provider's partner (hosted)
 * product: Fabric is the partner. The providers price that product differently from a port the
 * customer owns outright, so the reference data keeps the two apart:</p>
 * <table>
 *   <caption>The product each model means, per provider</caption>
 *   <tr><th>Provider</th><th>{@link #DEDICATED_PORT}</th><th>{@link #HOSTED}</th></tr>
 *   <tr><td>AWS</td><td>Direct Connect dedicated connection (port-hour)</td>
 *       <td>Direct Connect hosted connection (port-hour by capacity)</td></tr>
 *   <tr><td>Microsoft Azure</td><td>ExpressRoute Direct port (monthly)</td>
 *       <td>ExpressRoute circuit, service-provider model, Standard Metered (monthly); the Fabric side
 *       of the circuit is a partner circuit</td></tr>
 *   <tr><td>Google Cloud</td><td>Dedicated Interconnect circuit plus one VLAN attachment (hourly)</td>
 *       <td>Partner Interconnect VLAN attachment by capacity (hourly)</td></tr>
 *   <tr><td>Oracle Cloud</td><td>FastConnect port (port-hour)</td>
 *       <td>FastConnect through an Oracle partner, charged the same port-hour by port speed</td></tr>
 * </table>
 */
public enum ConnectionModel {

    /** A physical port or circuit the customer orders from the provider (cross-connected at a colocation). */
    DEDICATED_PORT,

    /** A partner-provisioned (hosted) connection of a set capacity: what a Fabric cloud connection is. */
    HOSTED
}

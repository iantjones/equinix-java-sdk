## Egress Savings Estimate

**Workload:** 60000 GB/mo egress from AWS (us-east-1) via metro DC

| Line item | Monthly |
|---|---|
| Egress over public internet | USD 5400.00 |
| Egress over private interconnect | USD 1200.00 |
| **Egress saving** | **USD 4200.00** |
| Equinix interconnect (recurring) | -USD 1650.00 |
| AWS interconnect port (reference) | -USD 1204.50 |
| **Net monthly saving** | **USD 1345.50** |

- One-time Equinix setup: USD 0.00
- Annual net saving (steady state): USD 16146.00
- First-year net saving (incl. setup): USD 16146.00
- Break-even egress volume: 40779 GB/mo

_Derived from spec fingerprint `a5f92102787e`_
- Assumed: 4000 Mbps of workloads to 'aws' rounded up to the 5000 Mbps Fabric tier
- Assumed: The portfolio DC+FR+SG has several metros; the connection is priced in DC

_Design-time estimate, not a quote. Equinix interconnect costs use live Fabric pricing where available; egress rates are indicative reference or caller-supplied figures. Actual costs depend on region, tiering, volume, and contract terms. Excludes per-provider free-tier egress allowances and compute/storage costs. Net savings include the AWS interconnect port fee at the bundled reference rate._


## Total Cost of Ownership: Deployment Comparison

**Term:** 12 months (archetypes are ranked by total cost over the term, including one-time setup)

| Approach | Monthly | One-time | Total over term | |
|---|---|---|---|---|
| Public cloud over internet | USD 5400.00 | USD 0.00 | USD 64800.00 |  |
| On-premises / self-managed | USD 5275.00 | USD 0.00 | USD 63300.00 |  |
| Equinix interconnected | USD 4054.50 | USD 0.00 | USD 48654.00 | recommended |


_Derived from spec fingerprint `a5f92102787e`_
- Assumed: 4000 Mbps of workloads to 'aws' rounded up to the 5000 Mbps Fabric tier
- Assumed: The portfolio DC+FR+SG has several metros; the connection is priced in DC

**Recommended:** Equinix interconnected

- Monthly saving vs. Public cloud over internet: USD 1345.50
- Annual saving vs. Public cloud over internet: USD 16146.00
- Saving over the 12-month term vs. Public cloud over internet: USD 16146.00

_Design-time TCO estimate, not a quote. Equinix Fabric connection costs use live pricing where available; cloud-egress, cloud-provider interconnect-port, cross-connect, and on-prem figures are indicative reference midpoints (the on-prem inputs are coarse and overridable). Compute, storage, software, staffing, and per-provider free-tier egress allowances are out of scope. Actual costs depend on region, volume, tiering, and contract terms. (reference data as of 2026-11)_

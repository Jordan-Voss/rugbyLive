# ADR 0014: AWS Deployment

## Status
Accepted

## Decision

Deploy on AWS free tier.

## Reason

- cost-effective
- scalable later

## Implementation Notes

- define AWS resources in infra/terraform
- keep environments minimal for free-tier compatibility

## References

- docs/deployment.md
- infra/terraform

## Consequences

- initial infra setup required

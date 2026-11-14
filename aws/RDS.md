# Amazon RDS PostgreSQL setup

For the first AWS learning deployment, create a small PostgreSQL RDS instance
in the same AWS region/VPC used by EKS.

Recommended learning settings:

- Engine: PostgreSQL
- Database: `walletdb`
- Username: `wallet`
- Initial database name: `walletdb`
- Storage: keep the smallest practical configuration
- Public access: avoid it when possible
- Security group: allow PostgreSQL port 5432 only from the EKS worker/node
  security group

After RDS is available, get the endpoint from the RDS console.

The application JDBC URL is:

```text
jdbc:postgresql://<rds-endpoint>:5432/walletdb
```

Flyway will create/validate the wallet schema when the application starts.

## Important networking point

EKS pods must be able to route to the RDS subnet/security group. Do not solve
connectivity by opening PostgreSQL to `0.0.0.0/0`.

For a more production-like version, keep RDS private and use the EKS VPC
security groups to permit only the application/node traffic.

## Cleanup

When finished with the learning environment, delete:

1. Kubernetes service/load balancer.
2. EKS cluster/node group.
3. RDS instance.
4. DynamoDB table if no longer needed.
5. ECR images/repository if no longer needed.

AWS billing can continue for resources that remain provisioned.

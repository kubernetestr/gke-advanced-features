# Using Cloud SQL in GKE Containers

This is a step by step guide about how to connect to Cloud SQL instances from GKE containers.

### Prerequisities

- Make sure the Cloud SQL Admin API is enabled.
- Create a second generation MySQL instance in Cloud SQL.
- Create a DB user.
- Create a service account, assign Cloud SQL Client role, create a key and download the json file.


### Sample App Deployment

For demo purposes, a wordpress app which connects to MySQL is deployed.

```
kubectl create ns sql
kubectl create secret generic cloudsql-db-credentials --from-literal username=test-user --from-literal password=123456 --namespace sql
kubectl create secret generic cloudsql-instance-credentials --from-file=credentials.json=./ace-demo-project-5f87b55bc21f.json --namespace sql
kubectl apply -f mysql_wordpress_deployment.yaml
kubectl expose deployment wordpress --type=LoadBalancer --namespace sql
kubectl get svc -n sql
```

Some notes:
- Cloudsql-proxy is a sidecar container and it is defined in mysql_wordpress_deployment.yaml.
- The secret cloudsql-instance-credentials contains service account credentials.
- The secret cloudsql-db-credentials contains credential information to connect to the database.
- Please change the instance name in mysql_wordpress_deployment.yaml. In this case, it is ace-demo-project:us-central1:ace.

Please try to access the external service IP and set up the blog. If it is successful, then it means containers are able to connect to Cloud SQL using Cloud SQL Proxy. In addition, you can see the "wordpress" database is created if you take a look at the databases section in Cloud SQL instance from the console.

### References

https://cloud.google.com/sql/docs/mysql/connect-kubernetes-engine

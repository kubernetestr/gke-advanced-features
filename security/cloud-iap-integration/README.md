# gke ingress with cloud identity aware proxy

This link has all the steps:

https://cloud.google.com/iap/docs/enabling-kubernetes-howto

Some important points when following the steps are:

### Configuring the OAuth consent screen:

- In consent screen, application type "internal" should be selected to only allow users in organization.
- At least one authorized domain should be defined. "aozturk.xyz" is used for this example.
- Before configuring identity-aware-proxy, a https load balancer must be configured. For GKE it corresponds to an ingress controller.

### Deploying App, Creating Service and HTTPS Ingress

```
kubectl create ns web
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /tmp/tls.key -out /tmp/tls.crt -subj "/CN=nginx.aozturk.xyz"
kubectl create secret tls foo-secret --key /tmp/tls.key --cert /tmp/tls.crt --namespace web
kubectl apply -f nginx-deployment.yml
kubectl apply -f nginx-service.yml
kubectl apply -f ingress.yml
kubectl get ingress -n web
```

Using Cloud DNS or another DNS service, an A record should be created for the application deployed. It should match with Ingress IP.

https://nginx.aozturk.xyz/ is reachable for anyone at this point. The thing is, it should be only allowed to selected identities in an organization.

### Setting up Cloud IAP access:

- Notice that owner role does not grant access to the application. IAP-secured Web App User role is required.

### Creating OAuth credentials:

- For this example https://nginx.aozturk.xyz/_gcp_gatekeeper/authenticate is an authorized redirect URI.
- Please note the client id and secret. 

### Configuring BackendConfig:

- Create the secret, backendconfig resource and change the metadata of nginx service according to the documentation.

Please wait a few minutes and try to access the application again. This time, only the IAP-secured Web App User can access.

### References

https://cloud.google.com/iap/docs/enabling-kubernetes-howto

# gke-ingress-with-cloud-armor
Allowing/denying traffic from specified IP ranges to the Kubernetes services in GKE.

This repo contains manifests and steps to enable IP whitelists for Google HTTP(S) Load Balancers configured by GKE Ingress Controller.

For demonstration and testing purposes, two apps will be deployed and one app will be accessible to all users (IPs) and the other will be accessible to only a subset of IPs. 



### Step 1: Creating Cloud Armor Security Policies/Rules and Corresponding BackendConfig

```
gcloud beta compute security-policies create allow-cidr-policy \
    --description "policy for only allowing some cidrs"

gcloud beta compute security-policies describe allow-cidr-policy
```

When the newly created security policy is described, it can be seen that there is a default rule which has an "allow" action. Only a subset of IPs will be allowed and the rest should be denied, thus the default rule action has to be "deny". It can be updated as follows:

```
gcloud beta compute security-policies rules update 2147483647 \
   --security-policy allow-cidr-policy \
   --action "deny-403"

gcloud beta compute security-policies rules create 1000 \
   --security-policy allow-cidr-policy \
   --src-ip-ranges "212.0.0.0/24" \
   --action "allow"
```

The corresponding backend config in Kubernetes can be created by this command:

```
kubectl create ns web
kubectl apply -f web-backend-config.yml
```

### Step 2: Deploying Apps and Creating Services

```
kubectl apply -f nginx-deployment.yml
kubectl apply -f nginx-service.yml
kubectl apply -f hello-deployment.yml
kubectl apply -f hello-service.yml
```

Please note that hello service has an annotation which include a backend config and nginx service has not.

### Step 3: Creating Ingress Resource

A host based ingress resource with an external static IP is created. It has two backends, one is nginx app and the other is hello app.

```
gcloud compute addresses create web-test --global
gcloud compute addresses list --filter web-test
kubectl apply -f web-ingress.yml
```

### Step 4: Test

First, the DNS entries for hosts in ingress definiton should be created, either using global DNS services or just editing etc/hosts file for test purposes. 

When trying to reach nginx app via Ingress IP, it can be seen that it is accessible to any IPs. But when trying to reach hello app via Ingress IP, it can be seen that it is only accessible to IPs allowed in security policy.

### References

https://cloud.google.com/kubernetes-engine/docs/how-to/cloud-armor-backendconfig

https://cloud.google.com/armor/docs/configure-security-policies

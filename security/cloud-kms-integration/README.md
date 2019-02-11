# GKE integration with Cloud KMS

This is a step by step guide about how to create a GKE cluster integrated with Cloud KMS.

### Prerequisities

- Make sure Cloud Key Management Service (KMS) API is enabled.
- Make sure Kubernetes Engine API is enabled.
- Run "gcloud components update".

### Creating a Cloud KMS key and binding

Create a key ring:

```
gcloud kms keyrings create gke-ring \
    --location us-central1 \
    --project [PROJECT_ID]
```

Create a key:

```
gcloud kms keys create gke-key \
    --location us-central1 \
    --keyring gke-ring \
    --purpose encryption \
    --project [PROJECT_ID]
```

Grant your GKE service account the Cloud KMS CryptoKey Encrypter/Decrypter role:

```
gcloud kms keys add-iam-policy-binding gke-key \
  --location us-central1  \
  --keyring gke-ring \
  --member serviceAccount:service-[CLUSTER_PROJECT_NUMBER]@container-engine-robot.iam.gserviceaccount.com \
  --role roles/cloudkms.cryptoKeyEncrypterDecrypter \
  --project [PROJECT_ID]
```

### Creating a cluster with Application-layer Secrets Encryption

Create a cluster:

```
gcloud beta container clusters create gke-test-cluster \
  --cluster-version=latest \
  --zone us-central1-a \
  --database-encryption-key projects/[PROJECT_ID]/locations/us-central1/keyRings/gke-ring/cryptoKeys/gke-key \
  --project [PROJECT_ID]
```

Describe the cluster:

```
gcloud beta container clusters describe gke-test-cluster \
  --zone us-central1-a \
  --format 'value(databaseEncryption)' \
  --project [PROJECT_ID]
```

The response should be like:

keyName=projects/[PROJECT-ID]/locations/us-central1/keyRings/gke-ring/cryptoKeys/gke-key;state=ENCRYPTED

### What happens when you create a Secret 

Because the cluster is using Application-layer Secrets Encryption, here's what happens when you create a new Secret:

- The Kubernetes API server generates a unique DEK for the Secret by using a random number generator.

- The Kubernetes API server uses the DEK locally to encrypt the Secret.

- The KMS plugin sends the DEK to Cloud KMS for encryption. The KMS plugin uses your project's GKE service account to authenticate to Cloud KMS.

- Cloud KMS encrypts the DEK, and sends it back to the KMS plugin.

- The Kubernetes API server saves the encrypted Secret and the encrypted DEK. The plaintext DEK is not saved to disk.

- When a client requests a Secret from the Kubernetes API server, the process described above is reversed.

### References

https://cloud.google.com/kubernetes-engine/docs/how-to/encrypting-secrets


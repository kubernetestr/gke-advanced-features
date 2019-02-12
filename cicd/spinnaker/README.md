# Using Spinnaker to Deploy Applications to GKE 

This is a step by step guide about how to deploy applications to Kubernetes environments using Spinnaker as CD tool.

### Prerequisities

- Make sure GKE cluster is created.


### Spinnaker Installation Steps

Install Halyard on Ubuntu:

```
curl -O https://raw.githubusercontent.com/spinnaker/halyard/master/install/debian/InstallHalyard.sh
sudo bash InstallHalyard.sh
hal -v
```

Configuring Provider:

```
CONTEXT=$(kubectl config current-context)

kubectl apply --context $CONTEXT \
    -f https://spinnaker.io/downloads/kubernetes/service-account.yml

TOKEN=$(kubectl get secret --context $CONTEXT \
   $(kubectl get serviceaccount spinnaker-service-account \
       --context $CONTEXT \
       -n spinnaker \
       -o jsonpath='{.secrets[0].name}') \
   -n spinnaker \
   -o jsonpath='{.data.token}' | base64 --decode)

cp ~/.kube/config ~/.kube/config-backup

kubectl config set-credentials ${CONTEXT}-token-user --token $TOKEN

kubectl config set-context $CONTEXT --user ${CONTEXT}-token-user

mv ~/.kube/config ~/.kube/config-spinnaker

mv ~/.kube/config-backup ~/.kube/config

hal config provider kubernetes enable

CONTEXT=$(kubectl config --kubeconfig=/home/ubuntu/.kube/config-spinnaker current-context)

hal config provider kubernetes account add spinnaker-account \
    --provider-version v2 \
	--kubeconfig-file "/home/ubuntu/.kube/config-spinnaker" \
    --context $CONTEXT 
	
hal config features edit --artifacts true
```

Environment Setup:

```
hal config deploy edit --type distributed --account-name spinnaker-account
```

Google Storage Setup:

```
SERVICE_ACCOUNT_NAME=spinnaker-gcs-account
SERVICE_ACCOUNT_DEST=~/.gcp/gcs-account.json

gcloud iam service-accounts create \
    $SERVICE_ACCOUNT_NAME \
    --display-name $SERVICE_ACCOUNT_NAME
	
SA_EMAIL=$(gcloud iam service-accounts list \
    --filter="displayName:$SERVICE_ACCOUNT_NAME" \
    --format='value(email)')

PROJECT=$(gcloud info --format='value(config.project)')

gcloud projects add-iam-policy-binding $PROJECT \
    --role roles/storage.admin --member serviceAccount:$SA_EMAIL
	
mkdir -p $(dirname $SERVICE_ACCOUNT_DEST)

gcloud iam service-accounts keys create $SERVICE_ACCOUNT_DEST \
    --iam-account $SA_EMAIL

BUCKET_LOCATION=us
BUCKET=ace-spin
gsutil mb  -l $BUCKET_LOCATION gs://$BUCKET/


hal config storage gcs edit --project $PROJECT \
    --bucket-location $BUCKET_LOCATION \
    --json-path $SERVICE_ACCOUNT_DEST \
	--bucket $BUCKET

hal config storage edit --type gcs
```

If you get 403 error from Google Storage, make sure spinnaker-gcs-account is explicitly has Storage Admin role by visiting Google Cloud Storage Bucket Details/Permissions page.



Deploy Spinnaker:

```
hal version list
hal config version edit --version 1.12.1
hal deploy apply
```

Connect to Spinnaker:

Edit the spin-deck service, spin-gate and change their type from ClusterIP to LoadBalancer and note the LoadBalancer IPs. Then run:

```
hal config security ui edit --override-base-url http://spin-deck-LB-IP
hal config security api edit --override-base-url http://spin-gate-LB-IP
hal deploy apply
```

Providing Docker Registry:

```
ADDRESS=index.docker.io
REPOSITORIES=aozturk12/hello
hal config provider docker-registry enable

hal config provider docker-registry account add test-docker-registry \
    --address $ADDRESS \
    --repositories $REPOSITORIES
hal deploy apply	
```

These steps are slightly different in other registries. 

Just to note, we could configure all the things above and could run "hal deploy apply" once.

### Creating a Pipeline

The steps:

- Open Spinnaker web UI and click applications tab.
- Create a new application.
- Click pipelines tab and configure a new pipeline.
- Click "Expected Artifacts" tab and set "Docker image" as index.docker.io/aozturk12/hello.
- Click "Automated Triggers" tab, add new trigger of type Docker registry, select your registry and select the aozturk12/hello image. Please leave "tag" blank.
- For now deselect the "Trigger Enabled" option.
- Add a new stage of type Deploy(Manifest).
- Copy the contents of example.yml. Notice that there is no "tag" part in image name.
- Make sure you've filled the "Req. Artifacts To Bind" option with Docker image artifact ID.
- Create "test" namespace in k8s.
- Start manual execution and select the tag you want to deploy. It will create the deployment and service.
- If you want new versions to be automatically deployed whenever a new Docker image is uploaded to the registry, you should select "Trigger Enabled" option obviously.

### References

https://www.spinnaker.io/setup/install/

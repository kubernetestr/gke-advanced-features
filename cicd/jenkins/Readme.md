#### Set Default Region
```bash
gcloud config set compute/zone us-central1-c
```

#### Configure default project
```bash
gcloud config get-value core/project
export PROJECT_ID=<PROJECT_ID>
gcloud config set project $PROJECT_ID
```
#### Enable kuberntes APIs
```bash
gcloud services enable compute.googleapis.com container.googleapis.com
```
#### Create kubernetes clusters
```bash
gcloud container clusters create tools-cluster \
      --cluster-version=1.10 \
      --num-nodes 3 --machine-type n1-standard-2 --scopes cloud-platform

gcloud container clusters create staging-cluster \
      --cluster-version=1.10 \
      --num-nodes 3 --machine-type n1-standard-2 --scopes cloud-platform
gcloud container clusters list
```

```bash
NAME             LOCATION       MASTER_VERSION  MASTER_IP      MACHINE_TYPE   NODE_VERSION   NUM_NODES  STATUS
staging-cluster  us-central1-c  1.10.12-gke.1   35.224.225.27  n1-standard-2  1.10.12-gke.1  3          RUNNING
tools-cluster    us-central1-c  1.10.12-gke.1   35.202.81.130  n1-standard-2  1.10.12-gke.1  3          RUNNING
```

```bash
gcloud container clusters get-credentials tools-cluster --zone us-central1-c --project inspired-bus-194216
```

#### Install Helm
```bash
kubectl create clusterrolebinding user-admin-binding --clusterrole=cluster-admin --user=$(gcloud config get-value account)
kubectl create serviceaccount tiller --namespace kube-system
kubectl create clusterrolebinding tiller-admin-binding --clusterrole=cluster-admin --serviceaccount=kube-system:tiller
helm init --service-account=tiller
helm update
```
#### Install Jenkins
```bash
helm install --name jenkins stable/jenkins --namespace jenkins -f values.yaml
#print password
 printf $(kubectl get secret --namespace jenkins jenkins -o jsonpath="{.data.jenkins-admin-password}" | base64 --decode);echo
#connect jenkins
kubectl port-forward -n jenkins \n
     $(kubectl get pods --namespace jenkins -l "component=jenkins-jenkins-master" -o jsonpath="{.items[0].metadata.name}") 8080:8080
```
* Login to Jenkins from http://localhost:8080
* Create Pipeline
* Copy Jenkinsfile content to pipeline script panel


#### Cretae Service Account for managing kubernetes clusters
```bash
export PROJECT_ID=$(gcloud config get-value core/project)
 gcloud alpha iam service-accounts create jenkins-sa --display-name jenkins-sa
 gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member serviceAccount:jenkins-sa@$PROJECT_ID.iam.gserviceaccount.com --role roles/container.developer

gcloud iam service-accounts keys create ~/key.json \
  --iam-account jenkins-sa@$PROJECT_ID.iam.gserviceaccount.com

kubectl create secret generic k8s-developer-sa --from-file=$HOME/key.json  -n jenkins
```

* Build the jenkins pipeline
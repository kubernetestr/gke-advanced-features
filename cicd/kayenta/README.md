# Canary Deployment with Kayenta 

This is a step by step guide about how to create canary pipelines and automating canary analysis with Kayenta.

### Prerequisities

- Spinnaker
- Prometheus


### Deploy Sample Application

- Create a new application and name it "sampleapp" in Spinnaker.

- Change Kubernetes account in pipeline json file according to your deployment:

```
sed -i 's/my-kubernetes-account/spinnaker-account/g' simple-deploy.json
```

- Create pipeline via Gate API:

```
curl -d@simple-deploy.json -X POST \
    -H "Content-Type: application/json" -H "Accept: */*" \
    http://spin-gate-IP/pipelines
```

- Manually execute the pipeline and select "Success Rate" 70.

- Create a pod that makes requests to the sample app:

```
kubectl -n default run injector --image=alpine -- \
    /bin/sh -c "apk add --no-cache --yes curl; \
    while true; do curl -sS --max-time 3 \
    http://sampleapp:8080/; done"
```


### Canary Deployment with Manual Judgement

- Change Kubernetes account in pipeline json file according to your deployment:

```
sed -i 's/my-kubernetes-account/spinnaker-account/g' canary-deploy.json
```


- Create pipeline via Gate API:

```
export PIPELINE_ID=$(curl \
    spin-gate-IP/applications/sampleapp/pipelineConfigs/Simple%20deploy \
    | jq -r '.id')
jq '(.stages[] | select(.refId == "9") | .pipeline) |= env.PIPELINE_ID | (.stages[] | select(.refId == "8") | .pipeline) |= env.PIPELINE_ID' canary-deploy.json | \
    curl -d@- -X POST \
    -H "Content-Type: application/json" -H "Accept: */*" \
    http://spin-gate-IP/pipelines
```

Canary analysis judgement is manual for now. You can test this by executing pipeline manually and selecting different "Success Rate" values and then decide promoting canary or fail it according to metrics.


### Configure Prometheus

```
kubectl apply -f sampleapp-service-monitor.yaml
```

### Automating Canary Analysis

- Configure Halyard:

```
hal config canary enable
hal config canary prometheus enable
hal config canary prometheus account add prod-prometheus --base-url http://kube-prometheus.monitoring:9090
hal config canary google enable
PROJECT=$(gcloud info --format='value(config.project)')
JSON_PATH=~/.gcp/gcs-account.json
SPINNAKER_BUCKET=ace-spin
hal config canary google account add storage-google-account \
  --project $PROJECT \
  --json-path $JSON_PATH \
  --bucket $SPINNAKER_BUCKET
hal config canary google edit --gcs-enabled true
hal config canary edit --default-metrics-store prometheus
hal config canary edit --default-metrics-account prod-prometheus
hal config canary edit --default-storage-account storage-google-account
hal deploy apply
```

Note: Spinnaker bucket and gcs-account json was created during Spinnaker installation. Please take a look at cicd/spinnaker folder in this repository for details.

- In Spinnaker, click Config. In the Features section, select Canary, and then click Save Changes.
- Now that canary is enabled, reload Spinnaker. The Pipelines section is replaced with Delivery. In the Delivery section, go to Canary Configs.
- Click Add Configuration. For Configuration Name, enter kayenta-test.
- Click Add Metric: Name: error_rate, Fail on: Increase, Metric Name: Requests. Filter Template name: http_code template:http_code = "500",pod=~"${scope}.*". Check "Fail the canary if this metric fails".
- Thresholds: Marginal=75, Pass=95, Group1=100
- Go to Delivery > Pipelines, and for the Canary Deploy pipeline, click Configure.
- Click Add Stage. For Type, select Canary Analysis. In the Depends On section, select Deploy Canary and Deploy Baseline.
- Canary Analysis Configuration: Config Name: kayenta-test, Lifetime: 5min, Delay: 0, Interval: 1, Step: 15, Baseline: sampleapp-baseline, Baseline Location: default, Canary: sampleapp-canary, Canary Location: default, Scoring Thresholds Marginal: 75, Pass: 95
- In the Execution Options section, select Ignore the failure.
- In the pipeline's schema, click Deploy to Production. Change the Depends On section: Add Canary Analysis. Remove Manual Judgment.
- Change the Conditional on Expression parameter: ${ #stage('Canary Analysis')['status'].toString() == 'SUCCEEDED'}
- In the pipeline's schema, click Delete Canary, and change the Depends On section: Add Canary Analysis. Remove Manual Judgment.
- In the pipeline's schema, click Delete Baseline, and change the Depends On section: Add Canary Analysis. Remove Manual Judgment.
- Click Successful deployment, edit Check Preconditions Configuration: ${ #stage('Canary Analysis')['status'].toString() == 'SUCCEEDED'}
- Remove Manual Judgement stage.


### Test

- Run Simple Deploy pipeline with "Success Rate" 80, then check the pods.
- Run Canary Deploy pipeline with "Success Rate" 50, then canary should fail.
- Run Canary Deploy pipeline with "Success Rate" 90, then canary should pass and do not forget to check new pods.

### References

https://cloud.google.com/solutions/automated-canary-analysis-kubernetes-engine-spinnaker

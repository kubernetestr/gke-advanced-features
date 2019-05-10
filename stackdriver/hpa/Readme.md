#### StackDriver based Horizontal Pod Autoscaler
This sample is based on https://cloud.google.com/kubernetes-engine/docs/tutorials/external-metrics-autoscaling
```bash
kubectl apply -f https://raw.githubusercontent.com/GoogleCloudPlatform/k8s-stackdriver/master/custom-metrics-stackdriver-adapter/deploy/production/adapter.yaml
```

Deploy your deployment, ingress and service
```bash
kubectl apply -f nginx-deployment.yaml
kubectl apply -f nginx-service.yaml
kubectl apply -f nginx-ingress.yaml
```

Extract your forwarding rule based on kubectl describe ingress nginx-ingress output
```yaml
  - external:
      metricName: loadbalancing.googleapis.com|https|request_count
      metricSelector:
        matchLabels:
          resource.labels.forwarding_rule_name: k8s-fw-default-nginx-ingress--58fb2804f96711b1
      targetAverageValue: "10"
    type: External 
```

```bash
kubectl apply -f nginx-hpa.yaml
```
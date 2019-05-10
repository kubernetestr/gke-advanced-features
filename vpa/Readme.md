#### Vertical Pod Autoscaler
Create GKE Cluster
```bash
gcloud beta container clusters create vpa-clutser  --enable-vertical-pod-autoscaling --cluster-version=1.12.7
```
Finding and setting optimal values for pods can be challanging. On production, from prometheus or from stackdriver we can find and re-tune our applications cpu / memory requests/limits. This can be time-consuming process for a large cluster. Insted of that, googles gke vertical pod autoscaler promises finding and recommending these values  and also setting them on runtime. These sample is based oon
https://cloud.google.com/kubernetes-engine/docs/how-to/vertical-pod-autoscaling

```bash
kubectl apply -f nginx-deployment.yaml
kubectl apply -f nginx-service.yaml
kubectl apply -f nginx-ingress.yaml
```

```yaml
apiVersion: autoscaling.k8s.io/v1beta2
kind: VerticalPodAutoscaler
metadata:
  name: nginx-vpa
spec:
  targetRef:
    apiVersion: "extensions/v1beta1"
    kind:       Deployment
    name:       nginx
  updatePolicy:
    updateMode: "Off"
```
```bash
kubectl get vpa nginx-vpa -o yaml
```
Wait 5 minuts.
```bash
kubectl get vpa nginx-vpa -o yaml
```
```yaml
recommendation:
    containerRecommendations:
    - containerName: nginx
      lowerBound:
        cpu: 25m
        memory: 262144k
      target:
        cpu: 25m
        memory: 262144k
      uncappedTarget:
        cpu: 25m
        memory: 262144k
      upperBound:
        cpu: 15851m
        memory: 16571500k
```
Change update mode
```yaml
apiVersion: autoscaling.k8s.io/v1beta2
kind: VerticalPodAutoscaler
metadata:
  name: nginx-vpa
spec:
  targetRef:
    apiVersion: "extensions/v1beta1"
    kind:       Deployment
    name:       nginx
  updatePolicy:
    updateMode: "Auto"
```
```bash
kubectl apply -f nginx-vpa
```
Wait for VPA to kicks-in. Notice that pod name will be changed.
```bash
kubectl get pod [POD_NAME] -o yaml
```

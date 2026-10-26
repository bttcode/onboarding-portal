# Deploying to the OpenShift Developer Sandbox

The [Red Hat Developer Sandbox](https://developers.redhat.com/developer-sandbox) gives a free
OpenShift project. The images come from GitHub Container Registry, built by the CI workflow on
every push to `main`.

## 1. Make the images pullable

GHCR packages are private by default. In GitHub, open each package
(`onboarding-backend`, `onboarding-frontend`, `onboarding-sanctions-mock`) → Package settings →
Change visibility → Public. Alternatively, create a pull secret with `oc create secret docker-registry`.

If your GitHub user is not `bttcode`, replace `ghcr.io/bttcode/` in `openshift/*.yaml`.

## 2. Secrets and realm

```bash
oc login --token=... --server=...          # "Copy login command" in the OpenShift console
cp openshift/secret.example.yaml openshift/secret.yaml   # git-ignored; set real passwords
oc apply -f openshift/secret.yaml
```

## 3. First apply, to get the route hosts

```bash
oc create configmap onboarding-realm --from-file=keycloak/onboarding-realm.json
oc apply -k openshift/
oc get routes        # note the hosts of "keycloak" and "portal"
```

## 4. Wire the public URLs

1. Put both hosts (with `https://`) into `openshift/config.yaml`.
2. In `keycloak/onboarding-realm.json`, add `https://<portal host>/*` to the client's `redirectUris`.
3. Apply again and restart:

```bash
oc create configmap onboarding-realm --from-file=keycloak/onboarding-realm.json \
   --dry-run=client -o yaml | oc apply -f -
oc apply -k openshift/
oc rollout restart deployment/keycloak deployment/backend deployment/frontend
```

The portal is at `https://<portal host>`. The API and the check service have no route: the portal's
nginx proxies `/api` to the backend inside the project. For the outage demo, reach the check
service with `oc port-forward svc/sanctions-mock 8090`.

## Notes

- Keycloak runs in dev mode with an embedded database, so restarting it re-imports the realm
  and forgets self-registered users. Fine for a demo; not for real use.
- Sandbox projects are deleted after their trial period; the CI images stay on GHCR.

# Two-minute demo

Start the stack with `docker compose up --build` and wait until http://localhost:8081 loads.
Use a private browser window per user, so the sessions do not mix.

## 1. Apply (30 s)

1. Open http://localhost:8081 and log in as `alice` / `alice`.
2. **Apply**: fill in the three steps and drop a PDF or image on the upload area.
   Narrow the window to show the phone layout of the stepper.
3. Submit. **My applications** shows `Submitted` and `Check pending`, then `In review` and
   `Check clear` a few seconds later, after the outbox poller has run.

## 2. Four-eyes review (40 s)

1. In another private window, log in as `carol` / `carol`. The **Review queue** opens, sorted
   oldest first. Sort by applicant, page through, and search by name to show server-side paging.
2. Open Alice's application, download the document, and **Approve**. The status stays
   `In review`: one approval is not enough, and Carol cannot approve a second time.
3. Log in as `dave` / `dave` and approve. The status becomes `Approved`.

## 3. The check service goes down (50 s)

```bash
# take the check service down
curl -X PUT localhost:8090/admin/chaos -H 'Content-Type: application/json' \
     -d '{"down": true, "failureRate": 0, "latencyMs": 0}'
```

1. As Alice, submit a new application. It stays `Check pending`; nothing is lost.
2. Show the backend logs (`docker compose logs -f backend`): quick retries, then the circuit
   breaker opening, then the outbox scheduling the next attempt further and further out.
3. Optional: `curl localhost:8080/actuator/health` with a caseworker token shows the circuit breaker state.
4. Bring the service back:

   ```bash
   curl -X PUT localhost:8090/admin/chaos -H 'Content-Type: application/json' \
        -d '{"down": false, "failureRate": 0, "latencyMs": 0}'
   ```

5. On the next outbox attempt the application moves to `In review`.
6. `curl localhost:8090/admin/stats`: `totalCalls` counts every retry, `distinctChecks` counts
   the checks actually performed, one per application, thanks to the idempotency key.

To show the manual fallback, keep the service down until the last attempt (lower
`onboarding.outbox.max-attempts` and `max-backoff` for the demo). The check turns `Check failed`,
and a caseworker can press **Retry check**.

## Variations

- Last name `Blocked`: the check reports a match, and approving requires a written justification.
- `{"down": false, "failureRate": 0.5, "latencyMs": 0}`: half the calls fail and the retries absorb it.
- `{"down": false, "failureRate": 0, "latencyMs": 5000}`: slower than the 3-second read timeout,
  so calls time out.

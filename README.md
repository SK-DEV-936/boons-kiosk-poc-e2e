# Stripe Terminal M2 - End-to-End POC

**Status:** SUCCESS (Payment Flow Verified)
**Lead:** [Your Name/Title]

This repository contains a complete, working Proof of Concept (POC) for the **Stripe Terminal M2 Reader** integration on React Native (Android), backed by a custom **Java Spring Boot** server.

Use this as the **Reference Implementation** for the production build.

---

## 1. Architecture

This solution resolves previous integration blockers by implementing a verifiable "Golden Path":

| Component | Tech Stack | Port | Description |
| :--- | :--- | :--- | :--- |
| **Backend** | Java Spring Boot | `4567` | Handles Stripe Secrets, Token Creation, Payment Intents |
| **Mobile** | React Native | N/A | Android App that connects to M2 Reader via Bluetooth |
| **Protocol** | REST / HTTPS | N/A | Mobile App talks to Backend to get Tokens/Intents |

---

## 2. Quick Start (Automation)

We have created a single script to build the backend, install app dependencies, and launch everything.

```bash
# 1. Connect Android Device via USB
# 2. Ensure your device is on the SAME WiFi as your laptop

# 3. Run the E2E Script
source start_e2e.sh
```

**What the script does:**
1.  Builds & Starts the Java Server (`mvnw spring-boot:run`).
2.  Updates `.env` with your computer's local IP (`API_URL=http://10.0.0.X:4567`).
3.  Installs React Native dependencies (`yarn install`).
4.  Builds & Installs the Android App (`yarn android`).
5.  Streams server logs to `backend.log`.

---

## 3. Critical Implementation Details (The "Unblock")

The following patterns **MUST** be followed to ensure the M2 Reader works correctly.

### A. Backend: JSON Response Format (Critical Fix)
Previous recurring failures were caused by the backend returning the wrong JSON structure for Payment Intents. The Mobile SDK expects specific fields.

**Reference Implementation (`TerminalController.java`):**
```java
@PostMapping("/create_payment_intent")
public Map<String, String> createPaymentIntent(@RequestBody Map<String, Object> data) {
    // ... setup params ...
    PaymentIntent intent = PaymentIntent.create(params);

    Map<String, String> response = new HashMap<>();
    response.put("secret", intent.getClientSecret());
    response.put("id", intent.getId()); // MUST return "id", not "intent" object
    return response;
}
```

### B. Mobile: API Client (Critical Fix)
The mobile client must correctly parse the response. **Do not** look for a nested `intent` object if the server communicates flat IDs.

**Reference Implementation (`api.ts`):**
```typescript
createPaymentIntent(...) {
    // ...
    return fetch(`${this.api_url}/create_payment_intent`, ...)
        .then((resp) => resp.json())
        .then((j) => {
            console.log('[Api] createPaymentIntent response:', j);
            return {
                client_secret: j.secret,
                id: j.id, // READ THIS EXACT FIELD associated with Java Map key
            };
        });
}
```

### C. Hardware Stability ("Zombie Reader" Fix)
If the M2 Reader connects but refuses to take payments (Lights don't flash, "Zombie" state):
1.  **Hard Reset:** Hold the power button on the M2 for **10+ seconds** until it fully reboots.
2.  **Permissions:** Ensure `ACCESS_FINE_LOCATION` and `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` are granted at runtime.
3.  **Simulated Mode:** Ensure the app is NOT in "Simulated" mode. The toggle is on the Home Screen.

---

## 4. Environment Variables

**Backend (`java-server/src/main/resources/application.properties`):**
```properties
server.port=4567
stripe.api.key=sk_test_... # YOUR SECRET KEY HERE
```

**Mobile (`stripe-terminal-react-native/example-app/.env`):**
```env
API_URL=http://<YOUR_LOCAL_IP>:4567
```

---

## 5. Next Steps for Production Team
1.  **Copy `TerminalController.java` endpoints** to the main production backend.
2.  **Update Mobile App `api.ts`** to match the logging and parsing logic shown here.

---

## 6. Key Files for Developer Review

Developers implementing the production solution should focus their code review on these specific files:

| Scope | File Path | Purpose |
| :--- | :--- | :--- |
| **Backend Logic** | [`java-server/.../TerminalController.java`](java-server/src/main/java/com/example/terminalbackend/TerminalController.java) | Contains the specific endpoints (`create_payment_intent`) and the correct response map logic. |
| **Mobile API** | [`.../example-app/src/api/api.ts`](stripe-terminal-react-native/example-app/src/api/api.ts) | Shows how to properly log requests and parse the specific `id` field from the backend. |
| **Mobile UI** | [`.../src/screens/CollectCardPaymentScreen.tsx`](stripe-terminal-react-native/example-app/src/screens/CollectCardPaymentScreen.tsx) | Handles the payment flow, including the "Cancel Payment" logic. |
| **Automation** | [`start_e2e.sh`](start_e2e.sh) | The master script that orchestrates the entire build and run process. |
| **Config** | [`.../example-app/.env`](stripe-terminal-react-native/example-app/.env) | Where the `API_URL` is defined. Must point to your local machine IP. |


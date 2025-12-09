package com.example.terminalbackend;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.terminal.ConnectionToken;
import com.stripe.model.terminal.Location;
import com.stripe.model.terminal.Reader;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.terminal.ConnectionTokenCreateParams;
import com.stripe.param.terminal.ReaderCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class TerminalController {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    @PostMapping("/connection_token")
    public Map<String, String> createConnectionToken() {
        try {
            ConnectionTokenCreateParams params = ConnectionTokenCreateParams.builder().build();
            ConnectionToken token = ConnectionToken.create(params);
            Map<String, String> response = new HashMap<>();
            response.put("secret", token.getSecret());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    @PostMapping("/create_payment_intent")
    public Map<String, String> createPaymentIntent(@RequestBody Map<String, Object> data) {
        try {
            Long amount = Long.parseLong(data.get("amount").toString());
            String currency = (String) data.getOrDefault("currency", "usd");

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency(currency)
                    .addPaymentMethodType("card_present")
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL) // Default to manual capture for
                                                                                      // Terminal
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            Map<String, String> response = new HashMap<>();
            response.put("secret", intent.getClientSecret());
            response.put("id", intent.getId());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    @PostMapping("/capture_payment_intent")
    public Map<String, String> capturePaymentIntent(@RequestBody Map<String, Object> data) {
        try {
            String paymentIntentId = (String) data.get("payment_intent_id");

            PaymentIntentCaptureParams params = PaymentIntentCaptureParams.builder().build();
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            intent = intent.capture(params);

            Map<String, String> response = new HashMap<>();
            response.put("secret", intent.getClientSecret());
            response.put("id", intent.getId());
            response.put("status", intent.getStatus());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    @PostMapping("/cancel_payment_intent")
    public Map<String, String> cancelPaymentIntent(@RequestBody Map<String, Object> data) {
        try {
            String paymentIntentId = (String) data.get("payment_intent_id");

            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            intent = intent.cancel();

            Map<String, String> response = new HashMap<>();
            response.put("secret", intent.getClientSecret());
            response.put("id", intent.getId());
            response.put("status", intent.getStatus());
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    @PostMapping("/register_reader")
    public Reader registerReader(@RequestBody Map<String, String> data) throws StripeException {
        ReaderCreateParams.Builder paramsBuilder = ReaderCreateParams.builder()
                .setRegistrationCode(data.get("registration_code"))
                .setLabel(data.get("label"));

        if (data.containsKey("location")) {
            paramsBuilder.setLocation(data.get("location"));
        }

        Reader reader = Reader.create(paramsBuilder.build());
        return reader;
    }
}

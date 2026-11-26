package org.paykesho.clientportal.transactions.pin;

import org.paykesho.clientportal.security.ClientUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.Map;

/** PIN endpoints are authenticated and tied to the principal's client ID. */
@RestController
@RequestMapping("/transactions/pin")
public class TransactionPinController {
    private final TransactionPinService pins;
    private final PasswordEncoder passwordEncoder;

    public TransactionPinController(TransactionPinService pins, PasswordEncoder passwordEncoder) {
        this.pins = pins;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@AuthenticationPrincipal ClientUserDetails me) {
        if (me == null || me.getClient() == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(Collections.singletonMap("pinSet", pins.isSet(me.getClient().getId())));
    }

    @PostMapping("/setup")
    public ResponseEntity<?> setup(@AuthenticationPrincipal ClientUserDetails me, @RequestBody Map<String, String> body) {
        if (!reauthenticated(me, body)) return unauthorized();
        try {
            TransactionPinService.Result result = pins.setup(me.getClient().getId(), body.get("pin"));
            return respond(result);
        } catch (DataIntegrityViolationException duplicate) {
            // Concurrent setup requests race on client_id's primary key; never overwrite.
            return respond(TransactionPinService.Result.ALREADY_SET);
        }
    }

    @PostMapping("/change")
    public ResponseEntity<?> change(@AuthenticationPrincipal ClientUserDetails me, @RequestBody Map<String, String> body) {
        if (!reauthenticated(me, body)) return unauthorized();
        return respond(pins.change(me.getClient().getId(), body.get("currentPin"), body.get("newPin")));
    }

    private boolean reauthenticated(ClientUserDetails me, Map<String, String> body) {
        return me != null && me.getClient() != null && body != null
                && body.get("password") != null
                && passwordEncoder.matches(body.get("password"), me.getPassword())
                && me.getClient().isEnabled();
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Collections.singletonMap("message", "Please re-enter your account password."));
    }

    private ResponseEntity<?> respond(TransactionPinService.Result result) {
        switch (result) {
            case OK: return ResponseEntity.ok(Collections.singletonMap("message", "Transaction PIN saved."));
            case NOT_SET: return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(Collections.singletonMap("message", "Set your transaction PIN first."));
            case ALREADY_SET: return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", "A transaction PIN already exists. Use change PIN."));
            case WRONG: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("message", "Incorrect transaction PIN."));
            case LOCKED: return ResponseEntity.status(HttpStatus.LOCKED).body(Collections.singletonMap("message", "Transaction PIN is locked. Account recovery is required."));
            case SAME_PIN: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Choose a different PIN."));
            default: return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "PIN must be six digits and not an obvious sequence."));
        }
    }
}

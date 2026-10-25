package org.paykesho.clientportal.clients.service;

/** A registration that fails a business rule; the message is safe to show the applicant. */
public class RegistrationRejectedException extends RuntimeException {
    public RegistrationRejectedException(String message) {
        super(message);
    }
}

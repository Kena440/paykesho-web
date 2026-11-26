package org.paykesho.backoffice.kyc;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Formats KYC timestamps in Zambian time for templates: ${@kycDates.format(...)}. */
@Component("kycDates")
public class KycDates {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.of("Africa/Lusaka"));

    public String format(Instant instant) {
        return instant == null ? "" : FORMAT.format(instant);
    }
}

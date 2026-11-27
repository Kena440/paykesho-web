package org.paykesho.clientportal.clients.service;


import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.commons.entity.client.Client;
import org.commons.entity.client.ClientBalance;
import org.commons.entity.organization.Organization;
import org.commons.entity.settings.Country;
import org.commons.enums.AuthenticationType;
import org.commons.enums.ClientType;
import org.commons.security.SecurityValidator;
import org.commons.utils.PaykeshoError;
import org.paykesho.clientportal.clients.repository.ClientBalanceRepository;
import org.paykesho.clientportal.clients.repository.ClientRepository;
import org.paykesho.clientportal.clients.repository.EmployeeRecordRepository;
import org.paykesho.clientportal.organizations.repository.OrganizationRepository;
import org.paykesho.clientportal.config.Utility;
import org.paykesho.clientportal.notification.general.NotificationClient;
import org.paykesho.clientportal.settings.repository.CountryRepository;
import org.paykesho.clientportal.utils.PaykeshoReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.commons.entity.kyc.ClientKycApplication;
import org.commons.entity.kyc.KycDocument;
import org.commons.enums.FileType;
import org.commons.enums.KycDocumentType;
import org.commons.enums.KycStatus;
import org.commons.utils.StoredFiles;
import org.paykesho.clientportal.files.service.FileUploadService;
import org.paykesho.clientportal.kyc.ClientKycApplicationRepository;
import org.paykesho.clientportal.kyc.KycDocumentRepository;
import org.paykesho.clientportal.kyc.KycStatusService;
import org.paykesho.clientportal.kyc.RegistrationKycForm;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class ClientService {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_TIME_DURATION = 24 * 60 * 60 * 1000; // 24 hours
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private EmployeeRecordRepository employeeRecordRepository;
    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;
    @Autowired
    private ClientBalanceRepository clientBalanceRepository;
    @Autowired
    private NotificationClient notificationClient;
    @Autowired
    private ClientKycApplicationRepository kycApplicationRepository;
    @Autowired
    private KycDocumentRepository kycDocumentRepository;
    @Autowired
    private KycStatusService kycStatusService;
    @Autowired
    private FileUploadService fileUploadService;
    @Autowired
    private Validator validator;

    public List<Country> countryList() {
        try {
            log.debug("Retrieving country list");
            List<Country> countries = countryRepository.findAllByOrderByNameDesc();
            log.debug("Successfully retrieved {} countries", countries.size());
            return countries;
        } catch (Exception ex) {
            log.error("Error retrieving country list: {}", ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve country list", ex);
        }
    }

    public boolean isEmailUnique(String email) {
        try {
            log.debug("Checking email uniqueness for: {}", email);
            Client client = clientRepository.findByEmail(email);
            boolean isUnique = client == null;
            log.debug("Email {} is unique: {}", email, isUnique);
            return isUnique;
        } catch (Exception ex) {
            log.error("Error checking email uniqueness for {}: {}", email, ex.getMessage(), ex);
            throw new RuntimeException("Failed to check email uniqueness", ex);
        }
    }

    /**
     * Registers an employee for KYC review (PAYKESHO REGISTRATION). The account is
     * created inactive with a KYC application and the uploaded documents; the
     * back office approves it before the client can sign in.
     */
    public void registerClient(Client client, RegistrationKycForm kyc, Map<KycDocumentType, MultipartFile> documents) {
        try {
            log.debug("Registering new client with email: {}", client.getEmail());

            // Client type is system-controlled; the disabled form field is not submitted.
            client.setClientType(ClientType.FORMAL);
            composeNameAndAddress(client, kyc);

            // SECURITY: Validate and sanitize all client input
            try {
                SecurityValidator.validateAndSanitizeClient(client);
            } catch (SecurityException se) {
                log.error("SECURITY VIOLATION during client registration: {}", se.getMessage());
                throw new RuntimeException("Invalid input: Malicious content detected", se);
            }
            Set<ConstraintViolation<Client>> violations = validator.validate(client);
            if (!violations.isEmpty()) {
                throw new RegistrationRejectedException(violations.iterator().next().getMessage() + ".");
            }

            // Registration is only for employees of an employer on the platform whose
            // details match that employer's staff list (PAYKESHO REGISTRATION, step 3 j-k).
            // The registration page checks this before submitting; enforce it here too so
            // a direct POST cannot bypass it.
            Organization organization = client.getOrganization();
            if (organization == null || organization.getId() == null) {
                throw new RegistrationRejectedException("Please select your employer.");
            }
            organization = organizationRepository.findById(organization.getId()).orElse(null);
            if (organization == null || !organization.isEnabled() || organization.isDeleted()) {
                throw new RegistrationRejectedException("Your employer is not registered on Paykesho.");
            }
            client.setOrganization(organization);
            String status = checkClientStatus(nameCandidates(kyc), client.getNrc(), client.getTpin(), client.getManNumber(), organization);
            if ("Duplicated".equals(status)) {
                throw new RegistrationRejectedException("An account already exists for these employee details.");
            }
            if (!"OK".equals(status)) {
                throw new RegistrationRejectedException(
                    "Your details do not match your employer's staff list. Please check your name, NRC, TPIN and man number, or contact your employer.");
            }
            if (clientRepository.findByEmail(client.getEmail()) != null) {
                throw new RegistrationRejectedException("An account already exists for this email address.");
            }

            // Read and check every document before anything is saved.
            Map<KycDocumentType, byte[]> files = readDocuments(documents);

            encodePassword(client);
            client.setEnabled(false); // activated by the back office after KYC review
            client.setAccountNonLocked(true);
            client.setCreatedTime(new Date());
            client.setAuthenticationType(AuthenticationType.DATABASE);

            String randomCode = RandomString.make(64);
            client.setVerificationCode(randomCode);
            Client savedClient = clientRepository.save(client);
            createClientBalance(savedClient);

            ClientKycApplication application = new ClientKycApplication();
            application.setClient(savedClient);
            application.setFirstName(kyc.getFirstName());
            application.setMiddleName(kyc.getMiddleName());
            application.setLastName(kyc.getLastName());
            application.setHouseNumber(kyc.getHouseNumber());
            application.setRoad(kyc.getRoad());
            application.setArea(kyc.getArea());
            application.setStatus(KycStatus.SUBMITTED);
            application = kycApplicationRepository.save(application);

            for (Map.Entry<KycDocumentType, byte[]> e : files.entrySet()) {
                MultipartFile original = documents.get(e.getKey());
                Long fileId = fileUploadService.storeOwnedBytes(e.getValue(), original.getOriginalFilename(),
                        fileTypeFor(e.getKey()), savedClient.getUuid(), "KYC " + e.getKey().getLabel());
                KycDocument doc = new KycDocument();
                doc.setClientId(savedClient.getId());
                doc.setKycApplicationId(application.getId());
                doc.setDocumentType(e.getKey());
                doc.setFileUploadId(fileId);
                kycDocumentRepository.save(doc);
            }
            log.info("Client {} registered; KYC application {} submitted with {} documents",
                    savedClient.getId(), application.getId(), files.size());

            sendRegistrationNotifications(savedClient);
        } catch (RegistrationRejectedException ex) {
            log.info("Client registration rejected: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Error registering client with email {}: {}", client.getEmail(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to register client", ex);
        }
    }

    /** Documents required at registration; the selfie is optional. */
    static final List<KycDocumentType> REQUIRED_DOCUMENTS =
            List.of(KycDocumentType.NRC_FRONT, KycDocumentType.NRC_BACK, KycDocumentType.PAYSLIP);

    private Map<KycDocumentType, byte[]> readDocuments(Map<KycDocumentType, MultipartFile> documents) {
        Map<KycDocumentType, byte[]> files = new LinkedHashMap<>();
        for (KycDocumentType type : KycDocumentType.values()) {
            MultipartFile file = documents == null ? null : documents.get(type);
            boolean present = file != null && !file.isEmpty();
            if (!present) {
                if (REQUIRED_DOCUMENTS.contains(type)) {
                    throw new RegistrationRejectedException("Please attach your " + type.getLabel() + ".");
                }
                continue;
            }
            byte[] data;
            try {
                data = file.getBytes();
            } catch (IOException e) {
                throw new RegistrationRejectedException("Your " + type.getLabel() + " could not be read. Please try again.");
            }
            String problem = StoredFiles.rejectionReason(data, type == KycDocumentType.SELFIE);
            if (problem != null) {
                throw new RegistrationRejectedException(type.getLabel() + ": " + problem);
            }
            files.put(type, data);
        }
        return files;
    }

    private static FileType fileTypeFor(KycDocumentType type) {
        switch (type) {
            case PAYSLIP: return FileType.payslip;
            case SELFIE: return FileType.selfie;
            case NRC_FRONT:
            case NRC_BACK: return FileType.nrc;
            default: return FileType.photo;
        }
    }

    private static String clean(String s) {
        return s == null ? null : s.trim().replaceAll("\\s+", " ");
    }

    /** Name and address on the client record are composed from the KYC fields. */
    private void composeNameAndAddress(Client client, RegistrationKycForm kyc) {
        if (kyc == null) throw new RegistrationRejectedException("Please enter your name and address.");
        kyc.setFirstName(clean(kyc.getFirstName()));
        kyc.setMiddleName(clean(kyc.getMiddleName()));
        kyc.setLastName(clean(kyc.getLastName()));
        kyc.setHouseNumber(clean(kyc.getHouseNumber()));
        kyc.setRoad(clean(kyc.getRoad()));
        kyc.setArea(clean(kyc.getArea()));
        if (isBlank(kyc.getFirstName()) || isBlank(kyc.getLastName())) {
            throw new RegistrationRejectedException("Please enter your first and last name.");
        }
        if (isBlank(kyc.getHouseNumber()) || isBlank(kyc.getRoad()) || isBlank(kyc.getArea())) {
            throw new RegistrationRejectedException("Please enter your house number, road and area.");
        }
        for (String part : new String[]{kyc.getFirstName(), kyc.getMiddleName(), kyc.getLastName()}) {
            if (part != null && part.length() > 100) throw new RegistrationRejectedException("Each name must be at most 100 characters.");
        }
        if (kyc.getHouseNumber().length() > 50 || kyc.getRoad().length() > 45 || kyc.getArea().length() > 45) {
            throw new RegistrationRejectedException("House number must be at most 50 characters, and road and area at most 45 each.");
        }
        List<String> names = nameCandidates(kyc);
        client.setName(names.get(0));
        client.setAddress(String.join(", ", kyc.getHouseNumber(), kyc.getRoad(), kyc.getArea()));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Names that may appear on the employer's staff list: the full name, and the
     * name without the middle name.
     */
    static List<String> nameCandidates(RegistrationKycForm kyc) {
        List<String> names = new ArrayList<>();
        String first = clean(kyc.getFirstName());
        String middle = clean(kyc.getMiddleName());
        String last = clean(kyc.getLastName());
        if (!isBlank(middle)) names.add(first + " " + middle + " " + last);
        names.add(first + " " + last);
        return names;
    }

    private void sendRegistrationNotifications(Client client) {
        try {
            String clientName = client.getName() != null ? client.getName() : "User";
            String emailMessage = String.format(
                "<p>Welcome to Paykesho, %s!</p>" +
                "<p>We have received your registration and documents.</p>" +
                "<p>Email: %s</p>" +
                "<p>Paykesho will review them and let you know when you can sign in.</p>" +
                "<br><p>Get lelo, Pay Kesho!</p>",
                clientName, client.getEmail()
            );
            
            String smsMessage = String.format(
                "Welcome to Paykesho, %s! We have received your registration (%s). We will let you know when it is approved and you can sign in.",
                clientName, client.getEmail()
            );
            
            // Send email notification
            if (client.getEmail() != null && !client.getEmail().isEmpty()) {
                notificationClient.sendNotification(client.getEmail(), emailMessage, "email");
                log.info("Registration email notification sent to: {}", client.getEmail());
            }
            
            // Send SMS notification
            if (client.getPhoneNumber() != null && !client.getPhoneNumber().isEmpty()) {
                notificationClient.sendNotification(client.getPhoneNumber(), smsMessage, "sms");
                log.info("Registration SMS notification sent to: {}", client.getPhoneNumber());
            }
        } catch (Exception e) {
            log.error("Failed to send registration notifications for client {}: {}", client.getEmail(), e.getMessage(), e);
            // Don't throw exception - registration should succeed even if notifications fail
        }
    }

    private void createClientBalance(Client client) {
        ClientBalance balance = getClientBalance(client);
        if (balance == null) {
            balance = new ClientBalance();
            balance.setClient(client);
            balance.setCurrentBalance(BigDecimal.ZERO);
            balance.setAvailableBalance(BigDecimal.ZERO);
            balance.setHeldBalance(BigDecimal.ZERO);
            balance.setLastUpdate(new Date());
            saveClientBalance(balance);
        } else {
            log.warn("ClientBalance already exists for client id: {}. Skipping creation.", client.getId());
        }
    }

    private void encodePassword(Client client) {
        String encodedPassword = passwordEncoder.encode(client.getPassword());
        client.setPassword(encodedPassword);
    }

    public boolean verifyClient(String verificationCode) throws PaykeshoError {
        Client client = clientRepository.findByVerificationCode(verificationCode);
        String paykeshoRef = paykeshoReferenceService.generateCode("ITEM_REQ_CLT");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        if (client == null || client.isEnabled() || !kycStatusService.isCleared(client.getId())) {
            // Accounts awaiting KYC review are activated by the back office, not the e-mail link.
            return false;
        } else {
            client.setCode(paykeshoRef);
            clientRepository.enable(client.getId());
            return true;
        }
    }

    public void updateAuthenticationType(Client client, AuthenticationType authenticationType) {
        if (!client.getAuthenticationType().equals(authenticationType)) {
            clientRepository.updateAuthenticationType(client.getId(), authenticationType);
        }
    }

    public Client getClientByEmail(String email) {
        try {
            log.debug("Retrieving client by email: {}", email);
            Client client = clientRepository.findByEmail(email);
            if (client != null) {
                log.debug("Found client with ID: {}", client.getId());
            } else {
                log.debug("No client found with email: {}", email);
            }
            return client;
        } catch (Exception ex) {
            log.error("Error retrieving client by email {}: {}", email, ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve client by email", ex);
        }
    }

    public Client getClientByCode(String code) {
        try {
            log.debug("Retrieving client by code: {}", code);
            Client client = clientRepository.findByCode(code);
            if (client != null) {
                log.debug("Found client with ID: {}", client.getId());
            } else {
                log.debug("No client found with code: {}", code);
            }
            return client;
        } catch (Exception ex) {
            log.error("Error retrieving client by code {}: {}", code, ex.getMessage(), ex);
            throw new RuntimeException("Failed to retrieve client by code", ex);
        }
    }

    public Client getClientByResetToken(String token) {
        return clientRepository.findByResetToken(token);
    }


    /**
     * Accounts are only created through registration and KYC review; Google and
     * Facebook sign-in is for existing, approved clients, so this refuses.
     */
    public void addNewClientUponOAuthLogin(String name, String email, String countryCode, AuthenticationType authenticationType) {
        throw new RegistrationRejectedException("Please register with your employer details first.");
    }

    private void setName(String name, Client client) {
        String[] nameArray = name.split(" ");
        if (nameArray.length < 2) {
            client.setName(name);
        } else {
            String firstName = nameArray[0];
            client.setName(firstName);
 /*           String lastName = name.replaceFirst(firstName + " ", "");
            csoAdmin.setLastName(lastName);*/
        }
    }

    public void updateClient(Client clientInForm) {
        try {
            log.debug("Updating client with ID: {}", clientInForm.getId());
            
            // SECURITY: Validate and sanitize all client input
            try {
                SecurityValidator.validateAndSanitizeClient(clientInForm);
            } catch (SecurityException se) {
                log.error("SECURITY VIOLATION during client update: {}", se.getMessage());
                throw new RuntimeException("Invalid input: Malicious content detected", se);
            }
            
            Client clientInDB = clientRepository.findById(clientInForm.getId()).get();

            if (clientInDB.getAuthenticationType().equals(AuthenticationType.DATABASE)) {
                if (!clientInForm.getPassword().isEmpty()) {
                    String encodedPassword = passwordEncoder.encode(clientInForm.getPassword());
                    clientInForm.setPassword(encodedPassword);
                } else {
                    clientInForm.setPassword(clientInDB.getPassword());
                }
            } else {
                clientInForm.setPassword(clientInDB.getPassword());
            }

            clientInForm.setSalutation(clientInDB.getSalutation());
            clientInForm.setEnabled(clientInDB.isEnabled());
            clientInForm.setCreatedTime(clientInDB.getCreatedTime());
            clientInForm.setVerificationCode(clientInDB.getVerificationCode());
            clientInForm.setAuthenticationType(clientInDB.getAuthenticationType());
            clientInForm.setResetPasswordToken(clientInDB.getResetPasswordToken());
            clientRepository.save(clientInForm);
            log.debug("Successfully updated client with ID: {}", clientInForm.getId());
        } catch (Exception ex) {
            log.error("Error updating client with ID {}: {}", clientInForm.getId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to update client", ex);
        }
    }

    public String updateClientResetToken(String email) throws PaykeshoError {
        Client client = clientRepository.findByEmail(email);

        if (client != null) {
            String token = RandomString.make(30);
            client.setResetPasswordToken(token);
            clientRepository.save(client);

            return token;
        } else {
            throw new PaykeshoError("customer not found");
        }
    }

    public void updatePassword(String token, String newPassword) throws PaykeshoError {
        Client client = clientRepository.findByResetToken(token);
        if (client == null) {
            throw new PaykeshoError("no customer found. invalid token");
        }

        client.setPassword(newPassword);
        client.setResetPasswordToken(null);
        if (kycStatusService.isCleared(client.getId())) {
            client.setEnabled(true); // Enable account after successful password reset
        }
        encodePassword(client);
        clientRepository.save(client);
    }

    public Client getAuthenticatedClient(HttpServletRequest request) {
        String email = Utility.getEKeshofAuthenticatedClient(request);
        return getClientByEmail(email);
    }

    public void increaseFailedAttempts(Client client) {
        int newFailAttempts = client.getFailedAttempt() + 1;
        clientRepository.updateFailedAttempts(newFailAttempts, client.getEmail());
    }

    public void resetFailedAttempts(String email) {
        clientRepository.updateFailedAttempts(0, email);
    }

    public void lock(Client client) {
        client.setAccountNonLocked(false);
        client.setLockTime(new Date());

        clientRepository.save(client);
    }

    public boolean unlockWhenTimeExpired(Client client) {
        long lockTimeInMillis = client.getLockTime().getTime();
        long currentTimeInMillis = System.currentTimeMillis();

        if (lockTimeInMillis + LOCK_TIME_DURATION < currentTimeInMillis) {
            client.setAccountNonLocked(true);
            client.setLockTime(null);
            client.setFailedAttempt(0);

            clientRepository.save(client);

            return true;
        }

        return false;
    }

    public String checkClientStatus(List<String> names, String nrc, String tpin, String manNumber, Organization organization) {
        String result = "Not Found";
        for (String name : names) {
            result = checkClientStatus(name, nrc, tpin, manNumber, organization);
            if (!"Not Found".equals(result)) return result;
        }
        return result;
    }

    public String checkClientStatus(String name, String nrc, String tpin, String manNumber, Organization organization) {
        try {
            log.debug("Checking employee eligibility for organization ID: {}", organization.getId());
            String resp = "";

            boolean clientExists = clientRepository.existsByNrcAndTpinAndManNumberAndOrganization(nrc, tpin, manNumber, organization);
            if (clientExists) {
                resp = "Duplicated";
                log.debug("Client status: Duplicated");
                return resp;
            }

            boolean employeeExists = employeeRecordRepository.existsByNameAndNrcAndTpinAndManNumberAndOrganization(name, nrc, tpin, manNumber, organization);
            if (!employeeExists) {
                resp = "Not Found";
            } else {
                resp = "OK";
            }

            log.debug("Client status: {}", resp);
            return resp;
        } catch (Exception ex) {
            log.error("Employee eligibility check failed: {}", ex.getMessage());
            throw new RuntimeException("Failed to check client status", ex);
        }
    }

    public ClientBalance saveClientBalance(ClientBalance clientBalance) {
        return clientBalanceRepository.save(clientBalance);
    }

    public ClientBalance getClientBalance(Client client) {
        try {
            log.debug("Retrieving client balance for client ID: {}", client.getId());
            ClientBalance balance = clientBalanceRepository.getClientBalanceByClientId(client.getId());
            if (balance != null) {
                log.debug("Found client balance for client ID: {}", client.getId());
            } else {
                log.warn("No ClientBalance found for client id: {}. Returning null.", client.getId());
            }
            return balance;
        } catch (Exception ex) {
            log.error("Error retrieving client balance for client ID {}: {}", client.getId(), ex.getMessage(), ex);
            log.warn("No ClientBalance found for client id: {}. Returning null.", client.getId());
            return null;
        }
    }

    // UUID-based methods for API endpoints
    public Optional<Client> getClientById(Integer id) {
        try {
            return clientRepository.findById(id);
        } catch (Exception ex) {
            log.error("Error retrieving client by ID: {}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    public Client getClientByUuid(String uuid) throws PaykeshoError {
        if (uuid == null || uuid.trim().isEmpty()) {
            throw new PaykeshoError("Invalid UUID parameter");
        }

        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new PaykeshoError("Invalid UUID format");
        }

        return clientRepository.findByUuid(uuid)
                .orElseThrow(() -> new PaykeshoError("Could not find client with UUID: " + uuid));
    }
}

package org.paykesho.clientportal.api.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class APIController {

  /*  @Autowired
    private CustomerService clientService;
    @Autowired
    private CorporateService corporateService;
    @Autowired
    private VoucherService voucherService;
    @Autowired
    private MerchantService merchantService;
    @Autowired
    private TransactionsService transactionsService;
    @Autowired
    private PaykeshoReferenceService paykeshoReferenceService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private SMSNotificationService smsNotificationService;
    @Autowired
    private SettingService settingService;

    @PostMapping(value = "/login")
    public ResponseEntity<Map<String, Object>> authenticateUser(@RequestBody LoginDto loginDto) {
        Map<String, Object> response = new HashMap<>();

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword()));

        try {
            if (authentication.isAuthenticated()) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
                Client client = clientService.getClientByPaykeshoId(userDetails.getPaykeshoId());

                response.put("error", false);
                response.put("user", client);
            } else {
                response.put("error", true);
                response.put("message", "Authentication error");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody SignUpDto signUpDto) throws PaykeshoError {
        Map<String, Object> response = new HashMap<>();

        String paykeshoRef = paykeshoReferenceService.generatePaykeshoId("ITEM_REQ_CLT");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        try {
            if (clientService.getCustomerByEmail(signUpDto.getEmail()) == null) {

                Corporate corporate = corporateService.getCorporateByName(signUpDto.getCorporate());
                Country country = settingService.getCountryByName(signUpDto.getCountry());

                // create client object
                Client client = new Client();
                client.setPaykeshoId(paykeshoRef);
                client.setFirstName(signUpDto.getFirstName());
                client.setLastName(signUpDto.getLastName());
                client.setPhoneNumber(signUpDto.getPhoneNumber());
                client.setCity(signUpDto.getCity());
                client.setCorporate(corporate);
                client.setCountry(country);
                client.setState(signUpDto.getState());
                client.setAddressLine1(signUpDto.getAddressLine1());
                client.setAddressLine2(signUpDto.getAddressLine2());
                client.setPostalCode(signUpDto.getPostalCode());
                client.setEmail(signUpDto.getEmail());
                client.setPassword(signUpDto.getPassword());

                clientService.registerCustomer(client);

                response.put("error", false);
                response.put("client", client);

                //sendClientNotifications(client, paykeshoRef);

            } else {
                response.put("error", true);
                response.put("message", "Email exists already!!");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }

    @GetMapping("/vouchers/{uuid}")
    public ResponseEntity<?> getClientVouchers(@PathVariable("uuid") String clientUuid) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (clientUuid != null && !clientUuid.trim().isEmpty()) {
                Client client = clientService.getClientByUuid(clientUuid);

                List<VoucherDto> clientVoucherList = voucherService.getClientVouchersFlat(client);
                response.put("error", false);
                response.put("vouchers", clientVoucherList);
            } else {
                response.put("error", true);
                response.put("message", "data retrieval error");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }

    @GetMapping("/vouchers/{userUuid}/{merchantCode}")
    public ResponseEntity<?> getClientVouchersByMerchant(@PathVariable("userUuid") String userUuid, @PathVariable("merchantCode") String merchantCode) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (userUuid != null && !merchantCode.isEmpty()) {
                Client client = clientService.getClientByUuid(userUuid);
                MerchantDto merchant = merchantService.getMerchantByCode(merchantCode);

                List<VoucherDto> clientVoucherList = voucherService.getClientVouchersByMerchantFlat(client, merchant);
                response.put("error", false);
                response.put("vouchers", clientVoucherList);
            } else {
                response.put("error", true);
                response.put("message", "data retrieval error");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }

    @GetMapping("/merchant/{code}")
    public ResponseEntity<Map<String, Object>> getMerchantByCode(@PathVariable("code") String code) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (!code.isEmpty()) {
                MerchantDto merchant = merchantService.getMerchantByCode(code);

                response.put("error", false);
                response.put("merchant", merchant);
            } else {
                response.put("error", true);
                response.put("message", "Data retrieval error");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }

    @GetMapping("/transactions/{uuid}")
    public ResponseEntity<?> getClientTransactions(@PathVariable("uuid") String clientUuid) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (clientUuid != null && !clientUuid.trim().isEmpty()) {
                Client client = clientService.getClientByUuid(clientUuid);

                List<TransactionDto> clientTransactionList = transactionsService.getClientTransactionsFlat(client);
                response.put("error", false);
                response.put("transactions", clientTransactionList);
            } else {
                response.put("error", true);
                response.put("message", "data retrieval error");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }

    @GetMapping("/merchants")
    public ResponseEntity<?> getMerchants() {
        Map<String, Object> response = new HashMap<>();

        try {

            List<MerchantDto> merchantList = merchantService.getEnabledMerchantsFlat();

            if (!merchantList.isEmpty()) {
                response.put("error", false);
                response.put("merchants", merchantList);
            } else {
                response.put("error", true);
                response.put("message", "data retrieval error");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }

    //@PostMapping(value = "/voucher-request/{merchant}/{voucherAmount}/{payslipFileName}/{clientPaykeshoID}")
    @PostMapping(value = "/voucher-request")
    public ResponseEntity<Map<String, Object>> voucherRequest(@RequestBody VoucherRequestDto voucherRequestDto ) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (voucherRequestDto.getClientPaykeshoID() != null) {
                Voucher voucher = new Voucher();
                Voucher savedVoucher;
                Client client = clientService.getClientByPaykeshoId(voucherRequestDto.getClientPaykeshoID());
                Merchant merchantFromDB = merchantService.getMerchantByName(voucherRequestDto.getMerchant());

                String paykeshoRef = paykeshoReferenceService.generatePaykeshoId("ITEM_REQ_VCR");
                if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

                voucher.setPaykeshoReference(paykeshoRef);
                voucher.setApproved(false);
                voucher.setMerchant(merchantFromDB);
                voucher.setClient(client);
                voucher.setVoucherAmount(voucherRequestDto.getVoucherAmount());
                voucher.setVoucherAvailableAmount(voucherRequestDto.getVoucherAmount());
                voucher.setVoucherRequestDate(new Date());
                voucher.setPayslipFile(null);


                *//*      if (!file.isEmpty()) {
                    String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                    voucher.setPayslipFile(fileName);

                    savedVoucher = voucherService.save(voucher);
                    String uploadDir = "client_payslips/" + savedVoucher.getClient().getId();

                    if (!fileName.isEmpty()) {
                        AmazonS3Util.removeFolder(uploadDir);
                        AmazonS3Util.uploadFile(uploadDir, fileName, file.getInputStream());
                    }
                } else {*//*
     *//* if (voucher.getPayslipFile().isEmpty()) {
                    voucher.setPayslipFile(null);
                }*//*
                savedVoucher = voucherService.save(voucher);
                //}

                sendVoucherNotifications(savedVoucher, paykeshoRef);

                response.put("error", false);
                response.put("voucher", savedVoucher);
            } else {
                response.put("error", true);
                response.put("message", "Authentication error");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", true);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }

    @PostMapping(value = "/submit-payment")
    public ResponseEntity<Map<String, Object>> submitPayment(@RequestBody PaymentDto paymentDto) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (paymentDto != null) {

                Client client = clientService.getClientById(paymentDto.getClientId()).get();

                Transaction trx = runPaymentSubmission(client, paymentDto);

                TransactionDto transactionDto = new TransactionDto(
                        trx.getPaykeshoReference(),
                        trx.getMerchant().getMerchantName(),
                        trx.getTransactionAmount(),
                        trx.getTransactionDate(),
                        trx.getVoucher().getPaykeshoReference(),
                        trx.getTransactionNaration()
                );

                response.put("error", false);
                response.put("transaction", transactionDto);
            } else {
                response.put("error", true);
                response.put("message", "Authentication error");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", true);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }

    private Transaction runPaymentSubmission(Client client, PaymentDto paymentDto) throws PaykeshoError {
        String paykeshoRef = paykeshoReferenceService.generatePaykeshoId("ITEM_REQ_TRX");
        if (paykeshoRef == null) throw new PaykeshoError("FAILED_GEN_PAYKESHO_REF");

        String voucherCode = paymentDto.getVoucherCode();

        Voucher voucher = voucherService.getVoucherByCode(voucherCode);

        if (voucher == null) throw new PaykeshoError("voucher doesnt exist");

        BigDecimal voucherAvailableAmount = voucher.getVoucherAvailableAmount();
        BigDecimal paymentAmount = paymentDto.getTransactionAmount();
        //String meterAccountNumber = request.getParameter("meterAccountNumber");

        if (voucherAvailableAmount.compareTo(paymentAmount) == -1)
            throw new PaykeshoError("error processing payment. payment amount is larger");
        BigDecimal newVoucherAvailableAmount = voucherAvailableAmount.subtract(paymentAmount);

        voucher.setVoucherAvailableAmount(newVoucherAvailableAmount);
        Voucher updateVoucher = voucherService.save(voucher);

        Transaction transaction = new Transaction();
        transaction.setClient(client);
        transaction.setMerchant(voucher.getMerchant());
        transaction.setTransactionAmount(paymentAmount);
        transaction.setPaykeshoReference(paykeshoRef);
        transaction.setVoucher(updateVoucher);
        transaction.setTransactionDate(new Date());
        transaction.setTransactionNaration("test from app");
            transaction.setSettled(false);
//        if (!meterAccountNumber.isEmpty()) transaction.setMeterAccountNumber(meterAccountNumber);

        try {
            sendNotifications(transaction, paykeshoRef);
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return (transactionsService.save(transaction));
    }

    private void sendVoucherNotifications(Voucher voucher, String paykeshoRef) throws MessagingException, UnsupportedEncodingException {
        sendVoucherEmail(voucher);
        smsNotificationService.send(new SMSDto(voucher.getClient().getPhoneNumber(), "Voucher request submitted. Kindly wait for processing. #" + paykeshoRef));
    }

    private void sendClientNotifications(HttpServletRequest request, Client client, String paykeshoRef) throws MessagingException, UnsupportedEncodingException {
        String verifyURL = Utility.getSiteUrl(request) + "/verify?code=" + client.getVerificationCode();

        sendVerificationEmail(request, client);
        smsNotificationService.send(new SMSDto(client.getPhoneNumber(), "@PKID".replace("@PKID", client.getPaykeshoId()) + " Client registration successful. Kindly verify your account " + verifyURL));
    }

    private void sendVoucherEmail(Voucher voucher) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = voucher.getClient().getEmail();
        String subject = "Voucher Request";
        String content = "<p>Hello, </p>" +
                "<p>You requested for a voucher. Voucher Reference: \"" + voucher.getPaykeshoReference() + "\"</p>" +
                "<p>Kindly wait for processing</>" +
                "<br>" +
                "<p>Get lelo, Pay Kesho. </p>";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }

    private void sendVerificationEmail(HttpServletRequest request, Client customer) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = customer.getEmail();
        String subject = emailSettingBags.getCustomerVerificationSubject();
        String content = emailSettingBags.getCustomerVerificationContent();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", customer.getFullName());
        String verifyURL = Utility.getSiteUrl(request) + "/verify?code=" + customer.getVerificationCode();
        content = content.replace("[[URL]]", verifyURL);

        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }

    private void sendNotifications(Transaction transaction, String paykeshoRef) throws MessagingException, UnsupportedEncodingException {
        sendPaymentEmail(transaction);
        smsNotificationService.send(new SMSDto(transaction.getClient().getPhoneNumber(), (!transaction.getPaykeshoReference().isEmpty()) ? "Payment successful. Kindly wait for processing. #" + paykeshoRef : "Payment failed. Please try again later"));
        smsNotificationService.send(new SMSDto(transaction.getMerchant().getPhoneNumber(), (!transaction.getPaykeshoReference().isEmpty()) ? "Payment of goods received. Kindly note transaction id. #" + paykeshoRef : "Payment failed."));
    }

    private void sendPaymentEmail(Transaction transaction) throws MessagingException, UnsupportedEncodingException {
        EmailSettingBag emailSettingBags = settingService.getEmailSettingsBag();
        JavaMailSenderImpl mailSender = Utility.prepareMailSender(emailSettingBags);

        String toAddress = transaction.getClient().getEmail();
        String subject = "Paykesho Payment #" + transaction.getPaykeshoReference();
        String content = "<p>Hello, </p>" +
                "<p>You made a payment successfully of \"" + transaction.getTransactionAmount() + "\" to \"" + transaction.getMerchant().getMerchantName() + "\". Transaction Reference: \"" + transaction.getPaykeshoReference() + "\"</p>" +
                "<br>" +
                "<p>Get lelo, Pay Kesho. </p>";

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

        helper.setFrom(emailSettingBags.getFromAddress(), emailSettingBags.getSenderName());
        helper.setTo(toAddress);
        helper.setSubject(subject);

        helper.setText(content, true);

        mailSender.send(mimeMessage);
    }

*/
}

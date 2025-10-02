package org.paykesho.backoffice.organizations.controller;

import org.commons.entity.State;
import org.commons.entity.client.Client;
import org.commons.entity.client.EmployeeRecord;
import org.commons.entity.organization.Organization;
import org.commons.entity.organization.OrganizationAdmin;
import org.commons.entity.settings.Setting;
import org.commons.enums.FileType;
import org.commons.utils.PaykeshoError;
import org.paykesho.backoffice.files.dto.FileAccessResponse;
import org.paykesho.backoffice.files.dto.FileUploadResponse;
import org.paykesho.backoffice.files.service.FileUploadService;
import org.paykesho.backoffice.organizations.enums.Constants;
import org.paykesho.backoffice.organizations.service.OrganizationService;
import org.paykesho.backoffice.settings.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
@RequestMapping("/organizations")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private SettingService settingService;
    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("")
    public String listAllOrganizationsByPage(Model model) throws PaykeshoError {
        model.addAttribute("pageTitle", "List Organizations");
        model.addAttribute("organizationList", organizationService.listAll());
        return "organizations/organizations";
    }

    @GetMapping("/new")
    public String newOrganization(Model model) {
        String pageTitle = "New Organization";
        Organization organization = new Organization();
        List<State> stateList = settingService.listStatesByCountry("Zambia");

        organization.setLongitude(28.3228F);
        organization.setLatitude((float) -15.3875);
        organization.setEnabled(true);

        model.addAttribute("organization", organization);
        model.addAttribute("stateList", stateList);
        model.addAttribute("pageTitle", pageTitle);
        return "organizations/organization_form";
    }

    @PostMapping("/save")
    public String saveOrganization(
            @ModelAttribute Organization organization,
            RedirectAttributes redirectAttributes,
            @RequestParam(value = "image", required = false) MultipartFile multipartFile
    ) throws PaykeshoError {

        if (organization.getId() == null) { // New organization case
            organization.setRegistrationDate(new Date());
        } else { // Editing an existing organization
            Organization existingOrganization = organizationService.getOrganizationById(organization.getId());
            if (existingOrganization == null) {
                throw new PaykeshoError("Organization not found");
            }

            // Preserve existing values
            organization.setRegistrationDate(existingOrganization.getRegistrationDate());
            organization.setOrganizationType(existingOrganization.getOrganizationType());

            // If no new image is uploaded, keep the old image
            if (multipartFile == null || multipartFile.isEmpty()) {
                organization.setRemoteFileId(existingOrganization.getRemoteFileId());
                organization.setPhotos(existingOrganization.getPhotos());
                organization.setPhotoPath(existingOrganization.getPhotoPath());
            }
        }

        // Handle new image upload if provided
        if (multipartFile != null && !multipartFile.isEmpty()) {
            FileUploadResponse uploadResponse = fileUploadService.uploadFile(
                    multipartFile, FileType.logo, 1, "logo photo for " + organization.getEmail());

            if (!uploadResponse.isStatus()) {
                throw new PaykeshoError("File upload failed.");
            }

            organization.setRemoteFileId(uploadResponse.getFileId());
            organization.setPhotos(uploadResponse.getFilePath());
            organization.setPhotoPath(uploadResponse.getFilePath());
        }

        // Save the organization
        organizationService.save(organization);

        redirectAttributes.addFlashAttribute(Constants.MESSAGE,
                "Organization Operation Successful: " + organization.getEmail());

        return "redirect:/organizations";
    }


    @GetMapping("/view/{id}")
    public String viewOrganization(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model, HttpServletRequest request) {
        if (id == null) {
            return Constants.REDIRECT_ORGANIZATIONS;
        }
        String pageTitle = "View Organization";

        try {

            Organization organization = organizationService.getOrganizationById(id);
            List<EmployeeRecord> employeeRecords = null;
            List<Client> clientRecordsSize = null;
            OrganizationAdmin organizationAdmin = organization.getContactPerson();
            if (organizationAdmin == null) organizationAdmin = new OrganizationAdmin();

            employeeRecords = organizationService.listEmployeeRecordsAll(organization.getId());
            clientRecordsSize = organizationService.listClientRecordsAll(organization.getId());

            FileAccessResponse fileAccessResponse = fileUploadService.getFileAccessDetails(organization.getRemoteFileId());
            if (fileAccessResponse != null) {
                organization.setPhotoPath(fileAccessResponse.getFileUrl());
            }
            loadCurrencySetting(request);
            model.addAttribute("organization", organization);
            model.addAttribute("pageTitle", pageTitle);
            model.addAttribute("organizationAdmin", organizationAdmin);

            if (employeeRecords != null) {
                model.addAttribute("employeeRecords", employeeRecords);
                model.addAttribute("employeeRecordsSize", employeeRecords.size());
            } else {
                model.addAttribute("employeeRecords", new ArrayList<>());
                model.addAttribute("employeeRecordsSize", 0);
            }
            if (clientRecordsSize != null) {
                model.addAttribute("clientRecords", clientRecordsSize);
                model.addAttribute("clientRecordsSize", clientRecordsSize.size());
            } else {
                model.addAttribute("clientRecords", new ArrayList<>());
                model.addAttribute("clientRecordsSize", 0);
            }

            return "organizations/organization_view";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
            return Constants.REDIRECT_ORGANIZATIONS;
        }
    }


    @GetMapping("/edit/{id}")
    public String editOrganization(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) {
        if (id == null) {
            return Constants.REDIRECT_ORGANIZATIONS;
        }
        String pageTitle = "Edit Organization";

        try {
            Organization organization = organizationService.getOrganizationById(id);

            List<State> stateList = settingService.listStatesByCountry("Zambia");

            model.addAttribute("organization", organization);
            model.addAttribute("stateList", stateList);
            model.addAttribute("pageTitle", pageTitle);

            return "organizations/organization_form";
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
            return Constants.REDIRECT_ORGANIZATIONS;
        }

    }

    @GetMapping("delete/{id}")
    public String deleteOrganization(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {
        if (id == null) {
            throw new PaykeshoError("no parameter id");
        }

        try {
            organizationService.delete(id);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization deleted successfully");
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
        }
        return Constants.REDIRECT_ORGANIZATIONS;
    }

    @GetMapping("/{id}/enabled/{status}")
    public String updateOrganizationEnabledStatus(@PathVariable(name = "id") Integer id, @PathVariable(name = "status") boolean enabled, RedirectAttributes redirectAttributes, Model model) throws PaykeshoError {

        if (id == null) {
            throw new PaykeshoError("no parameter id");
        }

        try {
            Organization organization = organizationService.getOrganizationById(id);
            organizationService.updateOrganizationStatus(id, enabled);
            String status = enabled ? "enabled" : "has been disabled";
            String operationMSG = organization.getName() + " has been " + status;
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, operationMSG);
        } catch (PaykeshoError paykeshoError) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, paykeshoError.getMessage());
        }
        return Constants.REDIRECT_ORGANIZATIONS;

    }

    @GetMapping("/details/{id}")
    public String viewOrganizationDetails(@PathVariable("id") Integer id, Model model) throws PaykeshoError {

        Organization organization = organizationService.getOrganizationById(id);
        model.addAttribute("organization", organization);
        return "organizations/organization_details_modal";

    }


    @GetMapping("/export/{format}")
    public void exportOrganizationsByFormat(@PathVariable(name = "format") String format, HttpServletResponse response) throws PaykeshoError {
        switch (format) {
            case "csv":
                //(response);
                break;
            case "excel":
                //exportToExcel(response);
                break;
            case "pdf":
                //exportToPDF(response);
                break;
            default:
                throw new PaykeshoError("no format provided");
        }
    }

    @PostMapping("/employee-record/save")
    public String saveEmployeeRecord(HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError {

        String organizationIdParam = request.getParameter("organization");
        if (organizationIdParam == null || organizationIdParam.isEmpty()) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization ID is required.");
            return null;
        }

        int organizationId;
        try {
            organizationId = Integer.parseInt(organizationIdParam);
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Invalid Organization ID.");
            return null;
        }

        Organization organization = organizationService.getOrganizationById(organizationId);
        if (organization == null) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization not found.");
            return null;
        }

        EmployeeRecord employeeRecord = new EmployeeRecord();
        employeeRecord.setName(request.getParameter("name"));
        employeeRecord.setNrc(request.getParameter("nrc"));
        employeeRecord.setTpin(request.getParameter("tpin"));
        employeeRecord.setManNumber(request.getParameter("manNumber"));
        employeeRecord.setOrganization(organization);
        employeeRecord.setRegistrationDate(new Date());

        try {
            organizationService.saveEmployeeRecord(employeeRecord);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization Operation Successful: " + organization.getEmail());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Error saving employee record: " + e.getMessage());
            return null;
        }

        return Constants.REDIRECT_ORGANIZATIONS_VIEW + organization.getId();
    }


    private void loadCurrencySetting(HttpServletRequest request) {
        List<Setting> currencySettings = settingService.getCurrencySettings();

        for (Setting setting : currencySettings) {
            request.setAttribute(setting.getKey(), setting.getValue());
        }
    }

    // Employee Record CRUD Operations
    @GetMapping("/{id}/employee-records")
    @ResponseBody
    public ResponseEntity<List<EmployeeRecord>> getEmployeeRecords(@PathVariable Integer id) {
        try {
            List<EmployeeRecord> employeeRecords = organizationService.listEmployeeRecordsAll(id);
            return ResponseEntity.ok(employeeRecords);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/employee-record/update")
    public String updateEmployeeRecord(HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError {
        String idParam = request.getParameter("id");
        String organizationIdParam = request.getParameter("organization");

        if (idParam == null || idParam.isEmpty()) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Employee record ID is required.");
            return "redirect:/organizations";
        }

        if (organizationIdParam == null || organizationIdParam.isEmpty()) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization ID is required.");
            return "redirect:/organizations";
        }

        try {
            int employeeId = Integer.parseInt(idParam);
            int organizationId = Integer.parseInt(organizationIdParam);

            Organization organization = organizationService.getOrganizationById(organizationId);
            if (organization == null) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization not found.");
                return "redirect:/organizations";
            }

            EmployeeRecord employeeRecord = organizationService.getEmployeeRecordById(employeeId);
            if (employeeRecord == null) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Employee record not found.");
                return Constants.REDIRECT_ORGANIZATIONS_VIEW + organizationId;
            }

            employeeRecord.setName(request.getParameter("name"));
            employeeRecord.setNrc(request.getParameter("nrc"));
            employeeRecord.setTpin(request.getParameter("tpin"));
            employeeRecord.setManNumber(request.getParameter("manNumber"));
            employeeRecord.setOrganization(organization);

            organizationService.saveEmployeeRecord(employeeRecord);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Employee record updated successfully");

            return Constants.REDIRECT_ORGANIZATIONS_VIEW + organizationId;
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Invalid ID format.");
            return "redirect:/organizations";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Error updating employee record: " + e.getMessage());
            return "redirect:/organizations";
        }
    }

    @PostMapping("/employee-record/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteEmployeeRecord(@RequestParam Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            organizationService.deleteEmployeeRecord(id);
            response.put("success", true);
            response.put("message", "Employee record deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting employee record: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/employee-record/bulk/save")
    public String saveBulkEmployeeRecords(@RequestParam Integer organization,
                                          @RequestParam MultipartFile file,
                                          RedirectAttributes redirectAttributes) throws PaykeshoError {
        try {
            Organization org = organizationService.getOrganizationById(organization);
            if (org == null) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization not found.");
                return "redirect:/organizations";
            }

            // Validate file
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Please select a CSV file to upload.");
                return Constants.REDIRECT_ORGANIZATIONS_VIEW + organization;
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Please upload a valid CSV file.");
                return Constants.REDIRECT_ORGANIZATIONS_VIEW + organization;
            }

            List<EmployeeRecord> employeeRecords = parseEmployeeRecordsFromCSV(file, org);

            if (employeeRecords.isEmpty()) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "No valid employee records found in the CSV file.");
                return Constants.REDIRECT_ORGANIZATIONS_VIEW + organization;
            }

            // Save employee records
            int savedCount = organizationService.saveBulkEmployeeRecords(employeeRecords);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE,
                    String.format("Successfully uploaded %d employee records out of %d total records.",
                            savedCount, employeeRecords.size()));

            return Constants.REDIRECT_ORGANIZATIONS_VIEW + organization;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Error processing bulk upload: " + e.getMessage());
            return "redirect:/organizations";
        }
    }

    // Client Record CRUD Operations
    @GetMapping("/{id}/client-records")
    @ResponseBody
    public ResponseEntity<List<Client>> getClientRecords(@PathVariable Integer id) {
        try {
            List<Client> clientRecords = organizationService.listClientRecordsAll(id);
            return ResponseEntity.ok(clientRecords);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/client-record/save")
    public String saveClientRecord(HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError {
        String organizationIdParam = request.getParameter("organization");
        if (organizationIdParam == null || organizationIdParam.isEmpty()) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization ID is required.");
            return "redirect:/organizations";
        }

        try {
            int organizationId = Integer.parseInt(organizationIdParam);
            Organization organization = organizationService.getOrganizationById(organizationId);
            if (organization == null) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization not found.");
                return "redirect:/organizations";
            }

            Client client = new Client();
            client.setName(request.getParameter("name"));
            client.setNrc(request.getParameter("nrc"));
            client.setPhoneNumber(request.getParameter("phoneNumber"));
            client.setEmail(request.getParameter("email"));
            client.setOrganization(organization);
            client.setCreatedTime(new Date());
            client.setEnabled(true);

            organizationService.saveClientRecord(client);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Client record saved successfully");

            return Constants.REDIRECT_ORGANIZATIONS_VIEW + organizationId;
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Invalid Organization ID.");
            return "redirect:/organizations";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Error saving client record: " + e.getMessage());
            return "redirect:/organizations";
        }
    }

    @PostMapping("/client-record/update")
    public String updateClientRecord(HttpServletRequest request, RedirectAttributes redirectAttributes) throws PaykeshoError {
        String idParam = request.getParameter("id");
        String organizationIdParam = request.getParameter("organization");

        if (idParam == null || idParam.isEmpty()) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Client record ID is required.");
            return "redirect:/organizations";
        }

        if (organizationIdParam == null || organizationIdParam.isEmpty()) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization ID is required.");
            return "redirect:/organizations";
        }

        try {
            int clientId = Integer.parseInt(idParam);
            int organizationId = Integer.parseInt(organizationIdParam);

            Organization organization = organizationService.getOrganizationById(organizationId);
            if (organization == null) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization not found.");
                return "redirect:/organizations";
            }

            Client client = organizationService.getClientRecordById(clientId);
            if (client == null) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Client record not found.");
                return Constants.REDIRECT_ORGANIZATIONS_VIEW + organizationId;
            }

            client.setName(request.getParameter("name"));
            client.setNrc(request.getParameter("nrc"));
            client.setPhoneNumber(request.getParameter("phoneNumber"));
            client.setEmail(request.getParameter("email"));
            client.setOrganization(organization);

            organizationService.saveClientRecord(client);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Client record updated successfully");

            return Constants.REDIRECT_ORGANIZATIONS_VIEW + organizationId;
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Invalid ID format.");
            return "redirect:/organizations";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Error updating client record: " + e.getMessage());
            return "redirect:/organizations";
        }
    }

    @PostMapping("/client-record/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteClientRecord(@RequestParam Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            organizationService.deleteClientRecord(id);
            response.put("success", true);
            response.put("message", "Client record deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error deleting client record: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/client-record/bulk/save")
    public String saveBulkClientRecords(@RequestParam Integer organization,
                                        @RequestParam MultipartFile file,
                                        RedirectAttributes redirectAttributes) throws PaykeshoError {
        try {
            Organization org = organizationService.getOrganizationById(organization);
            if (org == null) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Organization not found.");
                return "redirect:/organizations";
            }

            // Validate file
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Please select a CSV file to upload.");
                return Constants.REDIRECT_ORGANIZATIONS_VIEW + organization;
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Please upload a valid CSV file.");
                return Constants.REDIRECT_ORGANIZATIONS_VIEW + organization;
            }

            List<Client> clientRecords = parseClientRecordsFromCSV(file, org);

            if (clientRecords.isEmpty()) {
                redirectAttributes.addFlashAttribute(Constants.MESSAGE, "No valid client records found in the CSV file.");
                return Constants.REDIRECT_ORGANIZATIONS_VIEW + organization;
            }

            // Save client records
            int savedCount = organizationService.saveBulkClientRecords(clientRecords);
            redirectAttributes.addFlashAttribute(Constants.MESSAGE,
                    String.format("Successfully uploaded %d client records out of %d total records.",
                            savedCount, clientRecords.size()));

            return Constants.REDIRECT_ORGANIZATIONS_VIEW + organization;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(Constants.MESSAGE, "Error processing bulk upload: " + e.getMessage());
            return "redirect:/organizations";
        }
    }

    /**
     * Parse employee records from CSV file
     * Expected CSV format: name,nrc,tpin,manNumber
     */
    private List<EmployeeRecord> parseEmployeeRecordsFromCSV(MultipartFile file, Organization organization) {
        List<EmployeeRecord> employeeRecords = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip header row
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    String[] fields = line.split(",");
                    if (fields.length < 4) {
                        System.err.println("Skipping line " + lineNumber + ": insufficient fields");
                        continue;
                    }

                    EmployeeRecord employee = new EmployeeRecord();
                    employee.setName(fields[0].trim());
                    employee.setNrc(fields[1].trim());
                    employee.setTpin(fields[2].trim());
                    employee.setManNumber(fields[3].trim());
                    employee.setOrganization(organization);
                    employee.setRegistrationDate(new Date());
                    employeeRecords.add(employee);

                } catch (Exception e) {
                    System.err.println("Error parsing line " + lineNumber + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }

        return employeeRecords;
    }

    /**
     * Parse client records from CSV file
     * Expected CSV format: name,email,phoneNumber,address,nrc,tpin,manNumber
     */
    private List<Client> parseClientRecordsFromCSV(MultipartFile file, Organization organization) {
        List<Client> clientRecords = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip header row
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    String[] fields = line.split(",");
                    if (fields.length < 7) {
                        System.err.println("Skipping line " + lineNumber + ": insufficient fields");
                        continue;
                    }

                    Client client = new Client();
                    client.setName(fields[0].trim());
                    client.setEmail(fields[1].trim());
                    client.setPhoneNumber(fields[2].trim());
                    client.setAddress(fields[3].trim());
                    client.setNrc(fields[4].trim());
                    client.setTpin(fields[5].trim());
                    client.setManNumber(fields[6].trim());
                    client.setOrganization(organization);
                    client.setEnabled(true);
                    // Set a default password (should be changed on first login)
                    client.setPassword("defaultPassword123");
                    clientRecords.add(client);

                } catch (Exception e) {
                    System.err.println("Error parsing line " + lineNumber + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }

        return clientRecords;
    }
}

package org.chaz.backoffice.user.fileHandlers;

import org.chaz.backoffice.utils.AbstractExporter;
import org.membershipcommons.entity.User;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class UserCSVExporter extends AbstractExporter {

    public void export(List<User> userList, HttpServletResponse response) throws IOException {

        super.setResponseHeader(response, "text/csv", ".csv", "users");

        ICsvBeanWriter csvBeanWriter = new CsvBeanWriter(response.getWriter(), CsvPreference.STANDARD_PREFERENCE);

        String[] csvHeader = {"Email", "First Name", "Last Name", "Roles", "Enabled"};
        String[] fieldMapping = {"email", "firstName", "lastName", "roles", "enabled"};

        csvBeanWriter.writeHeader(csvHeader);
        for (User user : userList) {
            csvBeanWriter.write(user, fieldMapping);
        }
        csvBeanWriter.close();
    }
}

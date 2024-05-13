let buttonLoadddd;
let dropdownDepartment;
let btnAddDepartment;
let btnUpdateDepartment;
let btnDeleteDepartment;
let lblDepartmentName;
let fieldDepartmentName;
let lblDepartmentCode;
let fieldDepartmentCode;
let lblHodEmail;
let fieldHodEmail;

$(document).ready(function () {
    buttonLoadddd = $("#buttonLoadDepartments");
    dropdownDepartment = $("#dropDownDepartments");
    btnAddDepartment = $("#btnAddDepartment");
    btnUpdateDepartment = $("#btnUpdateDepartment");
    btnDeleteDepartment = $("#btnDeleteDepartment");
    lblDepartmentName = $("#lblDepartmentName");
    fieldDepartmentName = $("#fieldDepartmentName");
    fieldDepartmentCode = $("#fieldDepartmentCode");
    fieldHodEmail = $("#fieldHodEmail");

    buttonLoadddd.click(function () {
        loadDepartments();
    });

    dropdownDepartment.on("change", function () {
        changeFormDepartmentToSelectedDepartment();
    });

    btnAddDepartment.click(function () {
        if (btnAddDepartment.val() === "Add") {
            addDepartment();
        } else {
            changeFormDepartmentToNewDepartment();
        }
    });

    btnUpdateDepartment.click(function () {
        updateDepartment();
    });

    btnDeleteDepartment.click(function () {
        deleteDepartment();
    });
});

function deleteDepartment() {
    let optionValue = dropdownDepartment.val();
    let departmentId = optionValue.split("-")[0];

    let url = moduleURL + "departments/delete/" + departmentId;

    $.ajax({
        type: 'DELETE',
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        }
    }).done(function () {
        $("#dropDownDepartments option[value='" + optionValue + "']").remove();
        changeFormDepartmentToNewDepartment();
        showToastMessage("The department has been deleted");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function updateDepartment() {
    if (!validateFormDepartment()) return;

    let url = moduleURL + "departments/save";
    let departmentName = fieldDepartmentName.val();
    let departmentCode = fieldDepartmentCode.val();
    let hodEmail = fieldHodEmail.val();

    let departmentId = dropdownDepartment.val().split("-")[0];

    let jsonData = {
        id: departmentId,
        departmentName: departmentName,
        departmentCode: departmentCode,
        hodEmail: hodEmail
    };

    $.ajax({
        type: 'POST',
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        },
        data: JSON.stringify(jsonData),
        contentType: 'application/json'
    }).done(function (departmentId) {
        $("#dropDownDepartments option:selected").val(departmentId + "-" + departmentCode);
        $("#dropDownDepartments option:selected").text(departmentName);
        showToastMessage("The department has been updated");

        changeFormDepartmentToNewDepartment();
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function validateFormDepartment() {
    let formDepartment = document.getElementById("formDepartment");
    if (!formDepartment.checkValidity()) {
        formDepartment.reportValidity();
        return false;
    }

    return true;
}

function addDepartment() {

    if (!validateFormDepartment()) return;

    let url = moduleURL + "departments/save";
    let departmentName = fieldDepartmentName.val();
    let departmentCode = fieldDepartmentCode.val();
    let hodEmail = fieldHodEmail.val();

    let jsonData = {departmentName: departmentName, departmentCode: departmentCode, hodEmail: hodEmail};

    $.ajax({
        type: 'POST',
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        },
        data: JSON.stringify(jsonData),
        contentType: 'application/json'
    }).done(function (departmentId) {
        selectNewlyAddedDepartment(departmentId, departmentCode, departmentName, hodEmail);
        showToastMessage("The new department has been added");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });

}

function selectNewlyAddedDepartment(departmentId, departmentCode, departmentName, hodEmail) {
    let optionValue = departmentId + "-" + departmentCode + "-" + hodEmail;
    $("<option>").val(optionValue).text(departmentName).appendTo(dropdownDepartment);

    $("#dropDownDepartments option[value='" + optionValue + "']").prop("selected", true);

    fieldDepartmentCode.val("");
    fieldDepartmentName.val("").focus();
    fieldHodEmail.val("").focus();
}

function changeFormDepartmentToNewDepartment() {
    btnAddDepartment.text("Add");
    btnAddDepartment.val("Add");
    lblDepartmentName.text("Name:");

    btnUpdateDepartment.prop("disabled", true);
    btnDeleteDepartment.prop("disabled", true);

    fieldDepartmentCode.val("");
    fieldDepartmentName.val("").focus();
    fieldHodEmail.val("");
}

function changeFormDepartmentToSelectedDepartment() {
    btnAddDepartment.text("New");
    btnAddDepartment.val("New");
    btnUpdateDepartment.prop("disabled", false);
    btnDeleteDepartment.prop("disabled", false);

    lblDepartmentName.text("Selected Project:");

    let selectedProjectName = $("#dropDownDepartments option:selected").text();
    fieldDepartmentName.val(selectedProjectName);

    let departmentCode = dropdownDepartment.val().split("-")[1];
    fieldDepartmentCode.val(departmentCode);

    let hodEmail = dropdownDepartment.val().split("-")[2];
    fieldHodEmail.val(hodEmail);

}

function loadDepartments() {
    let url = moduleURL + "departments/list";
    $.get(url, function (responseJSON) {
        dropdownDepartment.empty();

        $.each(responseJSON, function (index, department) {
            let optionValue = department.id + "-" + department.departmentCode + "-" + department.hodEmail;
            $("<option>").val(optionValue).text(department.departmentName).appendTo(dropdownDepartment);
        });

    }).done(function () {
        buttonLoadddd.val("Refresh Department List");
        showToastMessage("All Departments have been loaded");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function showToastMessage(message) {
    $("#toastMessage").text(message);
    $(".toast").toast('show');
}
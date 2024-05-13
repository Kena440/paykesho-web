let buttonLoaddd;
let dropdownProject;
let btnAddProject;
let btnUpdateProject;
let btnDeleteProject;
let lblProjectName;
let fieldProjectName;
let lblProjectCode;
let fieldProjectCode;

$(document).ready(function () {
    buttonLoaddd = $("#buttonLoadProjects");
    dropdownProject = $("#dropDownProjects");
    btnAddProject = $("#btnAddProject");
    btnUpdateProject = $("#btnUpdateProject");
    btnDeleteProject = $("#btnDeleteProject");
    lblProjectName = $("#lblProjectName");
    fieldProjectName = $("#fieldProjectName");
    fieldProjectCode = $("#fieldProjectCode");

    buttonLoaddd.click(function () {
        loadProjects();
    });

    dropdownProject.on("change", function () {
        changeFormProjectToSelectedProject();
    });

    btnAddProject.click(function () {
        if (btnAddProject.val() === "Add") {
            addProject();
        } else {
            changeFormProjectToNewProject();
        }
    });

    btnUpdateProject.click(function () {
        updateProject();
    });

    btnDeleteProject.click(function () {
        deleteProject();
    });
});

function deleteProject() {
    let optionValue = dropdownProject.val();
    let projectId = optionValue.split("-")[0];

    let url = moduleURL + "funding-projects/delete/" + projectId;

    $.ajax({
        type: 'DELETE',
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        }
    }).done(function () {
        $("#dropDownProjects option[value='" + optionValue + "']").remove();
        changeFormProjectToNewProject();
        showToastMessage("The project has been deleted");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function updateProject() {
    if (!validateFormProject()) return;

    let url = moduleURL + "funding-projects/save";
    let projectName = fieldProjectName.val();
    let projectCode = fieldProjectCode.val();

    let projectId = dropdownProject.val().split("-")[0];

    let jsonData = {id: projectId, projectName: projectName, projectCode: projectCode};

    $.ajax({
        type: 'POST',
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        },
        data: JSON.stringify(jsonData),
        contentType: 'application/json'
    }).done(function (projectId) {
        $("#dropDownProjects option:selected").val(projectId + "-" + projectCode);
        $("#dropDownProjects option:selected").text(projectName);
        showToastMessage("The project has been updated");

        changeFormProjectToNewProject();
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function validateFormProject() {
    let formProject = document.getElementById("formProject");
    if (!formProject.checkValidity()) {
        formProject.reportValidity();
        return false;
    }

    return true;
}

function addProject() {

    if (!validateFormProject()) return;

    let url = moduleURL + "funding-projects/save";
    let projectName = fieldProjectName.val();
    let projectCode = fieldProjectCode.val();
    let jsonData = {projectName: projectName, projectCode: projectCode};

    $.ajax({
        type: 'POST',
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        },
        data: JSON.stringify(jsonData),
        contentType: 'application/json'
    }).done(function (projectId) {
        selectNewlyAddedProject(projectId, projectCode, projectName);
        showToastMessage("The new project has been added");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });

}

function selectNewlyAddedProject(projectId, projectCode, projectName) {
    let optionValue = projectId + "-" + projectCode;
    $("<option>").val(optionValue).text(projectName).appendTo(dropdownProject);

    $("#dropDownProjects option[value='" + optionValue + "']").prop("selected", true);

    fieldProjectCode.val("");
    fieldProjectName.val("").focus();
}

function changeFormProjectToNewProject() {
    btnAddProject.text("Add");
    btnAddProject.val("Add");
    lblProjectName.text("Name:");

    btnUpdateProject.prop("disabled", true);
    btnDeleteProject.prop("disabled", true);

    fieldProjectCode.val("");
    fieldProjectName.val("").focus();
}

function changeFormProjectToSelectedProject() {
    btnAddProject.text("New");
    btnAddProject.val("New");
    btnUpdateProject.prop("disabled", false);
    btnDeleteProject.prop("disabled", false);

    lblProjectName.text("Selected Project:");

    let selectedProjectName = $("#dropDownProjects option:selected").text();
    fieldProjectName.val(selectedProjectName);

    let projectCode = dropdownProject.val().split("-")[1];
    fieldProjectCode.val(projectCode);

}

function loadProjects() {
    let url = moduleURL + "funding-projects/list";
    $.get(url, function (responseJSON) {
        dropdownProject.empty();

        $.each(responseJSON, function (index, fundingProject) {
            let optionValue = fundingProject.id + "-" + fundingProject.projectCode;
            $("<option>").val(optionValue).text(fundingProject.projectName).appendTo(dropdownProject);
        });

    }).done(function () {
        buttonLoaddd.val("Refresh Project List");
        showToastMessage("All Projects have been loaded");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function showToastMessage(message) {
    $("#toastMessage").text(message);
    $(".toast").toast('show');
}
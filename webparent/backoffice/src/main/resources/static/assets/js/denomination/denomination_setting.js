let buttonLoadDenominations;
let dropdownDenomination;
let btnAddDenomination;
let btnUpdateDenomination;
let btnDeleteDenomination;
let lblDenominationName;
let fieldDenominationName;

$(document).ready(function () {
    buttonLoadDenominations = $("#buttonLoadDenominations");
    dropdownDenomination = $("#dropDownDenominations");
    btnAddDenomination = $("#btnAddDenomination");
    btnUpdateDenomination = $("#btnUpdateDenomination");
    btnDeleteDenomination = $("#btnDeleteDenomination");
    lblDenominationName = $("#lblDenominationName");
    fieldDenominationName = $("#fieldDenominationName");

    buttonLoadDenominations.click(function () {
        loadDenominations();
    });

    dropdownDenomination.on("change", function () {
        changeFormDenominationToSelectedDenomination();
    });

    btnAddDenomination.click(function () {
        if (btnAddDenomination.val() === "Add") {
            addDenomination();
        } else {
            changeFormDenominationToNewDenomination();
        }
    });

    btnUpdateDenomination.click(function () {
        updateDenomination();
    });

    btnDeleteDenomination.click(function () {
        deleteDenomination();
    });
});

function deleteDenomination() {
    let optionValue = dropdownDenomination.val();
    let denominationId = optionValue.split("-")[0];

    let url = moduleURL + "denominations/delete/" + denominationId;

    $.ajax({
        type: 'DELETE', url: url, beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        }
    }).done(function () {
        $("#dropDownDenominations option[value='" + optionValue + "']").remove();
        changeFormDenominationToNewDenomination();
        showToastMessage("The denomination has been deleted");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function updateDenomination() {
    if (!validateFormDenomination()) return;

    let url = moduleURL + "denominations/save";
    let name = fieldDenominationName.val();

    let denominationId = dropdownDenomination.val().split("-")[0];

    let jsonData = {id: denominationId, name: name};

    $.ajax({
        type: 'POST', url: url, beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        }, data: JSON.stringify(jsonData), contentType: 'application/json'
    }).done(function (denominationId) {
        $("#dropDownDenominations option:selected").val(denominationId);
        $("#dropDownDenominations option:selected").text(name);
        showToastMessage("The denomination has been updated");

        changeFormDenominationToNewDenomination();
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function validateFormDenomination() {
    let formDenomination = document.getElementById("formDenomination");
    if (!formDenomination.checkValidity()) {
        formDenomination.reportValidity();
        return false;
    }

    return true;
}

function addDenomination() {

    if (!validateFormDenomination()) return;

    let url = moduleURL + "denominations/save";
    let name = fieldDenominationName.val();

    let jsonData = {name: name};

    $.ajax({
        type: 'POST', url: url, beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        }, data: JSON.stringify(jsonData), contentType: 'application/json'
    }).done(function (denominationId) {
        selectNewlyAddedDenomination(denominationId, name);
        showToastMessage("The new denomination has been added");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });

}

function selectNewlyAddedDenomination(denominationId, name) {
    let optionValue = denominationId + "-" + name;
    $("<option>").val(optionValue).text(name).appendTo(dropdownDenomination);

    $("#dropDownDenominations option[value='" + optionValue + "']").prop("selected", true);

    fieldDenominationName.val("").focus();
}

function changeFormDenominationToNewDenomination() {
    btnAddDenomination.text("Add");
    btnAddDenomination.val("Add");
    lblDenominationName.text("Name:");

    btnUpdateDenomination.prop("disabled", true);
    btnDeleteDenomination.prop("disabled", true);

    fieldDenominationName.val("").focus();
}

function changeFormDenominationToSelectedDenomination() {
    btnAddDenomination.text("New");
    btnAddDenomination.val("New");
    btnUpdateDenomination.prop("disabled", false);
    btnDeleteDenomination.prop("disabled", false);

    lblDenominationName.text("Selected Project:");

    let selectedDenominationName = $("#dropDownDenominations option:selected").text();
    fieldDenominationName.val(selectedDenominationName);

}

function loadDenominations() {
    let url = moduleURL + "denominations/list";
    $.get(url, function (responseJSON) {
        dropdownDenomination.empty();

        $.each(responseJSON, function (index, denomination) {
            let optionValue = denomination.id;
            $("<option>").val(optionValue).text(denomination.name).appendTo(dropdownDenomination);
        });

    }).done(function () {
        buttonLoadDenominations.val("Refresh Denomination List");
        showToastMessage("All Denominations have been loaded");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function showToastMessage(message) {
    $("#toastMessage").text(message);
    $(".toast").toast('show');
}
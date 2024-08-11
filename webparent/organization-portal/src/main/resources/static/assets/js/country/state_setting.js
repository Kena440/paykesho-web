let buttonLoad4States;
let dropdownCountry4States;
let dropdownStates;
let btnAddState;
let btnUpdateState;
let btnDeleteState;
let lblStateName;
let fieldStateName;

$(document).ready(function () {
    buttonLoad4States = $("#buttonLoadCountriesForStates");
    dropdownCountry4States = $("#dropDownCountriesForStates");
    dropdownStates = $("#dropDownStates");
    btnAddState = $("#btnAddState");
    btnUpdateState = $("#btnUpdateState");
    btnDeleteState = $("#btnDeleteState");

    lblStateName = $("#lblStateName");
    fieldStateName = $("#fieldStateName");

    buttonLoad4States.click(function () {
        loadCountries4States();
    });

    dropdownCountry4States.on("change", function () {
        loadStates4Country();
    });

    dropdownStates.on("change", function () {
        changeFormStateToSelectedState();
    });

    btnAddState.click(function () {
        if (btnAddState.text() === "Add") {
            addState();
        } else {
            changeFormStateToNew();
        }
    });

    btnUpdateState.click(function () {
        updateState();
    });

    btnDeleteState.click(function () {
        deleteState();
    });
});

function deleteState() {
    let stateId = dropdownStates.val();

    let url = moduleURL + "states/delete/" + stateId;

    $.ajax({
        type: 'DELETE',
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        }
    }).done(function () {
        $("#dropDownStates option[value='" + stateId + "']").remove();
        changeFormStateToNew();
        showToastMessage("The state has been deleted");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function updateState() {
    if (!validateFormState()) return;

    let url = moduleURL + "states/save";
    let stateId = dropdownStates.val();
    let stateName = fieldStateName.val();

    let selectedCountry = $("#dropDownCountriesForStates option:selected");
    let countryId = selectedCountry.val();
    let countryName = selectedCountry.text();

    let jsonData = {id: stateId, name: stateName, country: {id: countryId, name: countryName}};

    $.ajax({
        type: 'POST',
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        },
        data: JSON.stringify(jsonData),
        contentType: 'application/json'
    }).done(function (stateId) {
        $("#dropDownStates option:selected").text(stateName);
        showToastMessage("The state has been updated");
        changeFormStateToNew();
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function addState() {
    if (!validateFormState()) return;

    let url = moduleURL + "states/save";
    let stateName = fieldStateName.val();

    let selectedCountry = $("#dropDownCountriesForStates option:selected");
    let countryId = selectedCountry.val();
    let countryName = selectedCountry.text();

    let jsonData = {name: stateName, country: {id: countryId, name: countryName}};

    $.ajax({
        type: 'POST',
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        },
        data: JSON.stringify(jsonData),
        contentType: 'application/json'
    }).done(function (stateId) {
        selectNewlyAddedState(stateId, stateName);
        showToastMessage("The new state has been added");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });

}

function validateFormState() {
    let formState = document.getElementById("formState");
    if (!formState.checkValidity()) {
        formState.reportValidity();
        return false;
    }

    return true;
}

function selectNewlyAddedState(stateId, stateName) {
    $("<option>").val(stateId).text(stateName).appendTo(dropdownStates);

    $("#dropDownStates option[value='" + stateId + "']").prop("selected", true);

    fieldStateName.val("").focus();
}

function changeFormStateToNew() {
    btnAddState.text("Add");
    lblStateName.text("State/Province Name:");

    btnUpdateState.prop("disabled", true);
    btnDeleteState.prop("disabled", true);

    fieldStateName.val("").focus();
}

function changeFormStateToSelectedState() {
    btnAddState.text("New");
    btnUpdateState.prop("disabled", false);
    btnDeleteState.prop("disabled", false);

    lblStateName.text("Selected State/Province:");

    let selectedStateName = $("#dropDownStates option:selected").text();
    fieldStateName.val(selectedStateName);

}


function loadCountries4States() {
    let url = moduleURL + "countries/list";
    $.get(url, function (responseJSON) {
        dropdownCountry4States.empty();

        $.each(responseJSON, function (index, country) {
            $("<option>").val(country.id).text(country.name).appendTo(dropdownCountry4States);
        });
    }).done(function () {
        buttonLoad4States.text("Refresh Country list");
        showToastMessage("All countries have been loaded");
    }).fail(function () {
        showToastMessage("Error loading countries");
    });

}

function loadStates4Country() {
    let selectedCountry = $("#dropDownCountriesForStates option:selected");
    let countryId = selectedCountry.val();
    let url = moduleURL + "states/list-by-country/" + countryId;

    $.get(url, function (responseJSON) {
        dropdownStates.empty();

        $.each(responseJSON, function (index, state) {
            $("<option>").val(state.id).text(state.name).appendTo(dropdownStates);
        });
    }).done(function () {
        showToastMessage("All states have been loaded");
    }).fail(function () {
        showToastMessage("Error loading countries");
    });
}

function showToastMessage(message) {
    $("#toastMessage").text(message);
    $(".toast").toast('show');
}


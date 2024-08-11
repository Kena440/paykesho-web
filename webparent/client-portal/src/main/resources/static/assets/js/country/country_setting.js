let buttonLoad;
let dropdownCountry;
let btnAddCountry;
let btnUpdateCountry;
let btnDeleteCountry;
let lblCountryName;
let fieldCountryName;
let lblCountryCode;
let fieldCountryCode;

$(document).ready(function () {
    buttonLoad = $("#buttonLoadCountries");
    dropdownCountry = $("#dropDownCountries");
    btnAddCountry = $("#btnAddCountry");
    btnUpdateCountry = $("#btnUpdateCountry");
    btnDeleteCountry = $("#btnDeleteCountry");
    lblCountryName = $("#lblCountryName");
    fieldCountryName = $("#fieldCountryName");
    fieldCountryCode = $("#fieldCountryCode");

    buttonLoad.click(function () {
        loadCountries();
    });

    dropdownCountry.on("change", function () {
        changeFormStateToSelectedCountry();
    });

    btnAddCountry.click(function () {
        if (btnAddCountry.val() === "Add") {
            addCountry();
        } else {
            changeFormStateToNewCountry();
        }
    });

    btnUpdateCountry.click(function () {
        updateCountry();
    });

    btnDeleteCountry.click(function () {
        deleteCountry();
    });
});

function deleteCountry() {
    let optionValue = dropdownCountry.val();
    let countryId = optionValue.split("-")[0];

    let url = moduleURL + "countries/delete/" + countryId;

    $.ajax({
        type: 'DELETE',
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        }
    }).done(function () {
        $("#dropDownCountries option[value='" + optionValue + "']").remove();
        changeFormStateToNewCountry();
        showToastMessage("The country has been deleted");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function updateCountry() {
    if (!validateFormCountry()) return;

    let url = moduleURL + "countries/save";
    let countryName = fieldCountryName.val();
    let countryCode = fieldCountryCode.val();

    let countryId = dropdownCountry.val().split("-")[0];

    let jsonData = {id: countryId, name: countryName, code: countryCode};

    $.ajax({
        type: 'POST',
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        },
        data: JSON.stringify(jsonData),
        contentType: 'application/json'
    }).done(function (countryId) {
        $("#dropDownCountries option:selected").val(countryId + "-" + countryCode);
        $("#dropDownCountries option:selected").text(countryName);
        showToastMessage("The country has been updated");

        changeFormStateToNewCountry();
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function validateFormCountry() {
    let formCountry = document.getElementById("formCountry");
    if (!formCountry.checkValidity()) {
        formCountry.reportValidity();
        return false;
    }

    return true;
}

function addCountry() {

    if (!validateFormCountry()) return;

    let url = moduleURL + "countries/save";
    let countryName = fieldCountryName.val();
    let countryCode = fieldCountryCode.val();
    let jsonData = {name: countryName, code: countryCode};

    $.ajax({
        type: 'POST',
        url: url,
        beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        },
        data: JSON.stringify(jsonData),
        contentType: 'application/json'
    }).done(function (countryId) {
        selectNewlyAddedCountry(countryId, countryCode, countryName);
        showToastMessage("The new country has been added");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });

}

function selectNewlyAddedCountry(countryId, countryCode, countryName) {
    let optionValue = countryId + "-" + countryCode;
    $("<option>").val(optionValue).text(countryName).appendTo(dropdownCountry);

    $("#dropDownCountries option[value='" + optionValue + "']").prop("selected", true);

    fieldCountryCode.val("");
    fieldCountryName.val("").focus();
}

function changeFormStateToNewCountry() {
    btnAddCountry.text("Add");
    lblCountryName.text("Country Name:");

    btnUpdateCountry.prop("disabled", true);
    btnDeleteCountry.prop("disabled", true);

    fieldCountryCode.val("");
    fieldCountryName.val("").focus();
}

function changeFormStateToSelectedCountry() {
    btnAddCountry.text("New");
    btnUpdateCountry.prop("disabled", false);
    btnDeleteCountry.prop("disabled", false);

    lblCountryName.text("Selected Country:");

    let selectedCountryName = $("#dropDownCountries option:selected").text();
    fieldCountryName.val(selectedCountryName);

    let countryCode = dropdownCountry.val().split("-")[1];
    fieldCountryCode.val(countryCode);

}

function loadCountries() {
    let url = moduleURL + "countries/list";
    $.get(url, function (responseJSON) {
        dropdownCountry.empty();

        $.each(responseJSON, function (index, country) {
            let optionValue = country.id + "-" + country.code;
            $("<option>").val(optionValue).text(country.name).appendTo(dropdownCountry);
        });

    }).done(function () {
        buttonLoad.val("Refresh Country List");
        showToastMessage("All countries have been loaded");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function showToastMessage(message) {
    $("#toastMessage").text(message);
    $(".toast").toast('show');
}
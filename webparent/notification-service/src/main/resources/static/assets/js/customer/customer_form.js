let dropdownCountry;
let dataListStates;
let fieldState;
$(document).ready(function () {
    dropdownCountry = $("#country");
    dataListStates = $("#listStates");
    fieldState = $("#state");

    dropdownCountry.on("change", function () {
        loadStatesForCountry();
        fieldState.val("").focus();
    });
});

function loadStatesForCountry() {
    selectedCountry = $("#country option:selected");
    countryId = selectedCountry.val();
    url = moduleURL + 'settings/list-states-by-country/' + countryId;

    $.get(url, function (respJSON) {
        dataListStates.empty();

        $.each(respJSON, function (index, state) {
            $("<option>").val(state.name).text(state.name).appendTo(dataListStates);
        });
    }).fail(function () {
        alert('failed to connect to the server!');
    });
}

function checkPasswordMatch(confirmPassword) {
    if (confirmPassword.value !== $("#password").val()) {
        confirmPassword.setCustomValidity("Passwords do not match");
    } else {
        confirmPassword.setCustomValidity("");
    }
}


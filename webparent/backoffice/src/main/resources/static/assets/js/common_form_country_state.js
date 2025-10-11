var dropdownCountries;
var dropdownStates;

$(document).ready(function () {
    dropdownCountries = $("#country");
    dropdownStates = $("#listStates");

    dropdownCountries.on("change", function () {
        loadStates4Country();
        $("#state").val("").focus();
    });

    loadStates4Country();
});

function loadStates4Country() {
    selectedCountry = $("#country option:selected");
    countryId = selectedCountry.val();
    countryUuid = selectedCountry.data('uuid') || selectedCountry.attr('data-uuid');

    // Use UUID if available, otherwise fall back to ID
    var identifier = countryUuid || countryId;
    url = contextPath + "states/list-by-country/" + identifier;

    $.get(url, function (responseJson) {
        dropdownStates.empty();

        $.each(responseJson, function (index, state) {
            $("<option>").val(state.name).text(state.name).appendTo(dropdownStates);
        });
    }).fail(function () {
        showErrorModal("Error loading states/provinces for the selected country.");
    })
}
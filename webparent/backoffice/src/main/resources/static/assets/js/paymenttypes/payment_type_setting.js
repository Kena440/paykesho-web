let buttonLoadPaymentTypes;
let dropdownPaymentType;
let btnAddPaymentType;
let btnUpdatePaymentType;
let btnDeletePaymentType;
let lblPaymentTypeName;
let fieldPaymentTypeName;
let lblPaymentTypeCode;
let fieldPaymentTypeCode;

$(document).ready(function () {
    buttonLoadPaymentTypes = $("#buttonLoadPaymentTypes");
    dropdownPaymentType = $("#dropDownPaymentTypes");
    btnAddPaymentType = $("#btnAddPaymentType");
    btnUpdatePaymentType = $("#btnUpdatePaymentType");
    btnDeletePaymentType = $("#btnDeletePaymentType");
    lblPaymentTypeName = $("#lblPaymentTypeName");
    fieldPaymentTypeName = $("#fieldPaymentTypeName");
    lblPaymentTypeCode = $("#lblPaymentTypeCode");
    fieldPaymentTypeCode = $("#fieldPaymentTypeCode");

    buttonLoadPaymentTypes.click(function () {
        loadPaymentTypes();
    });

    dropdownPaymentType.on("change", function () {
        changeFormPaymentTypeToSelectedPaymentType();
    });

    btnAddPaymentType.click(function () {
        if (btnAddPaymentType.val() === "Add") {
            addPaymentType();
        } else {
            changeFormPaymentTypeToNewPaymentType();
        }
    });

    btnUpdatePaymentType.click(function () {
        updatePaymentType();
    });

    btnDeletePaymentType.click(function () {
        deletePaymentType();
    });
});

function deletePaymentType() {
    let optionValue = dropdownPaymentType.val();
    let paymentTypeId = optionValue.split("-")[0];

    let url = moduleURL + "payment-types/delete/" + paymentTypeId;

    $.ajax({
        type: 'DELETE', url: url, beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        }
    }).done(function () {
        $("#dropDownPaymentTypes option[value='" + optionValue + "']").remove();
        changeFormPaymentTypeToNewPaymentType();
        showToastMessage("The paymentType has been deleted");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function updatePaymentType() {
    if (!validateFormPaymentType()) return;

    let url = moduleURL + "payment-types/save";
    let name = fieldPaymentTypeName.val();
    let code = fieldPaymentTypeCode.val();

    let paymentTypeId = dropdownPaymentType.val().split("-")[0];

    let jsonData = {id: paymentTypeId, name: name, code: code};

    $.ajax({
        type: 'POST', url: url, beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        }, data: JSON.stringify(jsonData), contentType: 'application/json'
    }).done(function (paymentTypeId) {
        $("#dropDownPaymentTypes option:selected").val(paymentTypeId + "-" + code);
        $("#dropDownPaymentTypes option:selected").text(name);
        showToastMessage("The payment type has been updated");

        changeFormPaymentTypeToNewPaymentType();
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function validateFormPaymentType() {
    let formPaymentType = document.getElementById("formPaymentType");
    if (!formPaymentType.checkValidity()) {
        formPaymentType.reportValidity();
        return false;
    }

    return true;
}

function addPaymentType() {

    if (!validateFormPaymentType()) return;

    let url = moduleURL + "payment-types/save";
    let name = fieldPaymentTypeName.val();
    let code = fieldPaymentTypeCode.val();

    let jsonData = {name: name, code: code};

    $.ajax({
        type: 'POST', url: url, beforeSend: function (xhr) {
            xhr.setRequestHeader(csrfHeaderName, csrfValue);
        }, data: JSON.stringify(jsonData), contentType: 'application/json'
    }).done(function (paymentTypeId) {
        selectNewlyAddedPaymentType(paymentTypeId, name);
        showToastMessage("The new payment type has been added");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });

}

function selectNewlyAddedPaymentType(paymentTypeId, name) {
    let optionValue = paymentTypeId + "-" + name;
    $("<option>").val(optionValue).text(name).appendTo(dropdownPaymentType);

    $("#dropDownPaymentTypes option[value='" + optionValue + "']").prop("selected", true);

    fieldPaymentTypeName.val("").focus();
    fieldPaymentTypeCode.val("");
}

function changeFormPaymentTypeToNewPaymentType() {
    btnAddPaymentType.text("Add");
    btnAddPaymentType.val("Add");
    lblPaymentTypeName.text("Name:");
    lblPaymentTypeCode.text("Code:");

    btnUpdatePaymentType.prop("disabled", true);
    btnDeletePaymentType.prop("disabled", true);

    fieldPaymentTypeCode.val("");
    fieldPaymentTypeName.val("").focus();
}

function changeFormPaymentTypeToSelectedPaymentType() {
    btnAddPaymentType.text("New");
    btnAddPaymentType.val("New");
    btnUpdatePaymentType.prop("disabled", false);
    btnDeletePaymentType.prop("disabled", false);

    lblPaymentTypeName.text("Selected Type:");

    let selectedPaymentTypeName = $("#dropDownPaymentTypes option:selected").text();
    fieldPaymentTypeName.val(selectedPaymentTypeName);

    let paymentTypeCode = dropdownPaymentType.val().split("-")[1];
    fieldPaymentTypeCode.val(paymentTypeCode);

}

function loadPaymentTypes() {
    let url = moduleURL + "payment-types/list";
    $.get(url, function (responseJSON) {
        dropdownPaymentType.empty();

        $.each(responseJSON, function (index, paymentType) {
            let optionValue = paymentType.id + "-" + paymentType.code;
            $("<option>").val(optionValue).text(paymentType.name).appendTo(dropdownPaymentType);
        });

    }).done(function () {
        buttonLoadPaymentTypes.val("Refresh Payment Type List");
        showToastMessage("All Payment Types have been loaded");
    }).fail(function () {
        showToastMessage("ERROR: Could not connect to server or server encountered an error");
    });
}

function showToastMessage(message) {
    $("#toastMessage").text(message);
    $(".toast").toast('show');
}
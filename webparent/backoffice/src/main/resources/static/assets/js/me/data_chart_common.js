var MILLISECONDS_A_DAY = 31540000000;

function setupButtonEventHandlers(reportType, callBackFunction) {
    $(".btn-data-by" + reportType).on("click", function () {
        $(".btn-data-by" + reportType).each(function (e) {
            $(this).removeClass('btn-primary').addClass('btn-light');
        });
        $(this).removeClass('btn-light').addClass('btn-primary');
        let period = $(this).attr("period");
        if (period) {
            callBackFunction(period);
            $("#divCustomYearRange" + reportType).addClass("d-none");
        } else {
            $("#divCustomYearRange" + reportType).removeClass("d-none");
        }
    });

    initCustomDateRange(reportType);

    $("#viewReportByYearRange" + reportType).on("click", function (e) {
        validateDateRange(reportType, callBackFunction);
    });
}

function validateDateRange(reportType, callBackFunction) {

    startYearField = document.getElementById('startYear' + reportType);
    endYearField = document.getElementById('endYear' + reportType);

    let days = calculateYears(reportType);
    startYearField.setCustomValidity("");

    if (days >= 7 && days <= 30) {
        callBackFunction("custom");
    } else {
        startYearField.setCustomValidity("Dates must be in the range of 7 to 30 days");
        startYearField.reportValidity();
    }
}

function calculateYears(reportType) {

    startYearField = document.getElementById('startYear' + reportType);
    endYearField = document.getElementById('endYear' + reportType);

    startYear = startYearField.valueAsDate;
    endYear = endYearField.valueAsDate;

    differenceInMilliseconds = endYear - startYear;
    return differenceInMilliseconds / MILLISECONDS_A_DAY;
}

function initCustomDateRange(reportType) {

    startYearField = document.getElementById('startYear' + reportType);
    endYearField = document.getElementById('endYear' + reportType);

    toDate = new Date().getFullYear();
    endYearField = toDate;
    fromDate = new Date().getFullYear();
    startYearField = fromDate;
}

function formatCurrency(amount, reportType) {
    formattedAmount = $.number(amount, decimalDigits, decimalPointType, thousandPointType);
    return formattedAmount;
}

function getChartTitle(period, reportType) {
    if (period === "current-year") return "Data in the current year";
    if (period === "last-year") return "Data in the last year";
    if (period === "last-3-years") return "Data in the last 3 years";
    if (period === "last-5-years") return "Data in the last 5 years";
    if (period === "last-10-years") return "Data in the last 10 years";
    if (period === "custom") return "Data in custom year range";

    return "Data in the current year";
}

function getDenominator(period, reportType) {
    if (period === "current-year") return 1;
    if (period === "last-year") return 2;
    if (period === "last-3-years") return 3;
    if (period === "last-5-years") return 5;
    if (period === "last-10-years") return 10;
    if (period === "custom") return calculateYears(reportType);

    return 7;
}

function setDataAmount(period, reportType, labelTotal) {
    denominator = getDenominator(period, reportType);
    $("#totalItems" + reportType).text((totalItems));
    $("#labelTotalItems" + reportType).text((labelTotal));
}

function formatChartData(data, columnIndex1) {
    const formatter = new google.visualization.NumberFormat({
        prefix: prefixCurrencySymbol,
        suffix: suffixCurrencySymbol,
        decimalSymbol: decimalPointType,
        groupingSymbol: thousandPointType,
        factionDigits: decimalDigits
    });

    formatter.format(data, columnIndex1);
}
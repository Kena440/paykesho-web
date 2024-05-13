let data;
let chartOptions;
var totalGrossData;
var totalNetData;
var totalItems;

$(document).ready(function () {
    setupButtonEventHandlers("-year", loadDataReportByYear);
});

function loadDataReportByYear(period) {

    var requestUrl;

    if (period === "custom") {
        startYear = $("#startYear-year").val();
        endYear = $("#endYear-year").val();
        requestUrl = contextPath + "me-indicators/rest/data-by-year/" + startYear + "/" + endYear + "/" + $("#varName").val();
    } else {
        requestUrl = contextPath + "me-indicators/rest/data-by-year/" + period + "/" + $("#varName").val();
    }

    $.get(requestUrl, function (responseJSON) {
        prepareChartDataForDataReportByYear(responseJSON);
        customizeChartOptionsForDataReportByYear(period);
        formatChartData(data, 1)
        drawChartForDataReportByYear(period);
        setDataAmount(period, "-year", "Total Items")
    });
}

function prepareChartDataForDataReportByYear(responseJSON) {

    data = new google.visualization.DataTable();
    data.addColumn('string', 'Year');
    data.addColumn('number', 'Data');

    totalItems = 0;

    $.each(responseJSON, function (index, reportItem) {
        data.addRows([[reportItem.identifier, reportItem.totalCount]]);
        totalItems += parseInt(reportItem.totalCount);
    });
}

function customizeChartOptionsForDataReportByYear(period) {
    chartOptions = {
        title: getChartTitle(period),
        'height': 360,
        legend: {position: 'top'},
        series: {
            0: {targetAxisIndex: 0},
            1: {targetAxisIndex: 0},
            2: {targetAxisIndex: 1}
        },
        vAxes: {
            0: {title: 'Count', format: 'decimal'},
            1: {title: 'Number'},
        }
    };

}

function drawChartForDataReportByYear() {
    salesChart = new google.visualization.ColumnChart(document.getElementById('chart-data-by-year'));
    salesChart.draw(data, chartOptions);
}

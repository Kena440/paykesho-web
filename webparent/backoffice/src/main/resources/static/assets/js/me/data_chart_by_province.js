let provinceData;
let provinceChartOptions;

$(document).ready(function () {
    setupButtonEventHandlers("-province", loadDataReportByYearForProvince);
});

function loadDataReportByYearForProvince(period) {

    var requestUrl;

    if (period === "custom") {
        startYear = $("#startDate-province").val();
        endDate = $("#endDate-province").val();
        requestUrl = contextPath + "me-indicators/rest/province/" + startYear + "/" + endDate + "/" + $("#varName").val();
    } else {
        requestUrl = contextPath + "me-indicators/rest/province/" + period + "/" + $("#varName").val();
    }

    $.get(requestUrl, function (responseJSON) {
        prepareChartDataForReportByProvince(responseJSON);
        customizeChartOptionsForDataReportByProvince();
        formatChartData(provinceData, 1)
        drawChartForDataReportByProvince(period);
        setDataAmount(period, "-province", "Total Items")

    });
}

function prepareChartDataForReportByProvince(responseJSON) {

    provinceData = new google.visualization.DataTable();
    provinceData.addColumn('string', 'Province');
    provinceData.addColumn('number', 'Data');

    totalItems = 0;

    $.each(responseJSON, function (index, reportItem) {
        provinceData.addRows([[reportItem.identifier, reportItem.totalCount]]);
        totalItems += parseInt(reportItem.totalCount);
    });
}

function customizeChartOptionsForDataReportByProvince() {
    provinceChartOptions = {
        height: 360,
        legend: {
            position: 'right'
        }
    };

}

function drawChartForDataReportByProvince() {
    salesChart = new google.visualization.PieChart(document.getElementById('chart-data-by-province'));
    salesChart.draw(provinceData, provinceChartOptions);
}

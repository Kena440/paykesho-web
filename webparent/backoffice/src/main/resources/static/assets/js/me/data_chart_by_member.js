let brandData;
let brandChartOptions;

$(document).ready(function () {
    setupButtonEventHandlers("-member", loadDataReportByYearForMember);
});

function loadDataReportByYearForMember(period) {

    var requestUrl;

    if (period === "custom") {
        startYear = $("#startYear-member").val();
        endYear = $("#endYear-member").val();
        requestUrl = contextPath + "me-indicators/rest/member/" + startYear + "/" + endYear + "/" + $("#varName").val();
    } else {
        requestUrl = contextPath + "me-indicators/rest/member/" + period + "/" + $("#varName").val();
    }

    $.get(requestUrl, function (responseJSON) {
        prepareChartDataForDataReportByMember(responseJSON);
        customizeChartOptionsForDataReportByMember();
        formatChartData(brandData, 1)
        drawChartForDataReportByMember(period);
        setDataAmount(period, "-member", "Total Items")
    });
}

function prepareChartDataForDataReportByMember(responseJSON) {

    brandData = new google.visualization.DataTable();
    brandData.addColumn('string', 'Member');
    brandData.addColumn('number', 'Data');

    totalItems = 0;

    $.each(responseJSON, function (index, reportItem) {
        brandData.addRows([[reportItem.identifier, reportItem.totalCount]]);
        totalItems += parseInt(reportItem.totalCount);
    });
}

function customizeChartOptionsForDataReportByMember() {
    brandChartOptions = {
        height: 360,
        legend: {
            position: 'right'
        }
    };

}

function drawChartForDataReportByMember() {
    salesChart = new google.visualization.PieChart(document.getElementById('chart-data-by-member'));
    salesChart.draw(brandData, brandChartOptions);
}

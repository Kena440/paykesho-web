$(document).ready(function () {

    $("button[name='linkRemoveDetail']").each(function (index) {
        $(this).click(function () {
            removeDetailSectionByIndex(index);
        });
    });
});

function removeDetailSectionByIndex(index) {
    $("#divDetail" + index).remove();
}

function addNextDetailSection() {
    let allDivDetails = $("[id^='divDetail']");
    let divDetailsCount = allDivDetails.length;
    let htmlDetailSection = `
         <div class="row" id="divDetail${divDetailsCount}">
            <input type="hidden" name="detailIDs" value="0">
                   <div class="col-12 col-md-3">
            <div class="form-group">
                <label class="form-label">
                    Period
                </label>
                <input type="date" class="form-control" name="period">
            </div>
        </div>
        <div class="col-12 col-md-3">
            <div class="form-group">
                <label class="form-label">
                    Principal Paid
                </label>
                <input type="number" class="form-control" name="principalPaid">
            </div>
        </div>
        <div class="col-12 col-md-3">
            <div class="form-group">
                <label class="form-label">
                    Interest Paid
                </label>
                <input type="number" class="form-control" name="interestPaid">
            </div>
        </div>
        </div>
    `;

    $("#divLoanItemDetails").append(htmlDetailSection);

    let previousDivDetailSection = allDivDetails.last();
    let previousDivDetailId = previousDivDetailSection.attr("id");
    let htmlLinkRemove = `<div class="col-12 col-md-3">
                <div class="form-group">
                     <label class="form-label">
                        Action
                    </label>
                    <br>
                <a class="btn btn-danger btn-sm float-right"
                         href="javascript:removeDetailSectionById('${previousDivDetailId}')"
                         title="Remove item"> 
                          <i class="fe fe-delete"></i>
                            Remove
                      </a></div></div>`;

    previousDivDetailSection.append(htmlLinkRemove);
    $("select[name = 'period']").last().focus();
}

function removeDetailSectionById(id) {
    $("#" + id).remove();
}
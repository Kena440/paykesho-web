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
            Type
        </label>
        <select class="form-select mb-3" data-choices name="paymentType">
          <option value="">Select...</option>
          <option value="14" style="font-weight: normal;
    display: block;
    white-space: nowrap;
    min-height: 1.2em;
    padding: 0px 2px 1px;">Genset Fuel</option>
        </select>
                </div>
               </div>
                      <div class="col-12 col-md-3">
                <div class="form-group">
                       <label class="form-label">
            Description
        </label>
        <input type="text" class="form-control" name="description">
                </div>
               </div>
                      <div class="col-12 col-md-3">
                <div class="form-group">
                 <label class="form-label">
            Amount
        </label>
        <input type="number" class="form-control" name="amount" step="0.01"
                           min="0">
                </div>
               </div>
      
        </div>
    `;

    $("#divInvoiceItemDetails").append(htmlDetailSection);

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
    $("select[name = 'paymentType']").last().focus();
}

function removeDetailSectionById(id) {
    $("#" + id).remove();
}
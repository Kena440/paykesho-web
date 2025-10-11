$(document).ready(function () {
    $("#btnCancel").on("click", function () {
        window.location = moduleURL;
    })

    $("#fileImage").change(function () {
        if (!checkFileSize(this)) {
            return;
        } else {
            // Use the new previewImage function if it exists, otherwise fall back to showImageThumbnail
            if (typeof previewImage === 'function') {
                previewImage(this);
            } else {
                showImageThumbnail(this);
            }
        }
    })
});

function showImageThumbnail(fileInput) {
    var file = fileInput.files[0];
    var reader = new FileReader();
    reader.onload = function (e) {
        $("#thumbnail").attr("src", e.target.result);
        $("#fileImageName").text(file.name)
    }
    reader.readAsDataURL(file);
}

function checkFileSize(fileInput) {
    fileSize = fileInput.files[0].size;
    if (fileSize > MAX_FILE_SIZE) {
        fileInput.setCustomValidity("Image size greater than 500KB!");
        //this.reportValidity();
        showModalDialog("Upload Error", "Kindly upload an image less than 500KB")
        return false
    } else {
        fileInput.setCustomValidity("");
        return true;
    }
}

function showModalDialog(title, message) {
    $("#modalTitle").text(title);
    $("#modalBody").text(message);
    $("#modalDialog").modal("show");
}

function showErrorModal(message) {
    showModalDialog("Error", message);
}

function showWarningModal(message) {
    showModalDialog("Warning", message);
}
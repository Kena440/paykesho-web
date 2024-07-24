$(document).ready(function () {

    $("#download").on('click', function () {

        html2canvas(document.getElementById("print-section"), {

            allowTaint: true,

            useCORS: true

        }).then(function (canvas) {

            var anchorTag = document.createElement("a");

            // document.body.appendChild(anchorTag);

            // document.getElementById("previewImg").appendChild(canvas);

            let image_name = $('#fileSerial').val() + ".jpg";

            anchorTag.download = image_name.replace(/\s/g, '');

            anchorTag.href = canvas.toDataURL();

            anchorTag.target = '_blank';

            anchorTag.click();

        });

    });


    $("#print").on('click', function () {

        event.preventDefault();

        var element = document.getElementById("print-section");

        html2canvas(element).then(function (canvas) {

            var myImage = canvas.toDataURL("image/jpg");

            var tmp = window.open("");

            $(tmp.document.body)

                .html("<img src=" + myImage + " alt=''>")

                .ready(function () {

                    setTimeout(function () {

                        tmp.focus();

                        tmp.print();

                    }, 200);

                })

        })

    });

    /*
        $("#download").on('click', function () {
            html2canvas(document.getElementById("print-section"), {

                allowTaint: true,

                useCORS: true

            }).then(function (canvas) {
                // Convert the captured image into a data URL
                var imgData = canvas.toDataURL('image/png');

                // Initialize jsPDF and set document properties
                var doc = new jsPDF('p', 'mm');
                doc.setProperties({
                    title: 'My Document',
                    author: 'John Doe',
                });

                // Add the captured image to the PDF document
                doc.addImage(imgData, 'PNG', 10, 10, 100, 100);

                // Save the PDF document
                doc.save('my-document.pdf');
            });
        });
    */
});

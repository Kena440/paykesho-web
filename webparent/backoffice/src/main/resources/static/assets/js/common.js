$(document).ready(function () {
    $("#logoutLink").on("click", function (e) {
        e.preventDefault();
        document.logoutForm.submit();
    });

    $("#logoutLink1").on("click", function (e) {
        e.preventDefault();
        document.logoutForm1.submit();
    });


    $("#accountLink").on("click", function (e) {
        e.preventDefault();
        location.href = this.href;
    })

    $("#accountLink1").on("click", function (e) {
        e.preventDefault();
        location.href = this.href;
    })


});

function customizeDropDownMenu() {

    $(".navbar .dropdown").hover(
        function () {
            $(this).find('.dropdown-menu').first().stop(true, true).delay(250).slideDown();
        },
        function () {
            $(this).find('.dropdown-menu').first().stop(true, true).delay(100).slideUp();
        }
    )
}

function customizeTabs() {
    // Javascript to enable link to tab
    const url = document.location.toString();
    if (url.match('#')) {
        $('.nav-tabs a[href="#' + url.split('#')[1] + '"]').tab('show');
    }

    // Change hash for page-reload
    $('.nav-tabs a').on('shown.bs.tab', function (e) {
        window.location.hash = e.target.hash;
    })
}
$(document).ready(function () {
    $("#logoutLink").on("click", function (e) {
        e.preventDefault();
        document.logoutForm.submit();
    });

    $("#logoutLink1").on("click", function (e) {
        e.preventDefault();
        document.logoutForm1.submit();
    });

    $("#logoutLink2").on("click", function (e) {
        e.preventDefault();
        document.logoutForm2.submit();
    });

    $("#logoutLink3").on("click", function (e) {
        e.preventDefault();
        document.logoutForm3.submit();
    });

    $("#logoutLink4").on("click", function (e) {
        e.preventDefault();
        document.logoutForm4.submit();
    });

    $("#accountLink").on("click", function (e) {
        e.preventDefault();
        location.href = this.href;
    })

    $("#accountLink1").on("click", function (e) {
        e.preventDefault();
        location.href = this.href;
    })

    $("#accountLink2").on("click", function (e) {
        e.preventDefault();
        location.href = this.href;
    })

    $("#accountLink3").on("click", function (e) {
        e.preventDefault();
        location.href = this.href;
    })

    $("#accountLink4").on("click", function (e) {
        e.preventDefault();
        location.href = this.href;
    })

});

function customizeDropDownMenu() {

    $(".navbar .dropdown").hover(function () {
        $(this).find('.dropdown-menu').first().stop(true, true).delay(250).slideDown();
    }, function () {
        $(this).find('.dropdown-menu').first().stop(true, true).delay(100).slideUp();
    })
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
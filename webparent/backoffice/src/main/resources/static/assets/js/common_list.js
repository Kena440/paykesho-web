function clearFilter() {
    if (typeof moduleURL !== 'undefined' && moduleURL) {
        window.location = moduleURL;
    } else {
        console.error('moduleURL is not defined');
        window.location.reload();
    }
}

function showDeleteConfirmModal(link, entityName) {
    try {
        if (!link || typeof link.attr !== 'function') {
            console.error('Invalid link object passed to showDeleteConfirmModal');
            return;
        }

        const entityId = link.attr("entityId");
        const href = link.attr("href");

        if (!href) {
            console.error('No href attribute found on delete link');
            return;
        }

        const entityDisplayName = entityName || 'item';

        $("#yesButton").attr("href", href);
        $("#confirmText").text(`Are you sure you want to delete this ${entityDisplayName}?`);
        $("#confirmModal").modal("show");

    } catch (error) {
        console.error('Error in showDeleteConfirmModal:', error);
        if (confirm(`Are you sure you want to delete this ${entityName || 'item'}?`)) {
            window.location = link.attr("href");
        }
    }
}



// open respond modal
function openRespondModal(btn) {
    const ticketId = btn.dataset.ticketId;
    const subject = btn.dataset.ticketSubject;
    document.getElementById('respondModalTitle').innerHTML = 'Phản hồi Ticket #' + ticketId;
    document.getElementById('respondTicketId').value = ticketId;
    document.getElementById('respondTicketSubject').textContent = subject;
    document.getElementById('respondModal').classList.add('show');
}


// check file uploaded of form respond
document.addEventListener('DOMContentLoaded', function () {
    const respondForm = document.querySelector('#respondModal form');
    respondForm.addEventListener('submit', function (event) {

        const fileInput = respondForm.querySelector('input[name="attachment"]');
        if (fileInput.files.length === 0) {
            return;
        }
        const file = fileInput.files[0];

        const ALLOWED_TYPES = [
            "image/jpeg",
            "image/png",
            "image/gif",
            "video/mp4",
            "video/quicktime", // for .mov files
            "application/msword", // for .doc
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // for .docx
        ];

        const MAX_SIZE_BYTES = 52428800;

        if (!ALLOWED_TYPES.includes(file.type)) {
            alert('Lỗi: Loại file không được hỗ trợ.\nVui lòng chỉ tải lên file ảnh(jpeg, png), video (mp4, mov) hoặc tài liệu Word (doc, docx).');
            event.preventDefault();
            return;
        }

        if (file.size > MAX_SIZE_BYTES) {
            alert('Lỗi: Dung lượng file không được vượt quá 50MB.');
            event.preventDefault();
        }
    });
});


// view detail response
function viewDetailResponse(btn) {
    const ticketId = btn.dataset.ticketId;
    const ticketSubject = btn.dataset.ticketSubject;
    const resId = btn.dataset.resId;
    const name = btn.dataset.name;
    const createdAt = btn.dataset.createdAt;
    const massageRes = btn.dataset.messagse;
    const attachmentUrl = btn.dataset.attachmentUrl;

    document.getElementById("titleRes").innerHTML =
        `<i class="fa-solid fa-reply"></i> Chi Tiết Phản Hồi R${resId}`;
    document.getElementById("idAndTileTicket").innerHTML = "#" + ticketId + " - " + ticketSubject;
    document.getElementById("nameResponder").innerHTML = name + "<br><small>ADMIN</small>";

    const formattedDate = new Date(createdAt).toLocaleString('vi-VN');
    document.getElementById("dateRes").textContent = formattedDate;

    document.getElementById("massageRes").textContent = massageRes;

    const attachmentEl = document.getElementById("attachmentRes");

    if (attachmentUrl && attachmentUrl.trim() !== "") {
        const fileName = attachmentUrl.split("/").pop();
        attachmentEl.innerHTML = `<a href="${attachmentUrl}" target="_blank"><i class="fa-regular fa-file"></i> ${fileName}</a>`;
    } else {
        attachmentEl.textContent = "Không có tệp đính kèm";
    }
    document.getElementById("responseDetailModal").classList.add("show");
}

// view detail support ticket
function viewDetailSupportTicket(btn) {
    const id = btn.dataset.ticketId;
    const subject = btn.dataset.subject;
    const priority = btn.dataset.priority;
    const status = btn.dataset.status;
    const createdAt = btn.dataset.createdAt;
    const description = btn.dataset.description;
    const attachmentUrl = btn.dataset.attachmentUrl;
    const contactEmail = btn.dataset.contactEmail;

    const statusBadgeMap = {
        OPEN: '<span class="badge badge-open">Đang mở</span>',
        IN_PROGRESS: '<span class="badge badge-processing">Đang xử lý</span>',
        CLOSED: '<span class="badge badge-close">Đã giải quyết</span>'
    };

    const priorityBadgeMap = {
        LOW: '<span class="badge badge-low">Thấp</span>',
        NORMAL: '<span class="badge badge-medium">Trung bình</span>',
        HIGH: '<span class="badge badge-high">Cao</span>'
    };

    document.getElementById("titleSP").innerHTML =
        `<i class="fa-solid fa-ticket"></i> Chi Tiết Ticket #${id}`;
    document.getElementById("idSP").textContent = id;
    document.getElementById("subjectSP").textContent = subject;

    const statusContainer = document.getElementById("statusSP");
    statusContainer.innerHTML = statusBadgeMap[status] ?? status;

    const priorityContainer = document.getElementById("prioritySP");
    priorityContainer.innerHTML = priorityBadgeMap[priority] ?? priority;

    document.getElementById("gmailSP").textContent = contactEmail;

    const formattedDate = new Date(createdAt).toLocaleString('vi-VN');
    document.getElementById("dateSP").textContent = formattedDate;
    document.getElementById("desSP").textContent = description;

    const attachmentEl = document.getElementById("attachmentSP");
    if (attachmentUrl && attachmentUrl.trim() !== "") {
        const fileName = attachmentUrl.split("/").pop();
        attachmentEl.innerHTML = `<a href="${attachmentUrl}" target="_blank"><i class="fa-regular fa-file"></i> ${fileName}</a>`;
    } else {
        attachmentEl.textContent = "Không có tệp đính kèm";
    }
    document.getElementById("ticketDetailModal").classList.add("show");
}


function openMarkResolvedModal(button) {
    const ticketId = button.getAttribute('data-ticket-id');
    document.getElementById('markResolvedTicketId').value = ticketId;
    document.getElementById('markResolvedModal').classList.add('show');
}



function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

window.onclick = function(event) {
    if (event.target.classList.contains('modal')) {
        event.target.classList.remove('show');
    }
}

// header profile
document.addEventListener('DOMContentLoaded', function () {
    const profileDropdown = document.getElementById('profileDropdown');
    const dropdownMenu = document.getElementById('dropdownMenu');

    if (profileDropdown) {
        profileDropdown.addEventListener('click', function (event) {
            event.stopPropagation(); // Ngăn sự kiện click lan ra ngoài
            dropdownMenu.classList.toggle('show');
        });
    }

    // Đóng dropdown khi click ra ngoài
    window.addEventListener('click', function (event) {
        if (dropdownMenu && dropdownMenu.classList.contains('show')) {
            dropdownMenu.classList.remove('show');
        }
    });
});

// sidebar profile
document.addEventListener('DOMContentLoaded', function () {
    const sidebarProfile = document.getElementById('sidebarProfile');
    const sidebarDropdownMenu = document.getElementById('sidebarDropdownMenu');

    if (sidebarProfile) {
        sidebarProfile.addEventListener('click', function (event) {
            event.stopPropagation();
            sidebarDropdownMenu.classList.toggle('show');
        });
    }

    // Đóng dropdown khi click ra ngoài
    window.addEventListener('click', function (event) {
        if (sidebarDropdownMenu && sidebarDropdownMenu.classList.contains('show')) {
            sidebarDropdownMenu.classList.remove('show');
        }
    });
});




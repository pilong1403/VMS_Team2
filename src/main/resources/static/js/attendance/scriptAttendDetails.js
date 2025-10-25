// Modal functions
function openModal() {
    const modalOverlay = document.getElementById("modalOverlay");
    modalOverlay.classList.add("active");
    document.body.style.overflow = "hidden";
}

function closeModal() {
    const modalOverlay = document.getElementById("modalOverlay");
    modalOverlay.classList.remove("active");
    document.body.style.overflow = "auto";
}

// Close modal when clicking outside
document
    .getElementById("modalOverlay")
    .addEventListener("click", function (event) {
        if (event.target === this) {
            closeModal();
        }
    });

//======================================================================
// CHECK IN modal functions
function openCheckinModal(button) {
    const applicationId = button.dataset.applicationId;
    const volunteerName = button.dataset.name;
    const avatarUrl = button.dataset.avatar;
    const oppId = button.dataset.oppId;
    const eventStartTimeString = button.dataset.eventStartTime;
    const eventEndTimeString = button.dataset.eventEndTime;
    const checkoutTimeString = button.dataset.checkoutTime;

    const modalAvatar = document.getElementById('modalAvatar');
    const modalVolunteerName = document.getElementById('modalVolunteerName');
    const modalTimeInput = document.getElementById('modalTimeInput');
    const applicationIdInput = document.getElementById('applicationIdInput');
    const oppIdInput = document.getElementById('oppId');

    applicationIdInput.value = applicationId;
    modalAvatar.src = avatarUrl;
    modalVolunteerName.textContent = volunteerName;
    oppIdInput.value = oppId;

    const pad = (num) => num.toString().padStart(2, '0');
    const formatToLocalString = (date) => {
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    };

    if (eventStartTimeString && eventEndTimeString) {
        const eventStartDate = new Date(eventStartTimeString);
        modalTimeInput.min = formatToLocalString(eventStartDate);

        // 2. Xác định MAX time, bắt đầu bằng thời gian kết thúc sự kiện
        let finalMaxDate = new Date(eventEndTimeString);

        // 3. Nếu có thời gian check-out, so sánh nó với MAX time hiện tại
        if (checkoutTimeString) {
            const checkoutDate = new Date(checkoutTimeString);
            if (checkoutDate < finalMaxDate) {
                finalMaxDate = checkoutDate;
            }
        }

        modalTimeInput.max = formatToLocalString(finalMaxDate);
    }

    // lấy ra tgian hiện tại theo múi giờ máy người dùng
    // nhưng Date luôn lưu giá trị theo UTC, nên cần trừ đi timezone offset để hiển thị đúng
    const now = new Date();

    // getTimezoneOffset trả về sự khác biệt về phút giữa UTC và giờ địa phương (VN) -> -420p ( ~7 tiếng)
    // set lại phút của now bằng cách trừ đi sự khác biệt này
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());

    // now.ISOString() trả về chuỗi theo định dạng (UTC): YYYY-MM-DDTHH:mm:ss.sssZ
    // do đã bù trừ timezone ở dòng trên, toISOString() sẽ xuất ra chuỗi có giờ local, nhưng được biểu diễn như UTC.
    // slice(0,16) để lấy phần YYYY-MM-DDTHH:mm
    modalTimeInput.value = now.toISOString().slice(0, 16);

    document.getElementById("attendanceModal").classList.add("active");
}


document.getElementById('checkinForm').addEventListener('submit', function(event) {
    const timeInput = document.getElementById('modalTimeInput');

    if (!timeInput.value || !timeInput.min || !timeInput.max) {
        alert('Lỗi: Không thể xác định được khoảng thời gian hợp lệ.');
        event.preventDefault();
        return;
    }

    const selectedTime = new Date(timeInput.value);
    const minTime = new Date(timeInput.min);
    const maxTime = new Date(timeInput.max);

    if (selectedTime < minTime) {
        alert(`Lỗi: Thời gian check-in không được sớm hơn thời gian bắt đầu sự kiện.\nVui lòng chọn thời gian sau ${minTime.toLocaleString('vi-VN')}.`);
        event.preventDefault();
        return;
    }

    if (selectedTime >= maxTime) {
        alert(`Lỗi: Thời gian check-in phải trước thời gian kết thúc sự kiện/checkout.\nVui lòng chọn thời gian trước ${maxTime.toLocaleString('vi-VN')}.`);
        event.preventDefault();
    }
});


function closeCheckModal() {
    const modalOverlay = document.getElementById("attendanceModal");
    modalOverlay.classList.remove("active");
}

document
    .getElementById("attendanceModal")
    .addEventListener("click", function (event) {
        if (event.target === this) {
            closeCheckModal();
        }
    });

//======================================================================
// CHECK OUT modal functions

const pad = (num) => num.toString().padStart(2, '0');
const formatToLocalString = (date) => {
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

function openCheckOutModal(button) {
    const applicationId = button.dataset.applicationId;
    const volunteerName = button.dataset.name;
    const avatarUrl = button.dataset.avatar;
    const oppId = button.dataset.oppId;
    const checkinTimeString = button.dataset.checkinTime;
    const eventStartTimeString = button.dataset.eventStartTime;
    const eventEndTimeString = button.dataset.eventEndTime;

    const modalAvatar = document.getElementById('modalAvatar2');
    const modalVolunteerName = document.getElementById('modalVolunteerName2');
    const modalTimeInput = document.getElementById('modalTimeInput2');
    const applicationIdInput = document.getElementById('applicationIdInput2');
    const oppIdInput = document.getElementById('oppIdOut');
    oppIdInput.value = oppId;

    applicationIdInput.value = applicationId;
    modalAvatar.src = avatarUrl;
    modalVolunteerName.textContent = volunteerName;


    // Set MIN (Thời gian check-out > thời gian check-in)
    if (checkinTimeString) {
        const checkinDate = new Date(checkinTimeString);
        // checkinDate.setMinutes(checkinDate.getMinutes() + 1);
        const minTimeForCheckout = formatToLocalString(checkinDate);
        modalTimeInput.min = minTimeForCheckout;
    }

    // Set MAX (Thời gian check-out <= tgian kết thúc sự kiện)
    if (eventEndTimeString) {
        const eventEndDate = new Date(eventEndTimeString);
        const maxTimeForCheckout = formatToLocalString(eventEndDate);
        modalTimeInput.max = maxTimeForCheckout;
    }

    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    modalTimeInput.value = now.toISOString().slice(0, 16);

    document.getElementById("attendanceModal2").classList.add("active");
}

document.getElementById('checkinForm2').addEventListener('submit', function(event) {
    const checkoutInput = document.getElementById('modalTimeInput2');
    const checkoutTime = new Date(checkoutInput.value);
    const minTime = new Date(checkoutInput.min);
    const maxTime = new Date(checkoutInput.max);

    if (checkoutTime <= minTime) {
        alert('Lỗi: Thời gian check-out không được sớm hơn hoặc bằng thời gian check-in.');
        event.preventDefault();
        return;
    }

    if (checkoutTime > maxTime) {
        alert('Lỗi: Thời gian check-out phải ở trong thời gian diễn ra sự kiện.');
        event.preventDefault();
    }
});

document
    .getElementById("attendanceModal2")
    .addEventListener("click", function (event) {
        if (event.target === this) {
            closeModal();
        }
    });

function closeCheckModal2() {
    const modalOverlay = document.getElementById("attendanceModal2");
    modalOverlay.classList.remove("active");
}

//======================================================================
// view details modal functions
function openViewDetailModal(button) {
    const data = button.dataset;

    document.getElementById('detailOppTitle').textContent = data.oppTitle || 'N/A';
    document.getElementById('detailCheckinTime').textContent = data.checkinTime || '--:--';
    document.getElementById('detailVolunteerName').textContent = data.name || 'N/A';
    document.getElementById('detailCheckoutTime').textContent = data.checkoutTime || '--:--';
    document.getElementById('detailPhone').textContent = data.phoneNumber || 'N/A';
    document.getElementById('detailAddress').textContent = data.address || 'N/A';
    document.getElementById('oppId2').value = data.oppId;


    const statusContainer = document.getElementById('detailStatusBadge');

    let statusClass = 'pending';
    let statusText = 'Chưa điểm danh';

    if (data.status === 'COMPLETED') {
        statusClass = 'completed';
        statusText = 'Hoàn thành';
    } else if (data.status === 'PRESENT') {
        statusClass = 'present';
        statusText = 'Có mặt';
    } else if (data.status === 'ABSENT') {
        statusClass = 'absent';
        statusText = 'Vắng mặt';
    }

    statusContainer.innerHTML = `<span class="status-badge ${statusClass}">${statusText}</span>`;

    const hoursContainer = document.getElementById('detailTotalHours');
    hoursContainer.innerHTML = data.totalHours ? `<span class="hours-badge">${data.totalHours}h</span>` : 'N/A';

    document.getElementById('detailNotes').value = data.note || '';
    document.getElementById('applicationId').value = data.applicationId || '';


    const proofContainer = document.getElementById('detailProofLinkContainer');

    const fileInput = document.getElementById('detailProofFile');

    if (data.proofUrl && data.proofUrl.trim() !== "") {
        const fileName = data.proofUrl.split("/").pop();
        proofContainer.innerHTML = `
                <p style="margin: 0; font-size: 0.9em; color: #555;">
                    File hiện tại:
                    <a href="${data.proofUrl}" target="_blank" title="${fileName}">
                        <i class="fa-regular fa-file-lines" style="margin-right: 5px;"></i>${fileName}
                    </a>
                </p>`;
    } else {
        proofContainer.innerHTML = "Không có tệp đính kèm.";
    }
    fileInput.value = '';
    document.getElementById('modalOverlay').classList.add('active');
}

// close view detail modal
function closeDetailModal() {
    document.getElementById('modalOverlay').classList.remove('active');
}

// check submit form details
const detailsForm = document.getElementById('detailsForm');
detailsForm.addEventListener('submit', function(event) {
    const checkinTimeText = document.getElementById('detailCheckinTime').textContent;
    if (checkinTimeText === '--:--') {
        event.preventDefault();
        alert('Hãy check-in trước khi cập nhật !!');
    }
});

// check file uploaded of form view details
document.addEventListener('DOMContentLoaded', function () {
    const respondForm = document.querySelector('#modalOverlay form');
    respondForm.addEventListener('submit', function (event) {

        const fileInput = respondForm.querySelector('input[name="proofFile"]');
        const filesList = fileInput.files;
        if (filesList.length === 0) {
            return;
        }
        const file = fileInput.files[0];

        // const MAX_FILES = 3;
        // if (filesList.length > MAX_FILES) {
        //     alert(`Lỗi: Bạn chỉ được tải lên tối đa ${MAX_FILES} file.`);
        //     event.preventDefault();
        //     return;
        // }

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

        // for (const file of filesList) {
        //     if (!ALLOWED_TYPES.includes(file.type)) {
        //         alert(`Lỗi: File "${file.name}" không được hỗ trợ.\nVui lòng chỉ tải lên ảnh (jpeg, png), video (mp4, mov), hoặc tài liệu (doc, docx, pdf).`);
        //         event.preventDefault();
        //         return;
        //     }
        //
        //     if (file.size > MAX_SIZE_BYTES) {
        //         alert(`Lỗi: File "${file.name}" vượt quá dung lượng 50MB.`);
        //         event.preventDefault();
        //         return;
        //     }
        // }

    });
});

















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

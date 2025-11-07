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
    const typeButton = button.dataset.type;
    const checkinTimeString = button.dataset.checkinTime;

    // Lấy các giá trị filter
    const keyword = button.dataset.keyword;
    const status = button.dataset.status;
    const num = button.dataset.num;
    const page = button.dataset.page;

    const modalAvatar = document.getElementById('modalAvatar');
    const modalVolunteerName = document.getElementById('modalVolunteerName');
    const modalTimeInput = document.getElementById('modalTimeInput');
    const applicationIdInput = document.getElementById('applicationIdInput');
    const oppIdInput = document.getElementById('oppId');
    const modalTitle = document.getElementById('modalTitleCheckIn');

    // Lấy các input filter
    const keywordInput = document.getElementById('keyword');
    const statusInput = document.getElementById('status');
    const numInput = document.getElementById('num');
    const pageInput = document.getElementById('page');


    // === PHẦN SỬA ĐỔI QUAN TRỌNG ===
    // Kiểm tra và vô hiệu hóa các input filter nếu chúng không có giá trị
    // (Giá trị "falsy" như undefined, null, "" đều sẽ vào 'else')

    if (keyword) {
        keywordInput.value = keyword;
        keywordInput.disabled = false; // Bật để gửi đi
    } else {
        keywordInput.value = '';
        keywordInput.disabled = true; // Tắt để không gửi đi
    }

    if (status) {
        statusInput.value = status;
        statusInput.disabled = false;
    } else {
        statusInput.value = '';
        statusInput.disabled = true;
    }

    if (num) {
        numInput.value = num;
        numInput.disabled = false;
    } else {
        numInput.value = '';
        numInput.disabled = true;
    }

    if (page) {
        pageInput.value = page;
        pageInput.disabled = false;
    } else {
        pageInput.value = '1';
        pageInput.disabled = true;
    }
    // === KẾT THÚC PHẦN SỬA ĐỔI ===


    if(typeButton === 'CheckIn') {
        modalTitle.textContent = 'XÁC NHẬN CHECK-IN';
    } else{
        modalTitle.textContent = 'CHỈNH SỬA THỜI GIAN CHECK-IN';
    }

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

    // TH: UPDATE CHECK-IN
    if(checkinTimeString) {
        const checkinDate = new Date(checkinTimeString);
        modalTimeInput.value = formatToLocalString(checkinDate);
    } else{
        // TH: NEW CHECK-IN
        const now = new Date();
        now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
        modalTimeInput.value = now.toISOString().slice(0, 16);
    }

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
    const eventEndTimeString = button.dataset.eventEndTime;
    const typeButton = button.dataset.type;
    const checkoutTimeString = button.dataset.checkoutTime;

    // Lấy các giá trị filter
    const keyword = button.dataset.keyword;
    const status = button.dataset.status;
    const num = button.dataset.num;
    const page = button.dataset.page;

    const modalAvatar = document.getElementById('modalAvatar2');
    const modalVolunteerName = document.getElementById('modalVolunteerName2');
    const modalTimeInput = document.getElementById('modalTimeInput2');
    const applicationIdInput = document.getElementById('applicationIdInput2');
    const oppIdInput = document.getElementById('oppIdOut');
    const modalTitle = document.getElementById('modalTitleCheckOut');


    // Lấy các input filter
    const keywordInput = document.getElementById('keyword2');
    const statusInput = document.getElementById('status2');
    const numInput = document.getElementById('num2');
    const pageInput = document.getElementById('page2');


    // === PHẦN SỬA ĐỔI QUAN TRỌNG ===
    // Kiểm tra và vô hiệu hóa các input filter nếu chúng không có giá trị
    // (Giá trị "falsy" như undefined, null, "" đều sẽ vào 'else')

    if (keyword) {
        keywordInput.value = keyword;
        keywordInput.disabled = false; // Bật để gửi đi
    } else {
        keywordInput.value = '';
        keywordInput.disabled = true; // Tắt để không gửi đi
    }

    if (status) {
        statusInput.value = status;
        statusInput.disabled = false;
    } else {
        statusInput.value = '';
        statusInput.disabled = true;
    }

    if (num) {
        numInput.value = num;
        numInput.disabled = false;
    } else {
        numInput.value = '';
        numInput.disabled = true;
    }

    if (page) {
        pageInput.value = page;
        pageInput.disabled = false;
    } else {
        pageInput.value = '1';
        pageInput.disabled = true;
    }
    // === KẾT THÚC PHẦN SỬA ĐỔI ===

    if(typeButton === 'CheckOut') {
        modalTitle.textContent = 'XÁC NHẬN CHECK-OUT';
    } else{
        modalTitle.textContent = 'CHỈNH SỬA THỜI GIAN CHECK-OUT';
    }

    oppIdInput.value = oppId;

    applicationIdInput.value = applicationId;
    modalAvatar.src = avatarUrl;
    modalVolunteerName.textContent = volunteerName;


    // Set MIN (Thời gian check-out > thời gian check-in)
    if (checkinTimeString) {
        const checkinDate = new Date(checkinTimeString);
        const minTimeForCheckout = formatToLocalString(checkinDate);
        modalTimeInput.min = minTimeForCheckout;
    }

    // Set MAX (Thời gian check-out <= tgian kết thúc sự kiện)
    if (eventEndTimeString) {
        const eventEndDate = new Date(eventEndTimeString);
        const maxTimeForCheckout = formatToLocalString(eventEndDate);
        modalTimeInput.max = maxTimeForCheckout;
    }

    if(checkoutTimeString) {
        const checkoutDate = new Date(checkoutTimeString);
        modalTimeInput.value = formatToLocalString(checkoutDate);
    } else{
        const now = new Date();
        now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
        modalTimeInput.value = now.toISOString().slice(0, 16);
    }

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
            closeCheckModal2();
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

    // Lấy các input filter
    const keywordInput = document.getElementById('keyword3');
    const statusInput = document.getElementById('status3');
    const numInput = document.getElementById('num3');
    const pageInput = document.getElementById('page3');


    // === PHẦN SỬA ĐỔI QUAN TRỌNG ===
    // Kiểm tra và vô hiệu hóa các input filter nếu chúng không có giá trị
    // (Giá trị "falsy" như undefined, null, "" đều sẽ vào 'else')

    if (data.keyword) {
        keywordInput.value = data.keyword;
        keywordInput.disabled = false; // Bật để gửi đi
    } else {
        keywordInput.value = '';
        keywordInput.disabled = true; // Tắt để không gửi đi
    }

    if (data.statusFilter) {
        statusInput.value = data.statusFilter;
        statusInput.disabled = false;
    } else {
        statusInput.value = '';
        statusInput.disabled = true;
    }

    if (data.num) {
        numInput.value = data.num;
        numInput.disabled = false;
    } else {
        numInput.value = '';
        numInput.disabled = true;
    }

    if (data.page) {
        pageInput.value = data.page;
        pageInput.disabled = false;
    } else {
        pageInput.value = '1';
        pageInput.disabled = true;
    }
    // === KẾT THÚC PHẦN SỬA ĐỔI ===

    document.getElementById('questionErrorEdit').style.display = 'none';

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

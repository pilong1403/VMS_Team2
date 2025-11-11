

function openEditSupportTicketModal(btn) {

    const ticketId = btn.dataset.ticketId;
    const subject = btn.dataset.subject;
    const priority = btn.dataset.priority;
    const description = btn.dataset.des;
    const attachmentUrl = btn.dataset.file;

    const titleElement = document.getElementById('titleUpdate');
    const ticketIdInput = document.getElementById('ticketIdUpdate');
    const subjectInput = document.getElementById('edit-title');
    const prioritySelect = document.getElementById('edit-priority');
    const descriptionTextarea = document.getElementById('desUpdate');
    const attachmentInput = document.getElementById('attachmentUpdate');
    const proofContainer = document.getElementById('detailProofLinkContainer');

    titleError.textContent = '';
    descError.textContent = '';


    titleElement.textContent = 'Cập nhật đơn #' + ticketId;
    ticketIdInput.value = ticketId;

    // set lại các giá trị đã nhập trc đó
    subjectInput.value = subject;
    descriptionTextarea.value = description;
    prioritySelect.value = priority;

    // Xử lý hiển thị file đính kèm
    if (attachmentUrl && attachmentUrl.trim() !== "") {
        const fileName = attachmentUrl.split("/").pop();
        proofContainer.innerHTML = `
                <p style="margin: 0; font-size: 0.9em; color: #555;">
                    File hiện tại:
                    <a href="${attachmentUrl}" target="_blank" title="${fileName}">
                        <i class="fa-regular fa-file-lines" style="margin-right: 5px;"></i>${fileName}
                    </a>
                </p>`;
    } else {
        proofContainer.innerHTML = "Không có tệp đính kèm.";
    }
    attachmentInput.value = '';
    document.getElementById('updateModal').classList.add('show');
}

// check submit UPDATE đơn yêu cầu
const form = document.getElementById('updateTicketForm');
const submitButton = document.getElementById('submit-update-btn');
const titleInput = document.getElementById('edit-title');
const descInput = document.getElementById('desUpdate');
const fileInput = document.getElementById('attachmentUpdate');
const titleError = document.getElementById('title-error');
const descError = document.getElementById('desc-error');

submitButton.addEventListener('click', function (event) {
    event.preventDefault();

    titleError.textContent = '';
    descError.textContent = '';

    let isValid = true;

    // --- VALIDATION 1: WHITESPACE ---
    const titleValue = titleInput.value.trim();
    const descValue = descInput.value.trim();

    if (titleValue.length === 0) {
        titleError.textContent = 'Tiêu đề không được để trống hoặc chỉ chứa khoảng trắng.';
        isValid = false;
    }

    if (descValue.length === 0) {
        descError.textContent = 'Mô tả không được để trống hoặc chỉ chứa khoảng trắng.';
        isValid = false;
    }

    // --- VALIDATION 2: FILE UPLOAD
    if (fileInput.files.length > 0) { // Chỉ kiểm tra nếu người dùng chọn file
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
        const MAX_SIZE_BYTES = 52428800; // 50MB

        if (!ALLOWED_TYPES.includes(file.type)) {
            alert('Lỗi: Loại file không được hỗ trợ.\nVui lòng chỉ tải lên file ảnh(jpeg, png), video (mp4, mov) hoặc tài liệu Word (doc, docx).');
            isValid = false; // Đánh dấu không hợp lệ
        }

        if (file.size > MAX_SIZE_BYTES) {
            alert('Lỗi: Dung lượng file không được vượt quá 50MB.');
            isValid = false; // Đánh dấu không hợp lệ
        }
    }

    if (isValid) {
        // Nếu TẤT CẢ validation đều qua
        console.log('Validation thành công, đang submit form...');
        form.submit(); // Tự tay submit form
    } else {
        // Nếu có bất kỳ lỗi nào
        console.log('Validation thất bại, form bị chặn.');
    }

    titleInput.addEventListener('input', function() {
        if (titleError.textContent !== '') {
            titleError.textContent = '';
        }
    });

    descInput.addEventListener('input', function() {
        if (descError.textContent !== '') {
            descError.textContent = '';
        }
    });
});

// tạo đơn yêu cầu
document.addEventListener('DOMContentLoaded', function () {

    const form = document.getElementById('newTicketForm');
    if (!form) return;

    const subjectInput = document.getElementById('subject');
    const descInput = document.getElementById('description');
    const fileInput = document.getElementById('file-upload');

    const subjectError = document.getElementById('submit-subject-error');
    const descError = document.getElementById('submit-desc-error');
    const fileError = document.getElementById('submit-file-error');

    const ALLOWED_TYPES = [
        "image/jpeg",
        "image/png",
        "image/gif",
        "video/mp4",
        "video/quicktime", // .mov
        "application/msword", // .doc
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // .docx
    ];
    const MAX_SIZE_BYTES = 52428800; // 50MB

    form.addEventListener('submit', function (event) {

        let isValid = true;

        subjectError.textContent = '';
        descError.textContent = '';
        fileError.textContent = '';

        // --- VALIDATION 1: WHITESPACE ---
        if (subjectInput.value.trim().length === 0) {
            subjectError.textContent = 'Tiêu đề không được để trống hoặc chỉ chứa khoảng trắng.';
            isValid = false;
        }

        if (descInput.value.trim().length === 0) {
            descError.textContent = 'Mô tả không được để trống hoặc chỉ chứa khoảng trắng.';
            isValid = false;
        }

        // --- VALIDATION 2: FILE ---
        const files = fileInput.files;

// 1. Kiểm tra xem người dùng có chọn file nào không
        if (files.length > 0) {

            const file = files[0];

            if (!ALLOWED_TYPES.includes(file.type)) {
                fileError.textContent = `File "${file.name}" có định dạng không hợp lệ.`;
                isValid = false;

            } else if (file.size > MAX_SIZE_BYTES) {
                fileError.textContent = `File "${file.name}" vượt quá 50MB.`;
                isValid = false;
            }
        }
// (Nếu files.length == 0, nó sẽ bỏ qua và không báo lỗi, điều này là đúng)

        if (isValid === false) {
            event.preventDefault();
            console.log('Validation thất bại, form đã bị chặn.');
        } else {
            console.log('Validation thành công, đang gửi form...');
        }
    });

    subjectInput.addEventListener('input', function() {
        if (subjectError.textContent !== '') {
            subjectError.textContent = '';
        }
    });

    descInput.addEventListener('input', function() {
        if (descError.textContent !== '') {
            descError.textContent = '';
        }
    });


});




function openResponseDetailModal() {
    document.getElementById('responseDetailModal').classList.add('show');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

window.onclick = function(event) {
    if (event.target.classList.contains('modal')) {
        event.target.classList.remove('show');
    }
}


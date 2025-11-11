// ẩn flash
setTimeout(() => {
    // Tìm tất cả alert bên trong #flashArea và bắt đầu hiệu ứng ẩn
    document.querySelectorAll('#flashArea .alert').forEach(a => {
        a.classList.remove('show'); // bỏ trạng thái hiện
        a.classList.add('hide');    // thêm trạng thái ẩn (dựa vào CSS transition của Bootstrap)
        // Khi transition kết thúc, xóa hẳn phần tử để tránh chiếm chỗ
        a.addEventListener('transitionend', () => a.remove(), { once: true });
    });
}, 4000); // Sau 4 giây mới ẩn

// Lấy các phần tử chính của form
const formEl = document.getElementById('oppForm');
const sectionsContainer = document.getElementById('sectionsContainer'); // vùng chứa các section động
const tmpl = document.getElementById('sectionTemplate'); // <template> để clone section mới
const btnAdd = document.getElementById('btnAddSection'); // nút thêm section

// ==== THUMBNAIL: preview + nút X ====
const thumbInput = document.getElementById('thumbInput');     // input file cho thumbnail
const thumbPreview = document.getElementById('thumbPreview'); // <img> preview ảnh mới (chưa lưu)
const thumbNewWrap = document.getElementById('thumbNewWrap'); // wrapper khối preview ảnh mới

// --- Image constraints (client-side, khớp server) ---
const MAX_IMAGE_BYTES = 5 * 1024 * 1024; // 5MB
const ALLOWED_IMAGE_TYPES = new Set(["image/jpeg","image/png","image/gif","image/webp"]); // các loại ảnh hợp lệ

// Hiển thị lỗi tại group chứa input
function showInputError(input, message) {
    input.classList.add('is-invalid'); // viền đỏ
    // tìm group (cột bootstrap) gần nhất để gắn error
    const group = input.closest('.col-md-2, .col-md-10, .col-md-6, .col-12');
    if (group) {
        let err = group.querySelector('.form-error');
        if (!err) {
            // Chưa có container lỗi thì tạo mới
            err = document.createElement('div');
            err.className = 'form-error';
            group.appendChild(err);
        }
        err.textContent = message; // thông báo lỗi
        err.style.display = 'block'; // hiện lỗi
    }
}

// Kiểm tra hợp lệ cho 1 input file ảnh (loại + kích thước)
function validateImageInput(inputEl) {
    if (!inputEl || !inputEl.files || inputEl.files.length === 0) return true; // không chọn ảnh => hợp lệ (tùy nghiệp vụ)
    const f = inputEl.files[0];
    if (!ALLOWED_IMAGE_TYPES.has(f.type)) {
        showInputError(inputEl, 'Chỉ chấp nhận ảnh jpg/png/gif/webp');
        return false;
    }
    if (f.size > MAX_IMAGE_BYTES) {
        showInputError(inputEl, 'Kích thước ảnh tối đa 5MB');
        return false;
    }
    return true;
}

// Quét toàn form để validate tất cả input ảnh (thumbnail + ảnh sections)
function validateAllImages() {
    let ok = true;
    // Lưu ý: bạn đang dùng cả .sectionFile và .sec-file cho 2 ngữ cảnh: section từ server và section mới từ template
    const fileInputs = document.querySelectorAll('#thumbInput, .sectionFile, .sec-file');
    fileInputs.forEach(inp => {
        hideErrorForInput(inp);           // clear lỗi cũ (nếu có)
        if (!validateImageInput(inp)) ok = false; // validate từng input
    });
    return ok;
}

// Sự kiện chọn thumbnail
if (thumbInput) {
    thumbInput.addEventListener('change', e => {
        hideErrorForInput(thumbInput);            // clear lỗi cũ
        if (!validateImageInput(thumbInput)) {    // nếu không hợp lệ => reset preview + input
            if (thumbNewWrap) thumbNewWrap.style.display = 'none';
            if (thumbPreview) { thumbPreview.removeAttribute('src'); thumbPreview.style.display = 'none'; }
            thumbInput.value = ''; // xóa file đã chọn
            return;
        }

        const f = e.target.files[0]; // file người dùng chọn

        // KHÔNG ẩn ảnh cũ khi chọn ảnh mới — hiển thị song song
        // Nếu người dùng xóa chọn, trả UI về trạng thái không có ảnh mới
        if (!f) {
            if (thumbNewWrap) thumbNewWrap.style.display = 'none';
            if (thumbPreview) {
                thumbPreview.removeAttribute('src');
                thumbPreview.style.display = 'none';
            }
            return;
        }

        // Tạo URL tạm để preview ảnh mới ngay trên client
        const url = URL.createObjectURL(f);
        if (thumbPreview) {
            thumbPreview.src = url;
            thumbPreview.style.display = 'block';
        }
        if (thumbNewWrap) thumbNewWrap.style.display = 'block';

        // Gắn handler cho nút X ở khối ảnh mới (chỉ xóa ảnh mới)
        const btnX = thumbNewWrap ? thumbNewWrap.querySelector('.btn-preview-remove') : null;
        if (btnX) {
            btnX.onclick = () => {
                // chỉ bỏ ảnh mới, KHÔNG clear thumbnailUrl (ẩn cờ ảnh cũ); server sẽ thấy thumbnailUrl giữ nguyên
                thumbNewWrap.style.display = 'none';
                thumbPreview.removeAttribute('src');
                thumbPreview.style.display = 'none';
                thumbInput.value = '';
            };
        }
    });
}

// Gắn sự kiện cho 1 section (khi render sẵn từ server hoặc khi vừa thêm mới từ template)
function wireSectionEvents(wrapper) {
    const btn = wrapper.querySelector('.btnRemoveSection');
    if (btn) btn.addEventListener('click', () => { wrapper.remove(); reindex(); }); // xóa section và đánh số lại

    // preview ảnh mới trong section + nút X
    const fileInput = wrapper.querySelector('input[type=file]');
    const newWrap   = wrapper.querySelector('.section-new-wrap'); // khối preview ảnh mới
    const imgNew    = wrapper.querySelector('img.new-image');     // thẻ <img> ảnh mới
    if (fileInput && imgNew) {
        fileInput.addEventListener('change', e => {
            hideErrorForInput(fileInput);            // clear lỗi
            if (!validateImageInput(fileInput)) {    // ảnh không hợp lệ => reset preview + input
                if (newWrap) newWrap.style.display = 'none';
                imgNew.style.display = 'none';
                imgNew.removeAttribute('src');
                fileInput.value = '';
                return;
            }

            const f = e.target.files && e.target.files[0];

            // KHÔNG ẩn ảnh cũ khi chọn ảnh mới — hiển thị song song
            // Nếu người dùng xóa lựa chọn => ẩn phần preview ảnh mới
            if (!f) {
                if (newWrap) newWrap.style.display = 'none';
                imgNew.style.display = 'none';
                imgNew.removeAttribute('src');
                return;
            }
            // Tạo URL tạm và hiện ảnh mới
            const url = URL.createObjectURL(f);
            imgNew.src = url;
            imgNew.style.display = 'block';
            if (newWrap) newWrap.style.display = 'block';

            // Nút X cho preview ảnh mới của section (chỉ xóa ảnh mới, không đụng ảnh cũ/hidden)
            const btnX = newWrap ? newWrap.querySelector('.btn-preview-remove') : null;
            if (btnX) {
                btnX.onclick = () => {
                    // chỉ bỏ ảnh mới, KHÔNG đụng tới hidden imageUrl (giữ ảnh cũ)
                    newWrap.style.display = 'none';
                    imgNew.removeAttribute('src');
                    imgNew.style.display = 'none';
                    fileInput.value = '';
                };
            }
        });
    }
}

// Helper gán name/id đúng cú pháp sections[<idx>].field để server bind vào list
function setField(root, sel, name, id) {
    const el = root.querySelector(sel);
    if (!el) return;
    el.setAttribute('name', name);
    el.id = id;
    // đồng bộ label[for] nếu có
    const group = el.closest('.col-md-2, .col-md-10, .col-md-6, .col-12');
    if (group) {
        const label = group.querySelector('label');
        if (label) label.setAttribute('for', id);
    }
}

// Đánh số lại toàn bộ section + cập nhật name/id theo index
function reindex() {
    const items = sectionsContainer.querySelectorAll('.section-item');
    items.forEach((it, idx) => {
        const num = it.querySelector('.sec-number'); // số hiển thị "Phần N"
        if (num) num.innerText = idx + 1;
        // Cập nhật name/id cho các field để Spring MVC bind chính xác theo index
        setField(it, '.sec-order',     `sections[${idx}].sectionOrder`, `sec-order-${idx}`);
        setField(it, '.sec-heading',   `sections[${idx}].heading`,      `sec-heading-${idx}`);
        setField(it, '.sec-content',   `sections[${idx}].content`,      `sec-content-${idx}`);
        setField(it, '.sec-file',      `sections[${idx}].imageFile`,    `sec-file-${idx}`);
        setField(it, '.sec-image-url', `sections[${idx}].imageUrl`,     `sec-image-url-${idx}`);
        setField(it, '.sec-caption',   `sections[${idx}].caption`,      `sec-caption-${idx}`);
    });
}

// Thêm section mới từ <template> rồi gắn sự kiện + reindex
if (btnAdd) {
    btnAdd.addEventListener('click', () => {
        const frag = tmpl.content.cloneNode(true);     // clone nội dung template
        sectionsContainer.appendChild(frag);           // thêm vào cuối
        const last = sectionsContainer.querySelector('.section-item:last-of-type');
        wireSectionEvents(last);                       // gắn events cho section mới
        reindex();                                     // đánh số lại + cập nhật name/id
    });
}

// Gắn sự kiện cho các section hiện có (render từ server)
sectionsContainer.querySelectorAll('.section-item').forEach(wireSectionEvents);
// Khởi tạo reindex để đồng bộ label/for và id ngay khi load
reindex();

// ẩn lỗi khi focus (clear trạng thái is-invalid và ẩn .form-error)
function hideErrorForInput(input) {
    if (!input) return;
    input.classList.remove('is-invalid');
    const group = input.closest('.col-md-2, .col-md-10, .col-md-6, .col-12');
    if (group) {
        const err = group.querySelector('.form-error');
        if (err) err.style.display = 'none';
    }
    // đồng thời ẩn alert-danger tổng (nếu có)
    document.querySelectorAll('.alert.alert-danger').forEach(a => a.style.display = 'none');
}

// Lắng nghe các sự kiện để chủ động ẩn lỗi khi người dùng tương tác
formEl.addEventListener('input', (e) => {
    const t = e.target;
    if (['INPUT','TEXTAREA','SELECT'].includes(t.tagName)) hideErrorForInput(t);
});
formEl.addEventListener('change', (e) => {
    const t = e.target;
    if (['INPUT','TEXTAREA','SELECT'].includes(t.tagName)) hideErrorForInput(t);
});
formEl.addEventListener('focusin', (e) => {
    const t = e.target;
    if (['INPUT','TEXTAREA','SELECT'].includes(t.tagName)) hideErrorForInput(t);
});
// Cho phép click vào thông báo .form-error để focus lại input tương ứng
formEl.addEventListener('click', (e) => {
    const err = e.target.closest('.form-error');
    if (!err) return;
    const group = err.closest('.col-md-2, .col-md-10, .col-md-6, .col-12');
    const input = group ? group.querySelector('input, textarea, select') : null;
    if (input) { input.focus(); hideErrorForInput(input); }
    else { err.style.display = 'none'; }
});

// ====== Validate thứ tự section (trùng/âm) ======

// Đánh dấu input không hợp lệ với message
function markInvalid(input, msg) {
    input.classList.add('is-invalid');
    const group = input.closest('.col-md-2, .col-md-10, .col-md-6, .col-12');
    if (group) {
        let err = group.querySelector('.form-error');
        if (!err) {
            err = document.createElement('div');
            err.className = 'form-error';
            group.appendChild(err);
        }
        err.textContent = msg;
        err.style.display = 'block';
    }
}
// Xóa tất cả lỗi thứ tự trước khi re-validate
function clearOrderErrors() {
    document.querySelectorAll('input[id^="sec-order-"], .sec-order').forEach(inp => {
        inp.classList.remove('is-invalid');
        const group = inp.closest('.col-md-2, .col-md-10, .col-md-6, .col-12');
        const err = group ? group.querySelector('.form-error') : null;
        if (err) err.style.display = 'none';
    });
}

// Kiểm tra các giá trị "Thứ tự" (>=1, không trùng)
function validateSectionOrdersClient() {
    clearOrderErrors();
    const orders = {}; // map giá trị thứ tự -> input
    let ok = true;
    const inputs = sectionsContainer.querySelectorAll('input[type="number"][id^="sec-order-"], .sec-order');
    inputs.forEach(inp => {
        const v = parseInt(inp.value, 10);
        if (!Number.isFinite(v) || v < 1) {
            markInvalid(inp, 'Thứ tự phải là số dương (>=1)');
            ok = false; return;
        }
        if (orders[v]) {
            // trùng thứ tự với một input khác
            markInvalid(inp, 'Thứ tự bị trùng');
            markInvalid(orders[v], 'Thứ tự bị trùng');
            ok = false;
        } else {
            orders[v] = inp;
        }
    });
    return ok;
}

// Khi người dùng thay đổi các ô "Thứ tự" -> validate realtime
formEl.addEventListener('input', (e) => {
    if (e.target.matches('input[type="number"][id^="sec-order-"], .sec-order')) {
        validateSectionOrdersClient();
    }
});

// Gói lại reindex để sau mỗi lần reindex sẽ validate thứ tự luôn
const oldReindex = reindex;
reindex = function() {
    oldReindex();
    validateSectionOrdersClient();
};

// Trim toàn bộ text trước khi submit (loại khoảng trắng đầu/cuối)
function trimAllFields() {
    const textInputs = formEl.querySelectorAll('input[type="text"], textarea');
    textInputs.forEach(el => {
        if (typeof el.value === 'string') el.value = el.value.trim();
    });
}

// Khi submit: trim + validate thứ tự + validate ảnh; nếu fail -> chặn submit
formEl.addEventListener('submit', (e) => {
    trimAllFields();
    const ordersOk = validateSectionOrdersClient();
    const imagesOk = validateAllImages();
    if (!ordersOk || !imagesOk) {
        e.preventDefault(); e.stopPropagation();
        return;
    }
});

// Ngăn Enter tự submit trong <input> (cho phép Enter trong <textarea>)
formEl.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && e.target.tagName === 'INPUT') {
        e.preventDefault();
    }
});

// ====== Modal publish/cancel tự bật (nếu có flag ẩn) ======
const publishModalEl = document.getElementById('publishConfirmModal');
const publishModal = publishModalEl ? new bootstrap.Modal(publishModalEl) : null;
const forcePublishEl = document.getElementById('forcePublishConfirm'); // hidden: 'true' => mở modal trước khi submit
const confirmPublishBtn = document.getElementById('confirmPublishBtn');
if (forcePublishEl && forcePublishEl.value === 'true' && publishModal) {
    let pendingSubmit = true;      // chặn submit khi modal đang hiển thị
    publishModal.show();
    if (confirmPublishBtn) {
        confirmPublishBtn.addEventListener('click', () => {
            document.getElementById('confirmPublish').value = 'true'; // gắn cờ xác nhận để server biết
            publishModal.hide();
            if (pendingSubmit) { pendingSubmit = false; formEl.submit(); } // submit lại sau khi xác nhận
        });
    }
}

const cancelModalEl = document.getElementById('cancelConfirmModal');
const cancelModal = cancelModalEl ? new bootstrap.Modal(cancelModalEl) : null;
const forceCancelEl = document.getElementById('forceCancelConfirm'); // hidden: 'true' => mở modal hủy trước khi submit
const confirmCancelBtn = document.getElementById('confirmCancelBtn');
if (forceCancelEl && forceCancelEl.value === 'true' && cancelModal) {
    let pendingSubmit = true;
    cancelModal.show();
    if (confirmCancelBtn) {
        confirmCancelBtn.addEventListener('click', () => {
            document.getElementById('confirmCancel').value = 'true'; // gắn cờ xác nhận hủy
            cancelModal.hide();
            if (pendingSubmit) { pendingSubmit = false; formEl.submit(); }
        });
    }
}

// ====== Khóa form khi CLOSED/CANCELLED nhưng vẫn cho phép bấm nút xóa ảnh (để UI nhất quán) ======
const isLocked = document.querySelector('form.locked') !== null;
if (isLocked) {
    // disable tất cả control ngoại trừ _csrf, #status, nút xóa ảnh, và các input hidden
    Array.from(formEl.elements).forEach(el => {
        const name = el.getAttribute('name') || '';
        if (name === '_csrf' || el.id === 'status') return; // chừa lại để server vẫn đọc
        if (el.classList.contains('btn-remove-thumb')) return;       // vẫn cho phép xóa ảnh cũ (UI)
        if (el.classList.contains('btn-remove-sec-image')) return;   // như trên
        if (el.type !== 'hidden') el.disabled = true;
    });
    const btnSubmit = document.getElementById('btnSubmit');
    if (btnSubmit) btnSubmit.disabled = true; // chặn submit khi form bị khóa
}

// --- Xóa thumbnail cũ: chỉ ẩn ảnh cũ + gắn cờ cho server, KHÔNG đụng ảnh mới ---
document.addEventListener('click', (e) => {
    const btn = e.target.closest('.btn-remove-thumb');
    if (!btn) return;

    const wrap = btn.closest('.old-thumb-wrap');
    if (wrap) wrap.classList.add('d-none'); // ẩn ảnh cũ khỏi UI

    const hiddenUrl = document.querySelector('input[name="thumbnailUrl"]');
    if (hiddenUrl) hiddenUrl.value = '__CLEAR__'; // cờ đặc biệt để server xóa ảnh cũ

    // KHÔNG xóa thumbInput, KHÔNG ẩn preview ảnh mới => nếu người dùng đã chọn ảnh mới thì vẫn giữ
});

// --- Xóa ảnh section cũ: chỉ ẩn ảnh cũ + gắn cờ, KHÔNG đụng ảnh mới ---
document.addEventListener('click', (e) => {
    const btn = e.target.closest('.btn-remove-sec-image');
    if (!btn) return;

    const oldWrap = btn.closest('.old-sec-wrap');
    if (oldWrap) oldWrap.classList.add('d-none'); // ẩn ảnh cũ phần section

    const colWrap = btn.closest('.col-md-6');
    if (!colWrap) return;

    // Tìm hidden imageUrl tương ứng trong cột và gán '__CLEAR__' để server biết cần xóa ảnh cũ
    const hidden = colWrap.querySelector('input[type="hidden"][name$=".imageUrl"]');
    if (hidden) hidden.value = '__CLEAR__';

    // KHÔNG xóa fileInput và KHÔNG ẩn preview ảnh mới => giữ lựa chọn hiện tại của người dùng
});

/***** 1. Helper function chung *****/
const $ = sel => document.querySelector(sel);
const $$ = sel => document.querySelectorAll(sel);

// Hiển thị toast thông báo
function showToast(title, msg) {
    const toast = $('#toast');
    if (!toast) return;
    $('#toastTitle').textContent = title;
    $('#toastMessage').textContent = msg;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 2500);
}

// Tạo chữ viết tắt từ tên
function initials(name) {
    return (name || '')
        .split(' ')
        .map(w => w[0])
        .join('')
        .toUpperCase()
        .slice(0, 2);
}


/***** 2. Sidebar toggle (ẩn/hiện menu bên trái) *****/
document.addEventListener('click', e => {
    if (e.target.id === 'sidebarToggle') {
        $('#sidebar')?.classList.toggle('collapsed');
    }
    if (e.target.dataset?.close) {
        $('#' + e.target.dataset.close)?.classList.remove('show');
    }
});


/***** 3. Router - gọi init theo trang hiện tại *****/
document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;
    if (page === 'users') initUsersPage();
});


/*************************************************
 * 4. Trang "Quản lý Người dùng"
 *************************************************/
function initUsersPage() {

    /*** Mở & đóng modal thêm user ***/
    $('#btnOpenAddUser')?.addEventListener('click', () => {
        $('#addUserModal').classList.add('show');
    });
    $$('[data-close]').forEach(btn =>
        btn.addEventListener('click', () => {
            $('#' + btn.dataset.close)?.classList.remove('show');
        })
    );

    /*** Chuyển tab giữa "Nhập tay" / "Tải hàng loạt" ***/
    $$('#addUserTabs .tab').forEach(tab => {
        tab.addEventListener('click', () => {
            document.querySelector('#addUserTabs .tab.active')?.classList.remove('active');
            tab.classList.add('active');
            document.querySelectorAll('[data-tab-panel]').forEach(panel => {
                panel.style.display = panel.dataset.tabPanel === tab.dataset.tab ? '' : 'none';
            });
        });
    });

    /*** Upload avatar người dùng ***/
    $('#btnChooseAvatar')?.addEventListener('click', () => {
        $('#avatarFile').click();
    });

    $('#avatarFile')?.addEventListener('change', function () {
        const file = this.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = function (e) {
                $('#avatarPreview').src = e.target.result;
                $('#avatarUrl').value = e.target.result; // base64 tạm thời
            };
            reader.readAsDataURL(file);
        }
    });

    /*** Validate form thêm user (frontend nhẹ) ***/
    $('#addUserForm')?.addEventListener('submit', e => {
        const email = $('#email').value.trim();
        const phone = $('#phone').value.trim();
        const pw = $('#password').value;

        if (!validateEmail(email)) { e.preventDefault(); showToast('Lỗi', 'Email không hợp lệ'); return; }
        if (!validatePhone(phone)) { e.preventDefault(); showToast('Lỗi', 'Số điện thoại phải bắt đầu bằng 0 và đủ 9-10 số'); return; }
        if (!validatePassword(pw)) { e.preventDefault(); showToast('Lỗi', 'Mật khẩu ≥8 ký tự, gồm số và ký tự đặc biệt'); return; }
    });

    /*** Nạp dữ liệu địa phương (tỉnh / huyện / xã) ***/
    loadLocations();
}


/*************************************************
 * 5. Validate form (Email, SĐT, Password, Tên)
 *************************************************/
function validateEmail(email) {
    return /^[\w.-]+@[\w.-]+\.\w+$/.test(email);
}
function validatePhone(phone) {
    return /^0\d{9,10}$/.test(phone);   // Bắt đầu bằng 0, dài 9-10 số
}
function validatePassword(pw) {
    return /^(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$/.test(pw);
}
function normalizeName(name) {
    return name.trim().replace(/\s+/g, ' ');
}
document.getElementById('fullName')?.addEventListener('blur', e => {
    e.target.value = normalizeName(e.target.value);
});


/*************************************************
 * 6. Tải dữ liệu địa chỉ Việt Nam (tỉnh/huyện/xã)
 *************************************************/
async function loadLocations() {
    try {
        const [pRes, dRes, wRes] = await Promise.all([
            fetch('/json/tinh_tp.json'),
            fetch('/json/quan_huyen.json'),
            fetch('/json/xa_phuong.json')
        ]);
        const provinces = await pRes.json();
        const districts = await dRes.json();
        const wards = await wRes.json();

        const citySelect = document.getElementById('citySelect');
        const districtSelect = document.getElementById('districtSelect');
        const wardSelect = document.getElementById('wardSelect');

        //  Nạp Tỉnh/TP
        for (const [code, info] of Object.entries(provinces)) {
            const opt = new Option(info.name, info.name); // gửi tên
            opt.dataset.code = code; // lưu code để dùng cho lọc
            citySelect.add(opt);
        }

        //  Khi chọn tỉnh -> load huyện
        citySelect.addEventListener('change', () => {
            const selectedCode = citySelect.selectedOptions[0]?.dataset.code;
            districtSelect.innerHTML = '<option value="">-- Chọn Quận/Huyện --</option>';
            wardSelect.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
            districtSelect.disabled = !selectedCode;
            wardSelect.disabled = true;

            for (const [code, info] of Object.entries(districts)) {
                if (info.parent_code === selectedCode) {
                    const opt = new Option(info.name, info.name);
                    opt.dataset.code = code;
                    districtSelect.add(opt);
                }
            }
        });

        //  Khi chọn huyện -> load xã
        districtSelect.addEventListener('change', () => {
            const selectedCode = districtSelect.selectedOptions[0]?.dataset.code;
            wardSelect.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
            wardSelect.disabled = !selectedCode;

            for (const [code, info] of Object.entries(wards)) {
                if (info.parent_code === selectedCode) {
                    const opt = new Option(info.name, info.name);
                    opt.dataset.code = code;
                    wardSelect.add(opt);
                }
            }
        });

    } catch (err) {
        console.error(" Lỗi khi tải dữ liệu địa phương:", err);
    }
}



/*************************************************
 * 7. Tự ẩn popup thông báo lỗi / thành công
 *************************************************/
window.addEventListener('DOMContentLoaded', () => {
    const errorPopup = $('#errorPopup');
    const successPopup = $('#successPopup');
    [errorPopup, successPopup].forEach(popup => {
        if (popup) setTimeout(() => popup.style.display = 'none', 4000);
    });
});

/*************************************************
 *8 chuẩn hóa search
 *************************************************/
document.addEventListener("DOMContentLoaded", function() {
    const searchForm = document.querySelector(".search-filter-bar form") || document.querySelector(".search-filter-bar");
    const searchInput = document.getElementById("searchInput");

    if (searchForm && searchInput) {
        searchForm.addEventListener("submit", function (e) {
            // Chuẩn hóa chuỗi tìm kiếm
            let val = searchInput.value.trim().replace(/\s+/g, " ");
            searchInput.value = val;
        });
    }
});


document.addEventListener("DOMContentLoaded", () => {
    const success = document.getElementById("successPopup");
    const error = document.getElementById("errorPopup");

    [success, error].forEach(popup => {
        if (popup) {
            popup.style.display = "block";
            setTimeout(() => {
                popup.style.opacity = "0";
                popup.style.transform = "translateY(-10px)";
                setTimeout(() => popup.remove(), 500);
            }, 3000);
        }
    });
});

document.addEventListener("DOMContentLoaded", () => {

    const fields = [
        { id: "email", validator: validateEmail, message: "Email không hợp lệ!" },
        { id: "phone", validator: validatePhone, message: "Số điện thoại không hợp lệ!" },
        { id: "password", validator: validatePassword, message: "Mật khẩu ≥ 8 ký tự, gồm số & ký tự đặc biệt!" },
        { id: "fullName", validator: (v) => v.trim().length >= 2, message: "Tên quá ngắn!" }
    ];

    fields.forEach(f => {
        const input = document.getElementById(f.id);
        if (!input) return;

        const small = input.parentElement.parentElement.querySelector(".error-text");
        const icon = input.parentElement.querySelector(".validate-icon");

        input.addEventListener("input", () => {
            const value = input.value.trim();
            const isValid = value === "" ? null : f.validator(value);

            if (isValid === null) {
                small.textContent = "";
                icon.style.opacity = 0;
                input.classList.remove("error-border", "success-border");
                small.classList.remove("active");
            }
            else if (isValid) {

                icon.innerHTML = '<i class="fa-solid fa-circle-check"></i>';
                icon.style.opacity = 1;
                input.classList.add("success-border");
                input.classList.remove("error-border");
                small.textContent = "✔ Hợp lệ";
                small.className = "error-text success active";
            }
            else {

                icon.innerHTML = '<i class="fa-solid fa-circle-xmark"></i>';
                icon.style.opacity = 1;
                input.classList.add("error-border");
                input.classList.remove("success-border");
                small.textContent = f.message;
                small.className = "error-text error active";
            }
        });
    });

});


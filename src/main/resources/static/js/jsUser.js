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
    return /^0\d{8,9}$/.test(phone);   // Bắt đầu bằng 0, dài 9-10 số
}
function validatePassword(pw) {
    return /^(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$/.test(pw);
}
function normalizeName(name) {
    return name.trim().replace(/\s+/g, ' ');
}
$('#fullName')?.addEventListener('blur', e => {
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

        const citySelect = $('#citySelect');
        const districtSelect = $('#districtSelect');
        const wardSelect = $('#wardSelect');

        // Nạp tỉnh
        for (const [code, info] of Object.entries(provinces)) {
            citySelect.add(new Option(info.name, code));
        }

        // Khi chọn tỉnh → load quận/huyện
        citySelect.addEventListener('change', () => {
            const selected = citySelect.value;
            districtSelect.innerHTML = '<option value="">-- Chọn Quận/Huyện --</option>';
            wardSelect.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
            districtSelect.disabled = !selected;
            wardSelect.disabled = true;
            for (const [code, info] of Object.entries(districts)) {
                if (info.parent_code === selected) {
                    districtSelect.add(new Option(info.name, code));
                }
            }
        });

        // Khi chọn huyện → load xã
        districtSelect.addEventListener('change', () => {
            const selected = districtSelect.value;
            wardSelect.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
            wardSelect.disabled = !selected;
            for (const [code, info] of Object.entries(wards)) {
                if (info.parent_code === selected) {
                    wardSelect.add(new Option(info.name, code));
                }
            }
        });
    } catch (err) {
        console.error("❌ Lỗi khi tải dữ liệu địa phương:", err);
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
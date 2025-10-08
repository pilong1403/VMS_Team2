/***** HELPER UI *****/
const $ = sel => document.querySelector(sel);
const $$ = sel => document.querySelectorAll(sel);

function showToast(title, msg) {
    const toast = $('#toast');
    if (!toast) return;
    $('#toastTitle').textContent = title;
    $('#toastMessage').textContent = msg;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 2500);
}

function initials(name) {
    return (name || '')
        .split(' ')
        .map(w => w[0])
        .join('')
        .toUpperCase()
        .slice(0, 2);
}

function formatDate(d) {
    return new Date(d).toLocaleDateString('vi-VN');
}

function roleBadge(role) {
    if (!role) return '';
    const cls =
        role.toLowerCase() === 'admin'
            ? 'badge-admin'
            : role.toLowerCase() === 'quản lý'
                ? 'badge-manager'
                : 'badge-staff';
    return `<span class="badge ${cls}">${role}</span>`;
}

/***** SIDEBAR TOGGLE *****/
document.addEventListener('click', e => {
    if (e.target.id === 'sidebarToggle') {
        $('#sidebar')?.classList.toggle('collapsed');
    }
    if (e.target.dataset?.close) {
        $('#' + e.target.dataset.close)?.classList.remove('show');
    }
});

/***** PAGE ROUTER *****/
document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;
    if (page === 'users') initUsersPage();
    if (page === 'orgs') initOrgsPage();
    if (page === 'reports') initReportsPage();
});

/**************** USERS PAGE ****************/
function initUsersPage() {
    // mở modal thêm user
    $('#btnOpenAddUser')?.addEventListener('click', () => {
        $('#addUserModal').classList.add('show');
    });

    // đóng modal
    $$('[data-close]').forEach(btn =>
        btn.addEventListener('click', () => {
            $('#' + btn.dataset.close)?.classList.remove('show');
        })
    );

    document.addEventListener('DOMContentLoaded', () => {
        if (document.body.dataset.page === 'users') initUsersPage();
    });
    // Chuyển tab
    document.querySelectorAll('#addUserTabs .tab').forEach(tab => {
        tab.addEventListener('click', () => {
            // remove active tab
            document.querySelector('#addUserTabs .tab.active')?.classList.remove('active');
            tab.classList.add('active');

            // show đúng panel
            document.querySelectorAll('[data-tab-panel]').forEach(panel => {
                panel.style.display = panel.dataset.tabPanel === tab.dataset.tab ? '' : 'none';
            });
        });
    });


    // filter tìm kiếm (client-side)
    $('#searchInput')?.addEventListener('input', () => filterUserTable());
    $('#roleFilter')?.addEventListener('change', () => filterUserTable());
    $('#statusFilter')?.addEventListener('change', () => filterUserTable());
    $('#userDateFilterBtn')?.addEventListener('click', () => filterUserTable());

    // lọc bảng
    function filterUserTable() {
        const kw = $('#searchInput')?.value.toLowerCase() || '';
        const role = $('#roleFilter')?.value || 'all';
        const status = $('#statusFilter')?.value || 'all';
        const from = $('#userFromDate')?.value || '';
        const to = $('#userToDate')?.value || '';

        $$('#userTableBody tr').forEach(tr => {
            const name = tr.children[1]?.textContent.toLowerCase() || '';
            const email = tr.children[2]?.textContent.toLowerCase() || '';
            const phone = tr.children[3]?.textContent || '';
            const roleText = tr.children[4]?.textContent || '';
            const statusText = tr.children[5]?.textContent || '';
            const date = tr.children[6]?.textContent || '';

            let visible = true;
            if (kw && !(name.includes(kw) || email.includes(kw) || phone.includes(kw))) visible = false;
            if (role !== 'all' && roleText !== role) visible = false;
            if (status !== 'all' && statusText !== status) visible = false;
            if (from && date < from) visible = false;
            if (to && date > to) visible = false;

            tr.style.display = visible ? '' : 'none';
        });
    }
}

let currentPage = 1;
const pageSize = 10;

function renderPagination(total) {
    const totalPages = Math.ceil(total / pageSize);
    const pageNumbers = $('#pageNumbers');
    pageNumbers.innerHTML = '';
    for (let i = 1; i <= totalPages; i++) {
        const btn = document.createElement('button');
        btn.textContent = i;
        btn.className = 'btn' + (i === currentPage ? ' btn-primary' : '');
        btn.addEventListener('click', () => { currentPage = i; showPage(); });
        pageNumbers.appendChild(btn);
    }
}

function showPage() {
    const rows = [...$$('#userTableBody tr')];
    rows.forEach((r, idx) => {
        r.style.display = (idx >= (currentPage-1)*pageSize && idx < currentPage*pageSize) ? '' : 'none';
    });
}


/**************** ORGS PAGE ****************/
function initOrgsPage() {
    // gắn sự kiện lọc
    $('#orgFilterBtn')?.addEventListener('click', () => filterOrgTable());
    $('#orgSearch')?.addEventListener('input', () => filterOrgTable());

    function filterOrgTable() {
        const kw = $('#orgSearch')?.value.toLowerCase() || '';
        const st = $('#orgStatusFilter')?.value || 'all';
        const from = $('#orgFromDate')?.value || '';
        const to = $('#orgToDate')?.value || '';

        $$('#orgTableBody tr').forEach(tr => {
            const sender = tr.children[0]?.textContent.toLowerCase() || '';
            const org = tr.children[1]?.textContent.toLowerCase() || '';
            const status = tr.children[4]?.textContent || '';
            const date = tr.children[2]?.textContent || '';

            let visible = true;
            if (kw && !(sender.includes(kw) || org.includes(kw))) visible = false;
            if (st !== 'all' && !status.includes(st)) visible = false;
            if (from && date < from) visible = false;
            if (to && date > to) visible = false;

            tr.style.display = visible ? '' : 'none';
        });
    }
}

/**************** REPORTS PAGE ****************/
function initReportsPage() {
    // ví dụ dùng data từ thẻ script hoặc server render
    if (!window.Chart) return;

    const dataElem = $('#reportData');
    if (!dataElem) return;

    const data = JSON.parse(dataElem.textContent || '{}');
    const labels = data.labels || [];
    const values = data.values || [];

    new Chart($('#reportChart'), {
        type: 'line',
        data: {
            labels,
            datasets: [{ label: 'Người dùng mới', data: values }]
        },
        options: { responsive: true, maintainAspectRatio: false }
    });
}
// === Validate form thêm user ===
function validateEmail(email) {
    return /^[\w.-]+@[\w.-]+\.\w+$/.test(email);
}
function validatePhone(phone) {
    return /^0\d{8,9}$/.test(phone);   // Bắt đầu bằng 0, dài 9-10 số
}
function validatePassword(pw) {
    return /^(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$/.test(pw);
}
// === Validate tên ===
function normalizeName(name) {
    // Xóa khoảng trắng đầu & cuối, thay nhiều khoảng trắng thành 1
    return name.trim().replace(/\s+/g, ' ');
}

// Khi người dùng rời khỏi ô nhập tên
document.getElementById('fullName').addEventListener('blur', (e) => {
    e.target.value = normalizeName(e.target.value);
});



$('#addUserForm')?.addEventListener('submit', e => {
    e.preventDefault();
    const email = $('#email').value.trim();
    const phone = $('#phone').value.trim();
    const pw = $('#password').value;

    if (!validateEmail(email)) { showToast('Lỗi', 'Email không hợp lệ'); return; }
    if (!validatePhone(phone)) { showToast('Lỗi', 'Số điện thoại phải bắt đầu bằng 0 và đủ 9-10 số'); return; }
    if (!validatePassword(pw)) { showToast('Lỗi', 'Mật khẩu ≥8 ký tự, gồm số và ký tự đặc biệt'); return; }

    e.target.submit(); // hợp lệ mới submit
});

$('#userTableBody')?.addEventListener('click', e => {
    if (e.target.classList.contains('avatar')) {
        const id = e.target.dataset.userid;
        // gọi API / lấy data từ list
        showUserDetailModal(id);
    }
});
// Nút mở chọn file
document.getElementById('btnChooseAvatar').addEventListener('click', () => {
    document.getElementById('avatarFile').click();
});

// Khi chọn file ảnh → hiển thị preview
document.getElementById('avatarFile').addEventListener('change', function() {
    const file = this.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            // Hiển thị ảnh preview
            document.getElementById('avatarPreview').src = e.target.result;
            // Có thể lưu tạm vào backend sau này, tạm thời chỉ gán vào hidden input
            document.getElementById('avatarUrl').value = e.target.result;
        };
        reader.readAsDataURL(file); // Đọc ảnh dưới dạng base64
    }
});

function showUserDetailModal(id) {
    // fetch(`/admin/users/${id}`) hoặc lấy từ biến JS
    const modal = $('#userDetailModal');
    $('#userDetailContent').innerHTML = `
    <div class="user-detail-header">
      <div class="user-detail-avatar">A</div>
      <div class="user-detail-info">
        <h3>Nguyễn Văn A</h3>
        <div class="rating">${'★'.repeat(4)}☆</div>
      </div>
    </div>
    <div class="user-detail-grid">
      <div class="detail-item"><span class="detail-label">Email:</span> a@gmail.com</div>
      ...
    </div>
  `;
    modal.classList.add('show');
}
// new Chart($('#roleChart'), {
//     type: 'doughnut',
//     data: {
//         labels: ['Admin','Quản lý','Nhân viên'],
//         datasets: [{ data:[10,25,65], backgroundColor:['#007bff','#ffc107','#28a745']}]
//     }
// });
//
// new Chart($('#orgTrendChart'), {
//     type: 'bar',
//     data: {
//         labels: ['1','2','3','4','5','6','7'],
//         datasets: [{ label:'Tổ chức mới', data:[3,5,2,6,8,4,7], backgroundColor:'#17a2b8'}]
//     }
// });

// thêm địa chỉ người dùng
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

        const citySelect     = document.getElementById('citySelect');
        const districtSelect = document.getElementById('districtSelect');
        const wardSelect     = document.getElementById('wardSelect');

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

// Gọi khi trang load
document.addEventListener('DOMContentLoaded', () => {
    if (document.body.dataset.page === 'users') {
        loadLocations();
    }
});
window.addEventListener('DOMContentLoaded', () => {
    const popup = document.getElementById('errorPopup');
    if (popup) {
        setTimeout(() => popup.style.display = 'none', 4000); // 4 giây tự ẩn
    }
});

    window.addEventListener('DOMContentLoaded', () => {
    const errorPopup = document.getElementById('errorPopup');
    const successPopup = document.getElementById('successPopup');

    [errorPopup, successPopup].forEach(popup => {
    if (popup) setTimeout(() => popup.style.display = 'none', 4000);
});
});





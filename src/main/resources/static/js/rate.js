/**
 * JS Quản lý Rating
 * - Click chọn sao
 * - Hiển thị số ký tự comment
 * - Tự động submit form khi thay đổi trạng thái / sort
 * - Hỗ trợ đổi kích thước trang
 */

document.addEventListener("DOMContentLoaded", () => {

    // ==========================
    // 1. STAR RATING HIỂN THỊ
    // ==========================
    document.querySelectorAll('.stars').forEach(starGroup => {
        const labels = starGroup.querySelectorAll('label');

        labels.forEach((label, index) => {
            const input = label.querySelector('input[type="radio"]');
            const span = label.querySelector('span');

            label.addEventListener('click', e => {
                e.preventDefault(); // không allow label submit ngay
                input.checked = true;
                updateStars(starGroup, index + 1);
            });
        });

        // Nếu có sao chọn sẵn (khi edit)
        const checked = starGroup.querySelector('input[type="radio"]:checked');
        if (checked) {
            const idx = Array.from(labels).findIndex(l => l.querySelector('input').checked);
            updateStars(starGroup, idx + 1);
        }
    });

    // Hàm update hiển thị sao
    function updateStars(group, value) {
        const spans = group.querySelectorAll('span');
        spans.forEach((s, i) => {
            s.textContent = i < value ? '★' : '☆';
            s.style.color = i < value ? '#f59e0b' : '#ccc';
        });
    }

    // ==========================
    // 2. COMMENT COUNTER
    // ==========================
    const textarea = document.querySelector('textarea[name$="comment"]');
    const counter = document.querySelector('.counter');
    if (textarea && counter) {
        const updateCount = () => {
            const len = textarea.value.length;
            counter.textContent = `Tối đa 500 ký tự (${len}/500)`;
        };
        updateCount();
        textarea.addEventListener('input', updateCount);
    }

    // ==========================
    // 3. AUTO SUBMIT FILTER FORM
    // ==========================
    const filterElements = [
        document.getElementById("statusPendingSelect"),
        document.getElementById("sortSelect"),
        document.getElementById("statusSelect"), // nếu bạn thêm select này
        document.getElementById("pageSizeSelect") // nếu có dropdown kích thước trang
    ];

    filterElements.forEach(select => {
        if (select) {
            select.addEventListener("change", function () {
                const form = this.closest("form");
                if (form) form.submit();
            });
        }
    });

});

/**
 * Hàm đổi kích thước trang (nếu bạn dùng dropdown page size riêng)
 */
function changePageSize(size) {
    const params = new URLSearchParams(window.location.search);
    params.set('size', size);
    params.set('page', 0);
    window.location.search = params.toString();
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

document.addEventListener("DOMContentLoaded", () => {

    /* ========== 1. Hiển thị popup thông báo ========== */
    const successPopup = document.getElementById("successPopup");
    const errorPopup = document.getElementById("errorPopup");

    [successPopup, errorPopup].forEach(popup => {
        if (popup) {
            setTimeout(() => {
                popup.style.opacity = "0";
                setTimeout(() => popup.remove(), 600);
            }, 4000);
        }
    });

    /* ========== 2. Mở modal duyệt / từ chối (Hàm chính) ========== */
    window.openDecisionModal = function (btn, actionType) {
        const orgId = btn.dataset.id;
        const orgName = btn.dataset.name;
        const orgEmail = btn.dataset.email;

        const modal = document.getElementById("decisionModal");
        const form = document.getElementById("decisionForm");
        const title = document.getElementById("modalTitle");
        const label = document.getElementById("reasonLabel");
        const placeholder = document.getElementById("reason");

        // Cập nhật form action theo loại
        form.action = `/admin/organizations/${orgId}/${actionType}`;
        document.getElementById("orgName").textContent = `Tổ chức: ${orgName}`;

        // Tùy chỉnh UI
        if (actionType === "approve") {
            title.textContent = "Lý do duyệt hồ sơ";
            label.textContent = "Ghi chú khi duyệt:";
            placeholder.placeholder = "Nhập ghi chú (không bắt buộc)";
            placeholder.classList.remove("reject-mode");
        } else {
            title.textContent = "Lý do từ chối hồ sơ";
            label.textContent = "Lý do từ chối:";
            placeholder.placeholder = "Nhập lý do từ chối (bắt buộc)";
            placeholder.classList.add("reject-mode");
        }

        modal.classList.add("show");
    };

    /* ========== 3. Đóng modal quyết định (Hàm chính) ========== */
    window.closeDecisionModal = function () {
        const modal = document.getElementById("decisionModal");
        if (modal) modal.classList.remove("show");
    };

    /* ========== 4. Xác nhận submit form duyệt/từ chối ========== */
    const decisionForm = document.getElementById("decisionForm");
    if (decisionForm) {
        decisionForm.addEventListener("submit", e => {
            const reasonField = document.getElementById("reason");
            const reason = reasonField.value.trim();
            const isReject = decisionForm.action.includes("/reject");

            if (isReject && !reason) {
                e.preventDefault();
                alert("⚠ Vui lòng nhập lý do từ chối!");
                return false;
            }

            const confirmText = isReject
                ? "Bạn có chắc muốn TỪ CHỐI hồ sơ này không?"
                : "Xác nhận DUYỆT hồ sơ này?";
            if (!confirm(confirmText)) e.preventDefault();
        });
    }

    /* ========== 5. Các hàm gọi modal quyết định (Wrapper) ========== */
    // Đặt vào đây để chúng có thể "thấy" các hàm ở mục 2 và 3
    window.openRejectModal = function (btn) {
        window.openDecisionModal(btn, "reject");
    };

    window.openApproveModal = function (btn) {
        window.openDecisionModal(btn, "approve");
    };

    // Gán hàm đã tồn tại
    window.closeRejectModal = window.closeDecisionModal;
    window.closeApproveModal = window.closeDecisionModal;


    /* ========== 6. Modal xem chi tiết (server-rendered) ========== */
    const detailModal = document.getElementById("orgDetailModal");
    if (detailModal && detailModal.classList.contains("show")) {
        detailModal.style.display = "flex";
    }

    const userDetailModal = document.getElementById("userDetailModal");
    if (userDetailModal && userDetailModal.classList.contains("show")) {
        userDetailModal.style.display = "flex";
    }

    /* ========== 7. Toast thông báo nhanh (runtime) ========== */
    window.showToast = function (title, message) {
        const toast = document.getElementById("toast");
        if (!toast) return;

        document.getElementById("toastTitle").textContent = title;
        document.getElementById("toastMessage").textContent = message;

        toast.classList.add("show");
        setTimeout(() => toast.classList.remove("show"), 3500);
    };

    /* ========== 8. Header profile dropdown ========== */
    const profileDropdown = document.getElementById('profileDropdown');
    const dropdownMenu = document.getElementById('dropdownMenu');

    if (profileDropdown) {
        profileDropdown.addEventListener('click', function (event) {
            event.stopPropagation(); // Ngăn sự kiện click lan ra ngoài
            dropdownMenu.classList.toggle('show');
        });
    }

    /* ========== 9. Sidebar profile dropdown ========== */
    const sidebarProfile = document.getElementById('sidebarProfile');
    const sidebarDropdownMenu = document.getElementById('sidebarDropdownMenu');

    if (sidebarProfile) {
        sidebarProfile.addEventListener('click', function (event) {
            event.stopPropagation();
            sidebarDropdownMenu.classList.toggle('show');
        });
    }

    /* ========== 10. Đóng dropdown khi click bên ngoài ========== */
    window.addEventListener('click', function (event) {
        if (dropdownMenu && dropdownMenu.classList.contains('show')) {
            dropdownMenu.classList.remove('show');
        }
        if (sidebarDropdownMenu && sidebarDropdownMenu.classList.contains('show')) {
            sidebarDropdownMenu.classList.remove('show');
        }
    });

    /* ========== 11. Validate reason textarea ========== */
    const reason = document.querySelector('.textarea-reason');

    // Định nghĩa hàm
    function validateReason() {
        if (!reason) return; // Kiểm tra nếu không tìm thấy

        if (!reason.value.trim()) {
            reason.classList.add('is-invalid');
            reason.classList.remove('is-valid');
        } else {
            reason.classList.remove('is-invalid');
            reason.classList.add('is-valid');
        }
    }

    // Gán sự kiện 'input' cho nó (an toàn hơn là gọi từ HTML)
    if (reason) {
        reason.addEventListener('input', validateReason);
    }

    // Gán vào window để HTML (nếu có) vẫn gọi được
    window.validateReason = validateReason;

}); // <-- ĐÓNG DOMContentLoaded DUY NHẤT


document.addEventListener("DOMContentLoaded", () => {

    /* ========== 1Hiển thị popup thông báo ========== */
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

    /* ========== 2️Mở modal duyệt / từ chối ========== */
    window.openDecisionModal = function (btn, actionType) {
        const orgId = btn.dataset.id;
        const orgName = btn.dataset.name;
        const orgEmail = btn.dataset.email;

        const modal = document.getElementById("decisionModal");
        const form = document.getElementById("decisionForm");
        const title = document.getElementById("modalTitle");
        const label = document.getElementById("reasonLabel");
        const placeholder = document.getElementById("reason");

        // cập nhật form action theo loại
        form.action = `/admin/organizations/${orgId}/${actionType}`;
        document.getElementById("orgName").textContent = `Tổ chức: ${orgName}`;

        // tùy chỉnh UI
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

    /* ========== 3️ Đóng modal quyết định ========== */
    window.closeDecisionModal = function () {
        const modal = document.getElementById("decisionModal");
        if (modal) modal.classList.remove("show");
    };

    /* ========== 4️ Xác nhận submit form duyệt/từ chối ========== */
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


    /* ========== 6️ Modal xem chi tiết bằng chứng (server-rendered) ========== */
    const detailModal = document.getElementById("orgDetailModal");
    if (detailModal && detailModal.classList.contains("show")) {
        detailModal.style.display = "flex";
    }

    /* ========== 7️⃣ Toast thông báo nhanh (runtime) ========== */
    window.showToast = function (title, message) {
        const toast = document.getElementById("toast");
        if (!toast) return;

        document.getElementById("toastTitle").textContent = title;
        document.getElementById("toastMessage").textContent = message;

        toast.classList.add("show");
        setTimeout(() => toast.classList.remove("show"), 3500);
    };
});
document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("userDetailModal");
    if (modal && modal.classList.contains("show")) {
        modal.style.display = "flex";
    }
});

/* ========== 8️ Modal quyết định chung ========== */
window.openRejectModal = function (btn) {
    openDecisionModal(btn, "reject");
};

window.openApproveModal = function (btn) {
    openDecisionModal(btn, "approve");
};

window.closeRejectModal = closeDecisionModal;
window.closeApproveModal = closeDecisionModal;

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


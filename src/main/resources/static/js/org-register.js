// org-register.js
document.addEventListener("DOMContentLoaded", function () {
    // ===== Ẩn lỗi của field khi user tương tác =====
    function hideFieldError(el) {
        const grp = el.closest(".mb-3, .col-md-6, .form-group") || el.parentElement;
        if (!grp) return;
        const err = grp.querySelector(".text-danger.small");
        if (err) err.style.display = "none";
    }

    // Gắn ẩn lỗi cho tất cả input/textarea/select
    document.querySelectorAll("input, textarea, select").forEach(function (el) {
        el.addEventListener("focus",  () => hideFieldError(el));
        el.addEventListener("input",  () => hideFieldError(el));
        el.addEventListener("change", () => hideFieldError(el));
    });

    // ===== Nút 'mắt' ẩn/hiện cho mật khẩu & xác nhận mật khẩu =====
    function attachEyeToggle(inputId) {
        const inp = document.getElementById(inputId);
        if (!inp) return;

        // Nếu đã là input-group rồi thì bỏ qua (tránh bọc trùng)
        if (inp.parentElement && inp.parentElement.classList.contains("input-group")) return;

        const wrapper = document.createElement("div");
        wrapper.className = "input-group";

        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "btn btn-outline-secondary";
        btn.setAttribute("aria-label", "Ẩn/Hiện mật khẩu");

        const icon = document.createElement("i");
        icon.className = "bi bi-eye";
        btn.appendChild(icon);

        // Chèn wrapper trước input, rồi đưa input + nút vào wrapper
        inp.parentElement.insertBefore(wrapper, inp);
        wrapper.appendChild(inp);
        wrapper.appendChild(btn);

        btn.addEventListener("click", function () {
            const isPwd = inp.type === "password";
            inp.type = isPwd ? "text" : "password";
            icon.className = isPwd ? "bi bi-eye-slash" : "bi bi-eye";
            inp.focus();
        });
    }

    // Thymeleaf th:field="*{password}" / "*{confirmPassword}" -> id="password"/"confirmPassword"
    attachEyeToggle("password");
    attachEyeToggle("confirmPassword");

    // ===== Loading khi submit form =====
    const form = document.querySelector('form[th\\:object], form[method="post"][enctype="multipart/form-data"]');
    if (form) {
        form.addEventListener("submit", function () {
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML =
                    '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang gửi OTP...';
            }
        });
    }
});

/**
 * form-error-handler.js
 * Tự động ẩn thông báo lỗi khi người dùng sửa lại ô nhập liệu.
 */
document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("form");
    if (!form) return;

    // Gắn listener cho tất cả input, select, textarea
    const fields = form.querySelectorAll("input, select, textarea");
    fields.forEach(field => {
        field.addEventListener("input", handleChange);
        field.addEventListener("focus", handleChange);
    });

    function handleChange(e) {
        const el = e.target;
        // Ẩn error-text nằm ngay sau hoặc gần field
        let error = el.parentElement.querySelector(".error-text");
        if (!error) {
            // Nếu label và input nằm tách grid
            const next = el.closest("div")?.querySelector(".error-text");
            if (next) error = next;
        }
        if (error) error.style.display = "none";
        // Bỏ viền vàng cam
        el.classList.remove("field-error");
    }
});

package com.fptuni.vms.service;

import com.fptuni.vms.dto.request.OpportunityForm;
import com.fptuni.vms.model.Opportunity;
import com.fptuni.vms.model.User;
import org.springframework.validation.BindingResult;

public interface OpportunityBusinessService {

    /**
     * Xử lý toàn bộ nghiệp vụ tạo/cập nhật Opportunity:
     * - trim dữ liệu
     * - validate time, status, số TNV, sections, lock
     * - upload ảnh thumbnail + section images
     * - lưu Opportunity + sections
     * - gửi notification cho TNV
     *
     * Nếu có lỗi, BindingResult sẽ có errors và method trả về null.
     */
    Opportunity saveOpportunityWithBusinessRules(
            OpportunityForm form,
            BindingResult binding,
            User currentUser
    );

    /**
     * Xử lý hủy sự kiện:
     * - kiểm tra lock
     * - đổi status
     * - lưu
     * - gửi notification
     *
     * Ném IllegalStateException khi không hủy được.
     */
    void cancelOpportunity(Integer oppId, User actor);
}

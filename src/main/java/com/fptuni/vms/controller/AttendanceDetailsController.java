package com.fptuni.vms.controller;

import com.fptuni.vms.dto.response.AttendanceRecordDTO;
import com.fptuni.vms.integrations.cloud.CloudStorageService;
import com.fptuni.vms.model.*;
import com.fptuni.vms.security.CustomUserDetails;
import com.fptuni.vms.service.AttendanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
public class AttendanceDetailsController {

    private final AttendanceService attendanceService;
    private final CloudStorageService cloudStorageService;

    public AttendanceDetailsController(AttendanceService attendanceService, CloudStorageService cloudStorageService) {
        this.attendanceService = attendanceService;
        this.cloudStorageService = cloudStorageService;
    }

    @GetMapping("/organization/attendance-details")
    public String showAttendanceDetails(Model model,
                                        @RequestParam("opportunityId") Integer opportunityId,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(name = "num", required = false) Integer size,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(defaultValue = "1") int page,
                                        RedirectAttributes redirectAttributes) {

        Opportunity opportunity = attendanceService.getOpportunity(opportunityId);

        if (opportunity == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sự kiện với ID: " + opportunityId);
            return "redirect:/organization/attendance";
        }

        int recordsPerPage = (size != null && size > 0) ? size : 5;
        List<AttendanceRecordDTO> attendanceList = attendanceService.getAttendanceListForOpportunity
                (opportunityId, keyword, status, page, recordsPerPage);


        //lấy status -> check trạng thái sự kiện để cấp quyền check in, check out
        Opportunity.OpportunityStatus currentStatus = opportunity.getStatus();

        LocalDateTime now = LocalDateTime.now();
        // TH STATUS = OPEN
            if (currentStatus == Opportunity.OpportunityStatus.OPEN) {
                // CHƯA DIỄN RA ( now < startTime )
                if (now.isBefore(opportunity.getStartTime())) {
                    model.addAttribute("statusOpp", "NOT_YET");
                }
                // ON_GOING ( startTime <= now <= endTime )
                else if (!now.isBefore(opportunity.getStartTime()) && !now.isAfter(opportunity.getEndTime())) {
                    model.addAttribute("statusOpp", "ON_GOING");
                }
            } else{
        //TH: STATUS = CLOSED
                LocalDateTime endOfDay = opportunity.getEndTime().toLocalDate().atTime(LocalTime.MAX); //LocalDate, tức là chỉ giữ ngày, bỏ phần giờ-phút-giây.
                // TH: now <= end of day (23:59:59 of event day) --> can check in, check out
                if (currentStatus == Opportunity.OpportunityStatus.CLOSED && !now.isAfter(endOfDay)) {
                    model.addAttribute("statusOpp", "HALF_CLOSED");
                    attendanceService.processAbsentVolunteers(attendanceList);

                } else if(currentStatus == Opportunity.OpportunityStatus.CLOSED && now.isAfter(endOfDay)) {
                    // TH: now > end of day ( đã sang ngay mới ) --> KO thể check in, check out
                    model.addAttribute("statusOpp", "CLOSED");
                    attendanceService.processAbsentVolunteers(attendanceList);
                }
            }


            List<AttendanceRecordDTO> allAttendanceList = attendanceService.getAllAttendance(opportunityId);
            // lấy số liệu thống kê attendance
            Map<String, Integer> attendanceStats  = attendanceService.getTotalStatusOfVolunteer(allAttendanceList);
            Integer presentCount = attendanceStats.get("presentCount");
            Integer absentCount = attendanceStats.get("absentCount");

            BigDecimal totalHoursOfVolunteers = attendanceService.getTotalHoursOfVolunteers(allAttendanceList);

            double completeStatistic = attendanceService.getCompleteStatistic(allAttendanceList);
            model.addAttribute("completeStatistic", completeStatistic);
            model.addAttribute("presentCount", presentCount);
            model.addAttribute("absentCount", absentCount);
            model.addAttribute("totalHours", totalHoursOfVolunteers);


        long totalOpp = attendanceService.countFilteredAttendanceRecords(opportunityId, keyword, status);
        int totalPages = (int) Math.ceil((double) totalOpp / recordsPerPage);
        if (totalPages == 0) {
            totalPages = 1;
        }

        int visiblePages = 3;
        int startPage = Math.max(1, page - visiblePages / 2);
        int endPage = Math.min(totalPages, startPage + visiblePages - 1);
        if (endPage < startPage) {
            startPage = 1;
            endPage = 1;
        }

        if (attendanceList == null || attendanceList.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy bản ghi điểm danh nào phù hợp!");
        }

        model.addAttribute("attendanceList", attendanceList);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("status", status);
        model.addAttribute("num", size);
        model.addAttribute("keyword", keyword);
        model.addAttribute("opportunity", opportunity);

        model.addAttribute("activePage", "attendance");
        return "attendance/AttendanceDetails";
    }

    @PostMapping("/organization/attendance-details/check-in")
    public String processCheckIn(@RequestParam("applicationId") Integer applicationId,
                                 @RequestParam("oppId") Integer oppId,
                                 @RequestParam("checkinTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkinTime,
                                 RedirectAttributes redirectAttributes) {

        Application application = attendanceService.findApplicationById(applicationId);

        if (application == null) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Không tìm thấy đơn ứng tuyển hợp lệ.");
            return "redirect:/organization/attendance-details?opportunityId=" + oppId;
        }

        Opportunity opportunity = application.getOpportunity();

        LocalDateTime eventStartTime = opportunity.getStartTime();
//        LocalDateTime endOfDay = eventStartTime.toLocalDate().atTime(LocalTime.MAX);
        LocalDateTime eventEndTime = opportunity.getEndTime();

        // Lấy bản ghi điểm danh và thời gian check-out hiện tại (nếu có)
        Attendance existingAttendance = attendanceService.findAttendanceByApplicationId(applicationId);
        LocalDateTime existingCheckoutTime = (existingAttendance != null) ? existingAttendance.getCheckoutTime() : null;

        //check xem time check in có < time bắt đầu sk ko
        if (checkinTime.isBefore(eventStartTime)) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Thời gian check-in không được sớm hơn thời gian bắt đầu sự kiện.");
            return "redirect:/organization/attendance-details?opportunityId=" + oppId;
        }

        //Xác định giới hạn cho thời gian check-in
        LocalDateTime maxAllowedTime = eventEndTime;
        if (existingCheckoutTime != null && existingCheckoutTime.isBefore(maxAllowedTime)) {
            maxAllowedTime = existingCheckoutTime;
        }

        //check xem time check in có > giới hạn ko
        if (checkinTime.isAfter(maxAllowedTime)) {
            String errorMessage = (existingCheckoutTime != null)
                    ? "Lỗi: Thời gian check-in không được muộn hơn thời gian đã check-out."
                    : "Lỗi: Thời gian check-in phải ở trong ngày diễn ra sự kiện.";
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/organization/attendance-details?opportunityId=" + oppId;
        }

        // TH chưa có bản ghi attendance thì tạo mới để lưu điểm danh cho volunteer
        if (existingAttendance == null) {
            Attendance newAttendance = new Attendance();
            newAttendance.setApplication(application);
            newAttendance.setCheckinTime(checkinTime);
            newAttendance.setStatus(Attendance.AttendanceStatus.PRESENT);
            attendanceService.createAttendance(newAttendance);
            redirectAttributes.addFlashAttribute("success", "Check-in thành công!");
        } else {
            //TH đã có bản ghi attendance trc đó rồi -> Cập nhật time checkin cho bản ghi đó
            existingAttendance.setCheckinTime(checkinTime);

            // TH: khi sk kết thúc và status của volunteer là ABSENT, admin muốn update lại time checkin -> status về PRESENT
            if(existingAttendance.getStatus() == Attendance.AttendanceStatus.ABSENT){
                existingAttendance.setStatus(Attendance.AttendanceStatus.PRESENT);
            }

            attendanceService.updateAttendance(existingAttendance);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thời gian check-in thành công!");
        }

        return "redirect:/organization/attendance-details?opportunityId=" + oppId;
    }


    @PostMapping("/organization/attendance-details/check-out")
    public String handleCheckOut(@RequestParam("applicationId") Integer applicationId,
                                 @RequestParam("oppId") Integer oppId,
                                 @RequestParam("checkOutTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkOutTime,
                                 @AuthenticationPrincipal CustomUserDetails loggedInUser,
                                 RedirectAttributes redirectAttributes) {


        Attendance attendance = attendanceService.findAttendanceByApplicationId(applicationId);

        if (attendance == null || attendance.getCheckinTime() == null) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Không thể check-out khi chưa check-in.");
            return "redirect:/organization/attendance-details?opportunityId=" + oppId;
        }

        Application application = attendance.getApplication();
        Opportunity opportunity = application.getOpportunity();

        LocalDateTime checkinTime = attendance.getCheckinTime();
        LocalDateTime endTime = opportunity.getEndTime();

        //check xem time check-out có < thời gian check-in
        if (checkOutTime.isBefore(checkinTime)) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Thời gian check-out không được sớm hơn thời gian check-in.");
            return "redirect:/organization/attendance-details?opportunityId=" + opportunity.getOppId();
        }

        //check xem time check-out có = thời gian check-in
        if (checkOutTime.equals(checkinTime)) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Thời gian check-out không được trùng với thời gian check-in.");
            return "redirect:/organization/attendance-details?opportunityId=" + opportunity.getOppId();
        }


        //Thời gian check-out > thời gian kết thúc sk
        if (checkOutTime.isAfter(endTime)) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Thời gian check-out phải ở trong ngày diễn ra sự kiện.");
            return "redirect:/organization/attendance-details?opportunityId=" + opportunity.getOppId();
        }

        // set time check-out và status của attendance = COMPLETED
        attendance.setCheckoutTime(checkOutTime);
        attendance.setStatus(Attendance.AttendanceStatus.COMPLETED);

        // sau khi điểm danh xong -> set status của application về COMPLETED, set processedBy
        application.setStatus(Application.ApplicationStatus.valueOf("COMPLETED"));
        application.setProcessedBy(loggedInUser.getUser());
        attendance.setApplication(application);
        attendanceService.updateAttendance(attendance);
        redirectAttributes.addFlashAttribute("success", "Check-out thành công!");

        return "redirect:/organization/attendance-details?opportunityId=" + opportunity.getOppId();
    }


    @PostMapping("organization/attendance-details/view-attendance-details")
    public String viewAttendanceDetails(@RequestParam("applicationId") Integer applicationId,
                                        @RequestParam("oppId") Integer oppId,
                                        @RequestParam("notes") String notes,
                                        @RequestParam("proofFile") MultipartFile proofFileUrl,
                                        RedirectAttributes redirectAttributes) {

        Attendance attendance = attendanceService.findAttendanceByApplicationId(applicationId);
        if(notes == null || notes.isEmpty()){
            attendance.setNotes(null);
        } else{
            attendance.setNotes(notes);
        }

        String proofFileUrlStr = cloudStorageService.uploadFile(proofFileUrl);
        if(proofFileUrlStr == null) {
            attendance.setProofFileUrl(null);
        }
        attendance.setProofFileUrl(proofFileUrlStr);
        attendanceService.updateAttendance(attendance);
        redirectAttributes.addFlashAttribute("success", "Cập nhật chi tiết điểm danh thành công !!");
        return "redirect:/organization/attendance-details?opportunityId=" + oppId;
    }

}

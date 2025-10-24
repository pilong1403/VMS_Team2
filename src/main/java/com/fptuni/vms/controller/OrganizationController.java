package com.fptuni.vms.controller;

import com.fptuni.vms.integrations.mail.MailService;
import com.fptuni.vms.model.Organization;
import com.fptuni.vms.service.OrganizationService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/organizations")
public class OrganizationController {

    @Autowired private OrganizationService organizationService;
    @Autowired private MailService mailService;

    @GetMapping
    public String listOrganizations(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Organization.RegStatus status,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortField", defaultValue = "createdAt") String sortField,
            @RequestParam(value = "sortDir", defaultValue = "DESC") String sortDir,
            @RequestParam(value = "viewOrg", required = false) Integer viewOrgId,
            Model model
    ) {
        // NOTE: OrganizationService.searchOrganizations signature expects (..., sortDir, sortField)
        List<Organization> orgs = organizationService.searchOrganizations(
                keyword, status, fromDate, toDate, page, size, sortDir, sortField);

        long total = organizationService.countAll();
        long filtered = organizationService.countFiltered(keyword, status, fromDate, toDate);

        // Kích thước trang động: 5, ~10%, ~40%, 100%
        List<Integer> pageSizes = new ArrayList<>();
        pageSizes.add(5);
        if (total > 10) pageSizes.add((int) Math.min(total, Math.round(total * 0.1)));
        if (total > 20) pageSizes.add((int) Math.min(total, Math.round(total * 0.4)));
        if(total>50) pageSizes.add((int) Math.min(total, Math.round(total * 0.8)));
        pageSizes.add((int) total);
        pageSizes = pageSizes.stream().distinct().sorted().toList();

        model.addAttribute("orgs", orgs);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("pageSizes", pageSizes);
        model.addAttribute("filteredOrganizations", filtered);
        model.addAttribute("totalOrganizations", total);

        if (viewOrgId != null) {
            Organization selected = organizationService.getOrganizationById(viewOrgId);
            model.addAttribute("selectedOrg", selected);
            model.addAttribute("showOrgDetailModal", true);
        } else {
            model.addAttribute("selectedOrg", null);
            model.addAttribute("showOrgDetailModal", false);
        }

        return "admin/organizationManagement";
    }

    @GetMapping("/{id}/detail")
    public String organizationDetail(@PathVariable Integer id,
                                     @RequestParam(defaultValue = "proof") String view,
                                     Model model) {
        Organization org = organizationService.getOrganizationById(id);
        if (org == null) {
            model.addAttribute("errorMessage", "Không tìm thấy hồ sơ!");
            return "redirect:/admin/organizations";
        }

        List<Organization> orgs = organizationService.searchOrganizations(null, null, null, null, 0, 10, "DESC", "createdAt");

        model.addAttribute("orgs", orgs);
        model.addAttribute("selectedOrg", org);
        model.addAttribute("viewType", view);
        model.addAttribute("page", 0);
        model.addAttribute("size", 10);
        model.addAttribute("sortField", "createdAt");
        model.addAttribute("sortDir", "DESC");
        model.addAttribute("filteredOrganizations", organizationService.countAll());
        return "admin/organizationManagement";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Integer id,
                          @RequestParam(value = "reason", required = false) String reason,
                          RedirectAttributes redirectAttributes) {
        Organization org = organizationService.getOrganizationById(id);
        if (org == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy hồ sơ!");
            return "redirect:/admin/organizations";
        }
        org.setRegStatus(Organization.RegStatus.APPROVED);
        org.setRegNote(reason);
        org.setRegReviewedAt(LocalDateTime.now());
        organizationService.saveOrganization(org);

        try {
            mailService.sendApproveEmail(org.getOwner().getEmail(), org.getName(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Duyệt hồ sơ thành công và đã gửi email!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Duyệt hồ sơ thành công nhưng gửi email thất bại!");
        }
        return "redirect:/admin/organizations";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Integer id,
                         @RequestParam("reason") String reason,
                         RedirectAttributes redirectAttributes) {
        Organization org = organizationService.getOrganizationById(id);
        if (org == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy hồ sơ!");
            return "redirect:/admin/organizations";
        }

        org.setRegStatus(Organization.RegStatus.REJECTED);
        org.setRegNote(reason);
        org.setRegReviewedAt(LocalDateTime.now());
        organizationService.saveOrganization(org);

        try {
            mailService.sendRejectEmail(org.getOwner().getEmail(), org.getName(), reason);
            redirectAttributes.addFlashAttribute("successMessage", " Đã từ chối hồ sơ và gửi email thông báo thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "⚠ Từ chối hồ sơ thành công, nhưng gửi email thất bại!");
        }

        return "redirect:/admin/organizations";
    }

    @GetMapping("/{id}/downloadExcel")
    public void downloadExcel(@PathVariable Integer id, HttpServletResponse response) throws IOException {
        Organization org = organizationService.getOrganizationById(id);
        if (org == null) return;

        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=organization_" + id + ".xlsx");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Thông tin tổ chức");
        sheet.createRow(0).createCell(0).setCellValue("Tên tổ chức: " + org.getName());
        sheet.createRow(1).createCell(0).setCellValue("Người gửi: " + org.getOwner().getFullName());
        sheet.createRow(2).createCell(0).setCellValue("Trạng thái: " + org.getRegStatus());
        sheet.createRow(3).createCell(0).setCellValue("Ngày gửi: " + org.getCreatedAt());
        sheet.createRow(4).createCell(0).setCellValue("Tệp minh chứng: " + org.getRegDocUrl());
        sheet.autoSizeColumn(0);
        workbook.write(response.getOutputStream());
        workbook.close();
    }

}

package com.fptuni.vms.service.impl;

import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.UserRepository;
import com.fptuni.vms.service.UserService;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.swing.text.Document;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;



@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    // ===== CRUD =====
    @Override
    public void saveUser(User user) {
        userRepository.save(user);
    }

    @Override
    public User getUserById(Integer id) {
        return userRepository.findById(id);
    }

    @Override
    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<User> searchUsers(String keyword, Integer roleId,
                                  User.UserStatus status,
                                  LocalDate fromDate, LocalDate toDate,
                                  int page, int size,
                                  String sortField, String sortDir) {
        return userRepository.search(keyword, roleId, status, fromDate, toDate, page, size, sortField, sortDir);
    }

    @Override
    public long countFilteredUsers(String keyword, Integer roleId,
                                   User.UserStatus status,
                                   LocalDate fromDate, LocalDate toDate) {
        return userRepository.countFiltered(keyword, roleId, status, fromDate, toDate);
    }


    // ===== STATISTICS =====
    @Override
    public long countAllUsers() {
        return userRepository.countAll();
    }

    @Override
    public long countUsersByStatus(String status) {
        return userRepository.countByStatus(status);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);    }

    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    @Override
    public void exportUserToExcel(User user, OutputStream os) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("User Detail");

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        int rowIdx = 0;
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Thông tin người dùng");
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        sheet.createRow(rowIdx++);

        String[][] data = {
                {"Họ tên", user.getFullName()},
                {"Email", user.getEmail()},
                {"Số điện thoại", user.getPhone() != null ? user.getPhone() : ""},
                {"Vai trò", user.getRole().getRoleName()},
                {"Địa chỉ", user.getAddress() != null ? user.getAddress() : ""},
                {"Trạng thái", user.getStatus().toString()},
                {"Ngày tạo", user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""}
        };

        for (String[] rowData : data) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(rowData[0]);
            row.createCell(1).setCellValue(rowData[1]);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);

        workbook.write(os);
        workbook.close();
    }




}

package com.fptuni.vms.service.impl;

import com.fptuni.vms.integrations.cloud.CloudStorageService;
import com.fptuni.vms.model.SupportResponse;
import com.fptuni.vms.model.SupportTicket;
import com.fptuni.vms.model.User;
import com.fptuni.vms.repository.SupportResponseRepository;
import com.fptuni.vms.service.SupportResponseService;
import com.fptuni.vms.service.SupportTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SupportResponseServiceImpl implements SupportResponseService {

    @Autowired
    private SupportResponseRepository supportResponseRepository;
    @Autowired
    private SupportTicketService supportTicketService;

    @Autowired
    private CloudStorageService cloudStorageService;

    @Override
    public List<SupportResponse> getListResponseWithPagination(int page, int size) {
        return supportResponseRepository.findAllWithPagination(page, size);
    }

    @Override
    public List<SupportResponse> getAllResponses() {
        return supportResponseRepository.findAll();
    }

    @Override
    public SupportResponse addResponse(SupportResponse response) {
        return supportResponseRepository.save(response);
    }

    @Override
    public List<SupportResponse> getResponsesByTicketId(Integer ticketId) {
        return supportResponseRepository.findByTicketId(ticketId);
    }

    @Override
    public boolean ticketHasResponses(Integer ticketId) {
        return supportResponseRepository.hasResponses(ticketId);
    }

    @Override
    public List<SupportResponse> filterAndPaginateResponses(String keyword, Integer num, int page, int size) {
        return supportResponseRepository.filterResponses(keyword, num, page, size);
    }

    @Override
    public long countFilteredResponses(String keyword) {
        return supportResponseRepository.countFilteredResponses(keyword);
    }

    @Override
    @Transactional
    public boolean addAdminResponse(int ticketId, String message, User admin, MultipartFile attachment) {
        try {
            Optional<SupportTicket> ticketOpt = supportTicketService.findById(ticketId);

            if (!ticketOpt.isPresent()) {
                return false;
            }
            SupportTicket existingTicket = ticketOpt.get();

            String attachmentUrl = null;
            if (attachment != null && !attachment.isEmpty()) {
                attachmentUrl = cloudStorageService.uploadFile(attachment);
                if(attachmentUrl == null) {
                    return false;
                }
            }

            SupportResponse newResponse = new SupportResponse();
            newResponse.setTicket(existingTicket);
            newResponse.setMessage(message);
            newResponse.setResponder(admin);
            newResponse.setCreatedAt(LocalDateTime.now());
            newResponse.setAttachmentUrl(attachmentUrl); // Lưu URL từ Cloudinary (có thể là null nếu không có file)

            supportResponseRepository.save(newResponse);

            if (existingTicket.getStatus() == SupportTicket.TicketStatus.OPEN) {
                existingTicket.setStatus(SupportTicket.TicketStatus.IN_PROGRESS);
            }
            existingTicket.setResolvedBy(admin);  // set người xử lý
            existingTicket.setUpdatedAt(LocalDateTime.now()); // Cập nhật thời gian chỉnh sửa
            supportTicketService.update(existingTicket);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



}

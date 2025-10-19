package com.fptuni.vms.service.impl;
import com.fptuni.vms.model.FAQ;
import com.fptuni.vms.repository.FAQRepository;
import com.fptuni.vms.service.FAQService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FAQServiceImpl implements FAQService {

    @Autowired
    private FAQRepository faqRepository;

    @Override
    public List<FAQ> getAllFAQs(int page, int size) {
        return faqRepository.findAll(page, size);
    }

    @Override
    public List<FAQ> getAllFAQsWithoutPagination() {
        return faqRepository.findAllWithoutPagination();
    }

    @Override
    public long getTotalFAQs() {
        return faqRepository.count();
    }

    @Override
    public FAQ getFAQById(Integer id) {
        return faqRepository.findById(id).orElse(null);
    }

    @Override
    public FAQ createFAQ(FAQ faq) {
        if(faqRepository.findByQuestion(faq.getQuestion()) != null) { // có câu hỏi bị trùng
            return null;
        }
        return faqRepository.create(faq);
    }

    @Override
    public FAQ updateFAQ(FAQ faq) {
        if(faqRepository.findDuplicateQuestionOnUpdate(faq.getQuestion(),faq.getFaqId()) != null ){ // có câu hỏi bị trùng
            return null;
        }
        return faqRepository.update(faq);
    }

    @Override
    public boolean existsById(Integer id) {
        return faqRepository.existsById(id);
    }

    @Override
    public List<FAQ> filterFAQs(String status, String category,Integer num, String keyword, int page, int size) {
        return faqRepository.filterFAQs(status, category, num, keyword, page, size);
    }

    @Override
    public long countFilteredFAQs(String status, String category, String keyword) {
        return faqRepository.countFilteredFAQs(status, category, keyword);
    }



}

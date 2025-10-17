package com.fptuni.vms.repository;

import com.fptuni.vms.model.FAQ;
import java.util.List;
import java.util.Optional;

public interface FAQRepository {
    List<FAQ> findAllWithoutPagination();
    List<FAQ> findAll(int page, int size);
    Optional<FAQ> findById(Integer id);
    FAQ create(FAQ faq);
    FAQ update(FAQ faq);
    boolean existsById(Integer id);
    long count();
    FAQ findByQuestion(String question);
    FAQ findDuplicateQuestionOnUpdate(String question, Integer currentFaqId);
    List<FAQ> filterFAQs(String status, String category,Integer num, String keyword, int page, int size);
    long countFilteredFAQs(String status, String category, String keyword);

}
